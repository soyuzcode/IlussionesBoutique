# SECURITY.md

Postura de seguridad del backend de Ilussiones Boutique (Spring Boot 4.1.1, Java 25).
Alcance: la API de negocio (`/api/customer`, `/api/appointment`, `/api/order`),
el login (`/api/auth/login`), la lectura de auditoría (`/api/audit/logs`) y la
ingesta de eventos (`/api/audit/events`).

## 1. Autenticación y autorización

La autenticación es **real y la decide el backend**. Ya no existe el login
simulado del frontend: `Login.tsx` envía las credenciales a
`POST /api/auth/login`, guarda el token emitido y lo adjunta como
`Authorization: Bearer <JWT>` en las peticiones protegidas. El frontend no
compara contraseñas ni decide por su cuenta si un acceso es válido.

`SecurityConfig` configura una API **sin estado**:

* `POST /api/auth/login` es público.
* `POST /api/audit/events` sigue siendo público porque el frontend emite eventos
  antes del login; su cuerpo está acotado (4 KB) y tiene límite de tasa.
* Todo el resto de `/api/**` (negocio y lectura de auditoría) exige un JWT válido.
* CSRF está deshabilitado de forma deliberada porque no se usan cookies de
  sesión; la credencial viaja en la cabecera `Authorization`. Si en el futuro se
  adoptan sesiones con cookie, CSRF debe reactivarse.
* No se usa HTTP Basic ni formularios de login de Spring.

Los JWT son HS256, los firma `JwtTokenService` y los valida el
`JwtDecoder` de Spring Security antes de llegar al controlador. Vida por defecto:
60 minutos (`app.auth.token-ttl-minutes`). No hay refresh ni revocación: al ser
sin estado, un token robado es válido hasta que expira.

## 2. Cuenta demo y hashing

Al no existir todavía un almacén de usuarios utilizable por la API, se implementó
**una sola cuenta de demostración configurada solo en el backend**:

| Propiedad | Variable de entorno | Valor de desarrollo |
| --- | --- | --- |
| `app.auth.demo.email` | `APP_DEMO_AUTH_EMAIL` | `admin@ilussiones.com` |
| `app.auth.demo.password` | `APP_DEMO_AUTH_PASSWORD` | `Ilussiones2026!` |
| `app.auth.demo.name` | `APP_DEMO_AUTH_NAME` | `Admin` |
| `app.auth.demo.role` | `APP_DEMO_AUTH_ROLE` | `ADMIN` |
| `app.auth.jwt-secret` | `APP_AUTH_JWT_SECRET` | secreto de desarrollo |

En cualquier entorno compartido se deben sobrescribir **todas** con variables de
entorno. La contraseña nunca se escribe en la base de datos: al arrancar,
`DemoAccountService` la hashea con **BCrypt** (`BCryptPasswordEncoder`) y solo
conserva el hash en memoria. La verificación usa `PasswordEncoder.matches()` y
compara el correo en tiempo constante. Un correo desconocido y una contraseña
incorrecta producen exactamente la misma respuesta `401 invalid_credentials`, y
la verificación BCrypt se ejecuta en ambos casos, para no revelar si la cuenta
existe. La aplicación falla al arrancar si falta una propiedad obligatoria o si
el secreto JWT mide menos de 32 bytes.

La contraseña, el hash y el token nunca se registran en logs ni se devuelven en
la respuesta de login; la respuesta solo incluye `token`, `tokenType`,
`expiresIn` y `{name, email, role}`. El endpoint responde con `Cache-Control:
no-store`.

**Importante:** la tabla `public."user"` sigue sin estar mapeada por JPA y su
único `password_hash` no tiene formato bcrypt/argon2. Esos valores son legado y
**no se usan para autenticar**; el login demo no consulta la base. Si en el
futuro se persisten usuarios reales, toda contraseña nueva debe hashearse con el
mismo `PasswordEncoder` antes de guardarse.

## 3. Validación de entrada y errores

* **Bean Validation** (`@NotBlank`, `@Email`, `@Size`, `@NotNull`, límites de
  `Product.price`) rechaza credenciales y payloads malformados con `400`.
* **Reglas de servicio** (`BusinessValidationException`) revalidan la entidad
  completa tras aplicar un PUT parcial y comprueban integridad referencial
  (`customer_not_found`, etc.).
* `ApiExceptionHandler` devuelve códigos cortos y estables:
  `validation_failed`, `invalid_request`, `malformed_parameter`,
  `malformed_payload`, `data_conflict`, `service_unavailable`,
  `internal_error`. Nunca incluye stack traces, paquetes de Spring/Hibernate,
  SQL ni el valor rechazado.

