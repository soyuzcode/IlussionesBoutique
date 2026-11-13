# Guía de pruebas de seguridad — Ilussiones Boutique

Guía práctica para comprobar **a mano** la autenticación y los controles de
seguridad del backend. Incluye comandos listos para copiar y pegar en Windows
(PowerShell) y variantes con `curl.exe`, el resultado esperado y cómo interpretar
cada respuesta.

> **Regla de oro:** esta guía distingue entre *fallo esperado* (el sistema
> rechaza correctamente), *servicio no disponible* (no se pudo probar) y
> *vulnerabilidad real* (el sistema acepta algo que no debería). No confundas
> “no obtuve 200” con “hay un problema”.

---

## 1. Qué necesitas

* Windows con PowerShell 5.1 o superior.
* Java 25 y el wrapper de Maven incluido en el repositorio.
* `curl.exe` (viene con Windows 10/11). Verifícalo con `curl.exe --version`.
* El repositorio en `...\IlussionesBoutique`.

No necesitas Supabase para esta guía: el perfil `httptest` usa una base H2 en
memoria, aislada y desechable.

---

## 2. Levantar el backend y encontrar la URL base

### Opción A — recomendada (H2 aislada, no toca Supabase)

Abre una terminal en la raíz del repositorio:

```powershell
cd Backend
.\mvnw.cmd spring-boot:test-run "-Dspring-boot.run.profiles=httptest"
```

Espera hasta ver estas dos líneas:

```text
Tomcat started on port 18080 (http)
Started IlussionessBackendApplication
```

La URL base es:

```powershell
$base = 'http://127.0.0.1:18080'
```

Deja esa terminal abierta: el proceso debe seguir corriendo. Las credenciales de
la cuenta demo en este perfil son:

| Campo | Valor |
| --- | --- |
| Correo | `admin@ilussiones.com` |
| Contraseña | `DemoPass123!` |

### Opción B — perfil normal

```powershell
$env:APP_DEMO_AUTH_EMAIL = 'admin@ilussiones.com'
$env:APP_DEMO_AUTH_PASSWORD = 'UnaClaveLargaDePrueba!'
$env:APP_AUTH_JWT_SECRET = 'un-secreto-de-al-menos-32-bytes-para-firmar-jwt'
cd Backend
.\mvnw.cmd spring-boot:run
```

La URL base por defecto es `http://localhost:8080` (o el `PORT` configurado).
Este perfil usa PostgreSQL/Supabase: necesita `SUPABASE_DB_USER`,
`SUPABASE_DB_PASS` y `SUPABASE_JWT_SECRET` válidos.

### Comprobar que el servidor responde

```powershell
curl.exe -s -o NUL -w "%{http_code}`n" "$base/api/customer"
```

* `401` → el servidor está arriba y el endpoint está protegido. **Correcto.**
* `000`, “connection refused” o timeout → el servidor no está arriba, la URL o
  el puerto son incorrectos. **No es una vulnerabilidad.**

Si ves un error de puerto ocupado:

```powershell
Get-NetTCPConnection -LocalPort 18080 | Select-Object -First 1 OwningProcess
```

Cierra ese proceso o cambia el puerto de tu perfil.

---

## 3. Preparar la sesión de PowerShell

Ejecuta esto en la misma terminal donde vas a probar:

```powershell
$base = 'http://127.0.0.1:18080'
```

A partir de aquí, todos los comandos usan `$base`. Define una vez este auxiliar
para ver el código y el cuerpo de una respuesta de error. Funciona tanto en
Windows PowerShell 5.1 como en PowerShell 7:

```powershell
function Show-HttpFailure {
  param($record)
  $response = $record.Exception.Response
  if ($null -eq $response) { return 'SIN_RESPUESTA' }
  $status = [int]$response.StatusCode
  $body = ''
  if ($record.ErrorDetails -and $record.ErrorDetails.Message) {
    $body = $record.ErrorDetails.Message
  } else {
    $stream = $response.GetResponseStream()
    if ($stream) {
      $reader = New-Object System.IO.StreamReader($stream)
      $body = $reader.ReadToEnd()
      $reader.Dispose()
    }
  }
  return "$status`n$body"
}
```

Para las pruebas con `curl.exe` en Windows, **no pases el JSON entre comillas
directamente**: PowerShell puede eliminar las comillas internas y el servidor
recibirá un cuerpo inválido (`malformed_payload`). Usa `Invoke-RestMethod`
(recomendado) o un archivo JSON como se muestra en la sección 16.

---

## 4. Prueba 1 — Login correcto y obtención del token

```powershell
$login = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
  -ContentType 'application/json' `
  -Body (@{
    email    = 'admin@ilussiones.com'
    password = 'DemoPass123!'
  } | ConvertTo-Json)

$login | Select-Object tokenType, expiresIn,
  @{n='name';  e={$_.user.name}},
  @{n='email'; e={$_.user.email}},
  @{n='role';  e={$_.user.role}} | Format-List

$token = $login.token
"Longitud del token: $($token.Length)"
```