## 4. Inyección y datos

* Toda la persistencia pasa por Spring Data JPA / Hibernate con parámetros
  enlazados; la aplicación no concatena SQL.
* Los payloads de inyección en cuerpos JSON se guardan como datos inertes y se
  devuelven sin cambios. En variables de ruta se rechazan con `400
  malformed_parameter` (UUID) o no encuentran filas (`404`).
* Las mismas pruebas se aplican al login: `' OR '1'='1` como contraseña devuelve
  `401 invalid_credentials`; como correo no pasa la validación (`400`). Ninguna
  variante emite token.
* `ddl-auto=validate` se mantiene: Hibernate no crea ni altera tablas en
  producción.

## 5. Auditoría y base de datos

Las operaciones de negocio y sus rechazos se registran en `audit_log`, protegida
por los triggers `audit_log_no_update` / `audit_log_no_delete`. La ingesta admite
solo eventos de UI incluidos en una lista blanca, con claves de metadatos
acotadas.

RLS está habilitado en las 16 tablas `public` pero el backup no incluye
`CREATE POLICY`; el backend entra con un rol privilegiado que lo evita. Cualquier
acceso directo desde Supabase con `anon`/`authenticated` requiere políticas
explícitas.

## 6. Secretos

Credenciales de base de datos y backups están en `.gitignore`. Los valores de
desarrollo de `application.properties` no son secretos de producción y deben
sobrescribirse en Railway/Supabase. No se commitean tokens, contraseñas reales ni
dumps.

## 7. Verificación ejecutada

```powershell
cd Backend
.\mvnw.cmd test    # 136 pruebas, 0 fallos, 0 errores — BUILD SUCCESS
```

Suites relevantes:

* `AuthApiTest` (15): login correcto, contraseña incorrecta, correo inexistente,
  credenciales vacías, correo inválido, JSON malformado, tipo de contenido no
  soportado, SQLi en correo y contraseña, token ausente/inválido/manipulado,
  acceso autorizado a negocio y a auditoría, y ausencia de fuga del
  hash/contraseña.
* `DemoAccountServiceTest` (6): hash BCrypt al arrancar, `matches()`, sal distinta
  por hash, normalización del correo, resultados indistinguibles y fallo rápido
  por configuración incompleta.
* `BusinessApiSecurityTest` (21): inyección, validación, errores controlados,
  `401` anónimo, token inválido y acceso autenticado.
* `ApiExceptionHandlerTest` (3): los errores `500/503/409` no filtran detalles.

También se verificó en vivo con `curl` contra el perfil `httptest` (H2, puerto
18080): login `200`, contraseña incorrecta y correo inexistente `401` idénticos,
credenciales vacías `400`, JSON malformado `400`, sin token `401`, token inválido
`401`, token válido `200`, SQLi `401/400` y creación autenticada de un cliente
con limpieza posterior. El procedimiento reproducible está en
`GUIA_PRUEBAS_SEGURIDAD.md`.

## 8. Limitaciones y recomendaciones

1. **Cuenta demo única, no un sistema de identidad.** Para producción se
   recomienda Supabase Auth (o un proveedor OIDC) y validar sus JWT con las
   claves públicas correspondientes.
2. **Sin roles efectivos.** El JWT incluye `role`, pero hoy toda petición
   autenticada puede usar todos los endpoints. Si se necesita autorización por
   rol, añadir `hasRole(...)` en los matchers y pruebas por rol.
3. **Sin rate limiting en el login.** Añadir un límite por IP/cuenta y
   bloqueo progresivo.
4. **Sin revocación ni refresh de tokens.** Tokens de 60 minutos; valorar
   rotación o lista de revocación si el riesgo lo exige.
5. **CORS abierto** (`@CrossOrigin(origins = "*")`). Restringirlo al dominio del
   frontend en producción.
6. **RLS sin políticas y hash legado** en `public."user"`; no usar esa tabla para
   autenticación hasta migrarla.

## 9. Resumen

| Área | Estado |
| --- | --- |
| Autenticación backend con BCrypt y JWT | Implementada (cuenta demo) |
| Endpoints de negocio y lectura de auditoría protegidos | Implementado |
| CSRF | Deshabilitado a propósito (API sin cookies) |
| Validación y errores controlados | Implementado |
| Resistencia a inyección | Implementada (JPA con parámetros) |
| Auditoría append-only | Implementada (triggers) |
| Fuga de hash/contraseña/token en respuestas | Ausente (verificado por pruebas) |
| Autorización por rol, rate limiting, revocación | Pendiente |
| RLS con políticas / hash legado de `"user"` | Pendiente |