**Resultado esperado**

```text
tokenType : Bearer
expiresIn : 3600
name      : Admin
email     : admin@ilussiones.com

role      : ADMIN

Longitud del token: 244
```

La longitud exacta puede variar unos caracteres, pero el token debe ser un JWT
con tres partes separadas por puntos y **no** debe contener tu contraseña.

**Qué demuestra:** el backend valida las credenciales, hashea/verifica con BCrypt
y emite un JWT firmado. El frontend no participa en esta decisión.

> No muestres el token completo en una grabación o pantalla compartida: mientras
> no expire, permite entrar a la API.

**Comprobación adicional:** la respuesta no debe incluir `password`,
`passwordHash` ni un hash que empiece por `$2a$`, `$2b$` o `$2y$`. Puedes verlo
con:

```powershell
$raw = $login | ConvertTo-Json -Depth 5
$raw
$raw -match 'password|\$2[aby]\$'
```

`False` significa que no hay fuga de contraseña ni de hash. `True` sería un
problema real.

---

## 5. Prueba 2 — Contraseña incorrecta

```powershell
try {
  Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
    -ContentType 'application/json' `
    -Body (@{ email = 'admin@ilussiones.com'; password = 'clave-incorrecta' } | ConvertTo-Json)
} catch {
  Show-HttpFailure $_
}
```

**Resultado esperado**

```text
401
{"error":"invalid_credentials"}
```

**Qué demuestra:** el backend rechaza la contraseña incorrecta y no emite token.
El mensaje es genérico a propósito.

---

## 6. Prueba 3 — Correo inexistente

```powershell
try {
  Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
    -ContentType 'application/json' `
    -Body (@{ email = 'nadie@ilussiones.com'; password = 'DemoPass123!' } | ConvertTo-Json)
} catch {
  Show-HttpFailure $_
}
```

**Resultado esperado:** exactamente el mismo que la prueba 2:

```text
401
{"error":"invalid_credentials"}
```

**Qué demuestra:** no se revela si la cuenta existe (evita enumeración de
usuarios). Compara ambos cuerpos; deben ser idénticos.

---

## 7. Prueba 4 — Credenciales vacías o ausentes

```powershell
try {
  Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
    -ContentType 'application/json' -Body '{}'
} catch {
  Show-HttpFailure $_
}
```

**Resultado esperado**

```text
400
{"fields":{...},"error":"validation_failed"}
```

El orden de `fields` puede variar; lo importante es que aparezcan `email` y
`password` junto con `validation_failed`.

También puedes probar solo el correo:

```powershell
try {
  Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
    -ContentType 'application/json' -Body '{"email":"","password":""}'
} catch {
  Show-HttpFailure $_
}
```

**Qué demuestra:** Bean Validation rechaza entradas incompletas antes de
autenticar y devuelve qué campos faltan, sin detalles internos.

---

## 8. Prueba 5 — JSON malformado

```powershell
try {
  Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
    -ContentType 'application/json' `
    -Body '{"email":"admin@ilussiones.com",'
} catch {
  Show-HttpFailure $_
}
```

**Resultado esperado**

```text
400
{"reason":"malformed_payload","error":"invalid_request"}
```

**Qué demuestra:** el error de parseo se traduce a un JSON controlado. No aparece
un stack trace de Jackson ni la ruta interna.

---

## 9. Prueba 6 — Endpoint protegido sin token

```powershell
try {
  Invoke-RestMethod -Uri "$base/api/customer"
} catch {
  Show-HttpFailure $_
}
```

**Resultado esperado**

```text
401
{"error":"unauthorized"}
```

Repite con otros endpoints de negocio y con la lectura de auditoría:

```powershell
foreach ($path in '/api/customer', '/api/order', '/api/appointment', '/api/audit/logs') {
  $code = curl.exe -s -o NUL -w "%{http_code}" "$base$path"
  "$path -> $code"
}
```

**Resultado esperado:** `401` en los cuatro.

**Qué demuestra:** las rutas de negocio y la lectura de auditoría exigen
autenticación. La única ruta pública de `/api` es el login y la ingesta de
eventos de auditoría (`POST /api/audit/events`).

---

## 10. Prueba 7 — Endpoint protegido con token válido

Usa el `$token` obtenido en la prueba 1:

```powershell
$headers = @{ Authorization = "Bearer $token" }

$customers = Invoke-RestMethod -Uri "$base/api/customer" -Headers $headers
$customers | ConvertTo-Json -Depth 5
```

**Resultado esperado:** `200` con un arreglo JSON (`[]` si la base está vacía o
con clientes si ya creaste alguno). No hay error de autenticación.

Crea un cliente autenticado para comprobar que el token permite escribir:

```powershell
$nuevo = Invoke-RestMethod -Method Post -Uri "$base/api/customer" `
  -Headers $headers -ContentType 'application/json' `
  -Body (@{ name = 'Cliente Prueba Guia'; phone = '555-0100' } | ConvertTo-Json)

$nuevo | ConvertTo-Json -Depth 5

# Limpieza
$id = $nuevo.id
curl.exe -s -o NUL -w "DELETE -> %{http_code}`n" -X DELETE `
  -H "Authorization: Bearer $token" "$base/api/customer/$id"
```

**Resultado esperado:** `200` al crear (con `id`, `name`, `phone`) y `204` al
borrar.

**Qué demuestra:** el token no solo “existe”: autoriza operaciones reales.

---

## 11. Prueba 8 — Token inválido o manipulado

```powershell
$tampered = $token + 'x'

foreach ($bad in 'no-es-un-token', $tampered) {
  try {
    Invoke-RestMethod -Uri "$base/api/customer" `
      -Headers @{ Authorization = "Bearer $bad" }
  } catch {
    "Token '$($bad.Substring(0, [Math]::Min(12, $bad.Length)))…' -> $([int]$_.Exception.Response.StatusCode)"
  }
}
```

**Resultado esperado**

```text
Token 'no-es-un-to…' -> 401
Token 'eyJ0eXAiOiJ…' -> 401
```

**Qué demuestra:** la firma del JWT se valida; añadir un carácter invalida el
token. No se acepta cualquier cadena.

---

## 12. Prueba 9 — Inyección SQL en el login (`' OR '1'='1`)

Primero en la contraseña:

```powershell
$sqli = @'
' OR '1'='1
'@

try {
  Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
    -ContentType 'application/json' `
    -Body (@{ email = 'admin@ilussiones.com'; password = $sqli } | ConvertTo-Json)
} catch {
  Show-HttpFailure $_
}
```

**Resultado esperado**

```text
401
{"error":"invalid_credentials"}
```

Ahora en el correo:

```powershell
try {
  Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
    -ContentType 'application/json' `
    -Body (@{ email = "admin@ilussiones.com$sqli"; password = 'DemoPass123!' } | ConvertTo-Json)
} catch {
  Show-HttpFailure $_
}
```

**Resultado esperado:** `400` (`validation_failed`, correo inválido), sin token.

**Qué demuestra:** el payload no altera la consulta ni concede acceso. La
contraseña se compara con BCrypt, no se concatena en SQL. Un `200` con token aquí
sería una **vulnerabilidad real**.

Variante con `curl.exe` (archivo JSON para evitar el problema de comillas de
PowerShell):

```powershell
$body = @{ email = 'admin@ilussiones.com'; password = $sqli } | ConvertTo-Json
[IO.File]::WriteAllText("$env:TEMP\login-sqli.json", $body)
curl.exe -s -w "`nHTTP %{http_code}`n" -H "Content-Type: application/json" `
  --data-binary "@$env:TEMP\login-sqli.json" "$base/api/auth/login"
```

---

## 13. Prueba 10 — Entrada inválida e identificadores malformados

Con el token válido:

```powershell
# UUID malformado
curl.exe -s -w "`nHTTP %{http_code}`n" -H "Authorization: Bearer $token" `
  "$base/api/customer/not-a-uuid"

# UUID que no existe
curl.exe -s -w "`nHTTP %{http_code}`n" -H "Authorization: Bearer $token" `
  "$base/api/customer/00000000-0000-0000-0000-000000000000"

# Id de pedido que no existe (el id de pedido es texto)
curl.exe -s -w "`nHTTP %{http_code}`n" -H "Authorization: Bearer $token" `
  "$base/api/order/no-existe"
```

**Resultado esperado**

* `/api/customer/not-a-uuid` → `400` con
  `{"reason":"malformed_parameter","error":"invalid_request"}`.
* UUID inexistente → `404` con **cuerpo vacío** (el controlador responde “no
  encontrado” sin añadir detalles internos).
* Pedido inexistente → `404` con cuerpo vacío.

**Qué demuestra:** los identificadores se convierten/validan antes de tocar la
base y los errores no filtran SQL ni nombres de clases.

### Inyección en la ruta (URL codificada)

Si pegas `' OR '1'='1` sin codificar en una URL, espacios y comillas rompen la
petición y `curl.exe` puede devolver `000`. Eso es un fallo del cliente, **no** una
vulnerabilidad del servidor. Codifica el payload:

```powershell
curl.exe -s -w "`nHTTP %{http_code}`n" -H "Authorization: Bearer $token" `
  "$base/api/customer/%27%20OR%20%271%27%3D%271"
```

**Resultado esperado**

```text
{"reason":"malformed_parameter","error":"invalid_request"}
HTTP 400
```

**Qué demuestra:** el payload se rechaza como identificador inválido; no se
interpreta como SQL.

---

## 14. Prueba 11 — Errores inesperados (500/503) y manejo controlado

Los errores `500` (excepción inesperada) y `503` (base de datos no disponible) se
traducen en `ApiExceptionHandler` a cuerpos genéricos:

```json
{"error":"internal_error"}
{"error":"service_unavailable"}
```

**Cómo verificarlo sin romper tu entorno:** ejecuta la prueba unitaria que invoca
el manejador directamente con excepciones que contienen datos sensibles:

```powershell
cd Backend
.\mvnw.cmd "-Dtest=ApiExceptionHandlerTest" test
```

**Resultado esperado:** `Tests run: 3, Failures: 0, Errors: 0`.

La prueba comprueba que la respuesta **no** contiene el mensaje de la excepción,
nombres de paquetes, `Exception`, direcciones IP ni detalles de conexión.

**Honestidad:** no se puede provocar un `500` real a través de entradas válidas
sin romper el servicio o la base. Por eso este caso se verifica con prueba
automatizada y **no** se presenta como “probado en vivo”. Un `400` o `404` no es
un error inesperado: es un rechazo controlado.

**Extra:** un tipo de contenido no soportado también produce un error controlado:

```powershell
curl.exe -s -w "`nHTTP %{http_code}`n" -X POST -H "Content-Type: text/plain" `
  --data "hola" "$base/api/auth/login"
```

**Resultado esperado:** `415` con
`{"error":"unsupported_media_type"}`, sin stack trace. Este caso se añadió al
manejador durante la revisión de esta guía: antes caía en el `500` genérico.

---

## 15. Cómo interpretar los resultados

| Lo que ves | Significado | ¿Es un problema? |
| --- | --- | --- |
| `200` con token y usuario público | Login correcto | No |
| `204` al borrar | Operación autenticada correcta | No |
| `401 invalid_credentials` con contraseña incorrecta | Rechazo correcto | No |
| `401 invalid_credentials` con correo inexistente, cuerpo idéntico | No revela si la cuenta existe | No |
| `401 unauthorized` sin token o con token inválido | Protección correcta | No |
| `400 validation_failed` con campos faltantes | Validación correcta | No |
| `400 malformed_payload` / `malformed_parameter` | Entrada inválida rechazada | No |
| `404` con id válido pero inexistente (cuerpo vacío) | Recurso no encontrado | No |
| `415` con `Content-Type` incorrecto (`unsupported_media_type`) | Tipo no soportado | No |
| `500 internal_error` genérico | Falla inesperada controlada; revisa logs del servidor | Sí, investigar |
| `503 service_unavailable` | Base de datos no disponible | Sí, revisar infraestructura |
| `000`, timeout, “connection refused” | Servidor apagado, URL/puerto incorrecto | No es vulnerabilidad |
| `200` **con token** usando contraseña incorrecta | El login acepta credenciales inválidas | **Vulnerabilidad real** |
| `200` en un endpoint protegido **sin** token | No hay control de acceso | **Vulnerabilidad real** |
| Respuesta con `password`, `passwordHash`, `$2a$…` o stack trace | Fuga de información sensible | **Vulnerabilidad real** |
| Respuesta con SQL, `org.springframework…` o nombres de tablas | Fuga de internos | **Vulnerabilidad real** |

### Cómo distinguir “éxito” de “no pude probar”

* **Éxito de seguridad:** obtuviste el rechazo esperado (`400/401/403/404/415`)
  con un cuerpo genérico y sin efectos secundarios.
* **Fallo funcional:** esperabas `200` con un token válido y obtuviste `401`.
  Vuelve a iniciar sesión; el token pudo expirar (60 minutos por defecto).
* **No testeable:** no hay servidor, no responde, o el caso requiere romper la
  base (por ejemplo, un `500` real). No lo cuentes como aprobado ni como fallo:
  indícalo como “verificado por prueba automatizada” o “no verificado”.
* **Vulnerabilidad real:** el sistema concede acceso, devuelve datos sensibles o
  revela internos. Repite la prueba dos veces y conserva la salida antes de
  reportarlo.

---

## 16. Comandos con `curl.exe` de principio a fin

Los siguientes comandos usan un archivo para el cuerpo y así evitar el problema
de comillas de PowerShell:

```powershell
$base = 'http://127.0.0.1:18080'
$json = "$env:TEMP\login.json"

# 1) Crear el cuerpo sin BOM
[IO.File]::WriteAllText($json, '{"email":"admin@ilussiones.com","password":"DemoPass123!"}')

# 2) Login y token
$token = (curl.exe -s -H "Content-Type: application/json" `
  --data-binary "@$json" "$base/api/auth/login" | ConvertFrom-Json).token
"Token: $($token.Length) caracteres"

# 3) Sin token
curl.exe -s -o NUL -w "sin token -> %{http_code}`n" "$base/api/customer"

# 4) Con token
curl.exe -s -o NUL -w "con token -> %{http_code}`n" `
  -H "Authorization: Bearer $token" "$base/api/customer"

# 5) Contraseña incorrecta
[IO.File]::WriteAllText($json, '{"email":"admin@ilussiones.com","password":"mala"}')
curl.exe -s -w "`nincorrecta -> %{http_code}`n" -H "Content-Type: application/json" `
  --data-binary "@$json" "$base/api/auth/login"
```

**Resultado esperado:** ~244 caracteres de token, `401` sin token, `200` con
token, `401` con contraseña incorrecta y `{"error":"invalid_credentials"}`.

En Linux, macOS o Git Bash el equivalente es:

```bash
BASE=http://127.0.0.1:18080
TOKEN=$(curl -s -X POST "$BASE/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@ilussiones.com","password":"DemoPass123!"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -s -o /dev/null -w "%{http_code}\n" "$BASE/api/customer"
curl -s -o /dev/null -w "%{http_code}\n" -H "Authorization: Bearer $TOKEN" "$BASE/api/customer"
```

---

## 17. Problemas frecuentes

| Síntoma | Causa probable | Solución |
| --- | --- | --- |
| `malformed_payload` con un JSON que “se ve bien” en `curl.exe` | PowerShell quitó las comillas internas | Usa `Invoke-RestMethod` o `--data-binary "@archivo.json"` |
| `malformed_payload` al leer un archivo | El archivo tiene BOM UTF-8 | Créalo con `[IO.File]::WriteAllText(...)` o `-Encoding ascii` |
| `401` en todas las llamadas tras el login | `$token` está vacío o expiró | Repite la prueba 1 y revisa `$token.Length` |
| `000` en `curl.exe` con SQLi en la URL | Caracteres sin codificar rompen el cliente | Usa la URL codificada (`%27`, `%20`, `%3D`) |
| Error de puerto en uso | Otro proceso usa 18080 | Ciérralo o cambia `server.port` |
| El frontend no inicia sesión contra localhost | `VITE_API_URL` apunta a Railway | Ejecuta `$env:VITE_API_URL='http://127.0.0.1:18080'; npm run dev` |
| `404` al pedir un id “que existe” | La base del perfil `httptest` es nueva en cada arranque | Crea el recurso primero o usa el id devuelto al crearlo |

---

## 18. Checklist de la práctica

Marca cada punto solo cuando viste el resultado con tus propios ojos:

- [ ] El backend arrancó y `GET /api/customer` sin token devuelve `401`.
- [ ] El login correcto devuelve `200`, `tokenType: Bearer` y un JWT.
- [ ] La respuesta del login no contiene `password`, `passwordHash` ni `$2a$`.
- [ ] Contraseña incorrecta y correo inexistente devuelven el mismo `401`.
- [ ] Credenciales vacías devuelven `400 validation_failed`.
- [ ] JSON malformado devuelve `400 invalid_request` / `malformed_payload`.
- [ ] Sin token, con token inválido y con token manipulado: `401`.
- [ ] Con token válido: `200` y una operación de escritura funciona.
- [ ] `' OR '1'='1` como contraseña devuelve `401`, no un token.
- [ ] `' OR '1'='1` codificado en la ruta devuelve `400`.
- [ ] UUID malformado devuelve `400`; UUID inexistente devuelve `404`.
- [ ] `ApiExceptionHandlerTest` pasa en verde (3 pruebas).
- [ ] Puedes explicar la diferencia entre `401/400`, `000` y una vulnerabilidad.

Si todo lo anterior se cumple, la autenticación y el manejo seguro de errores
están funcionando en tu entorno. Si algo falla, conserva el comando, el código de
estado y el cuerpo de la respuesta: eso permite distinguir un problema de
configuración de una vulnerabilidad real.
