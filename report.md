# Ilussiones Boutique — Informe de ingeniería

**Proyecto:** Ilussiones Boutique (gestión para boutique)
**Stack:** Spring Boot 4.1.1 · Java 25 · Maven (backend); React 19 + Vite + TypeScript (frontend); PostgreSQL en Supabase
**Rama:** `feature/security`
**Fecha:** 2026-11-13

Este informe cubre el modelo de base de datos y su ERD, las pruebas unitarias, las
pruebas de seguridad, la **autenticación real con BCrypt y JWT** incorporada en
este checkpoint y la documentación de apoyo. Todas las cifras provienen de
comandos ejecutados en este entorno; cuando algo no se pudo verificar, se indica
explícitamente.

---

## 1. Objetivo

1. Documentar el **modelo de base de datos + ERD**.
2. Añadir **pruebas unitarias** de la lógica de negocio.
3. Ampliar las **pruebas de seguridad** de la API.
4. Implementar **autenticación de backend** con hashing seguro, sin credenciales
   incrustadas en el frontend y sin almacenar contraseñas en texto plano.
5. Mantener `SECURITY.md`, `report.md`, el guion de presentación y una
   `GUIA_PRUEBAS_SEGURIDAD.md` en español.

Restricciones respetadas: `ddl-auto=validate` sin cambios, sin desactivar
restricciones ni RLS, pruebas con Maven/JUnit sobre una base aislada (sin Supabase
ni datos sembrados) y sin commitear credenciales, secretos, dumps ni datos reales.

---

## 2. Modelo de base de datos y ERD

### 2.1 Fuente y método

El modelo se extrajo del backup SQL plano
`docs/database/db_cluster-03-11-2026@16-11-56.backup` (solo lectura; nunca se
modificó, renombró ni commiteó; está en `.gitignore`). En este entorno no hay
`pg_restore`/`psql`/`docker`, así que el esquema se transcribió a mano: **no se
ejecutó contra un PostgreSQL vivo**.

Entregables en `docs/database/`:

| Archivo | Propósito |
| --- | --- |
| `schema.sql` | DDL consolidado del esquema `public`: enums, tablas, PK/FK, checks, índices, RLS, triggers. Solo documentación. |
| `erd.mmd` | Diagrama ER en Mermaid. |
| `README.md` | Resumen de tablas, relaciones, integridad, RLS y brechas Java↔BD. |

### 2.2 Resumen del modelo

Cuatro bloques:

1. **Catálogo** — `product` y los subtipos JOINED `album`, `card`, `cup`, `dress`,
   `plush`, todos con `product_id` como PK/FK y borrado en cascada.
2. **Pedidos** — modelo normalizado `"order"` + `order_item`, `order_dress`,
   `payment`; además existen las tablas legacy `orders` y `order_products`, que
   son las que mapea la entidad `Order`.
3. **Personas y agenda** — `customer`, `"user"` (personal) y `appointment`.
4. **Transversal** — `audit_log` append-only.

Enums: `appointment_status`, `order_status`, `payment_status`,
`product_status`, `product_type`, `user_role`.

Relaciones clave:

* `appointment.customer_id → customer.id` **ON DELETE CASCADE**
* `"order".customer_id → customer.id` y `"order".user_id → "user".id` **RESTRICT**
* `order_item.order_id → "order".id` **CASCADE**; `order_item.product_id → product.id` **RESTRICT**
* `payment.order_id → "order".id` **CASCADE**; `payment.user_id → "user".id` **SET NULL**
* Los subtipos hacen cascada desde `product`.

### 2.3 Integridad en la base

* `audit_log` append-only mediante `audit_log_no_update` y `audit_log_no_delete`.
* Checks que acotan columnas de texto de `audit_log` (regex, enumeraciones, JSON
  de metadatos ≤ 4 KB).
* `order_item` no puede referenciar un producto `DRESS`
  (`trg_prevent_dress_in_order_item`).
* Checks de no negatividad/positividad en precios, stock, cantidades y pagos.
* `customer.status ∈ {ACTIVO, FINALIZADO}` (por defecto `ACTIVO`).

### 2.4 Row-Level Security

RLS está **habilitado en las 16 tablas** `public`, pero el backup **no contiene
`CREATE POLICY`**. El backend conecta con un rol privilegiado que lo evita; por
eso la API funciona. Cualquier acceso directo con `anon`/`authenticated` necesita
políticas explícitas.

### 2.5 Brechas entre el modelo Java y la base (documentadas, no "arregladas")

| Brecha | Detalle |
| --- | --- |
| Subtipos no son entidades | `Product` usa `@Inheritance(JOINED)`, pero `Album`/`Card`/`Cup`/`Dress`/`Plush` no son `@Entity`. |
| Columnas sin mapear | `Product` solo mapea `id`, `price`, `photo_url`, `materials`, `status`; faltan `type`, `name`, `stock`, timestamps. |
| `Customer.status` | Existe en la BD, no en la entidad. |
| Dos modelos de pedido | `Order` usa las tablas legacy; el modelo normalizado no está mapeado. |
| `User` no es entidad | `"user"` no tiene mapeo JPA. |
| `password_hash` legado | El único valor almacenado no es bcrypt/argon2 y no se usa para autenticar (nunca se imprime). |

Se dejaron como deuda documentada para no cambiar el comportamiento.

---

## 3. Autenticación implementada

### 3.1 Situación de partida

No existía autenticación de backend: `SecurityConfig` usaba
`anyRequest().permitAll()` con CSRF deshabilitado, y `Login.tsx` validaba contra
un arreglo `MOCK_USERS` con la contraseña `123456` en texto plano, además de un
botón de Google que concedía acceso sin validar nada. `User` era una clase
abstracta sin mapeo JPA.

### 3.2 Solución (caso B: cuenta demo en el backend)

* `AuthProperties` (`app.auth.*`) permite configurar por variables de entorno el
  secreto JWT, la vida del token y la cuenta demo
  (`APP_DEMO_AUTH_EMAIL`, `APP_DEMO_AUTH_PASSWORD`, …). Los valores por defecto
  son solo para desarrollo y están documentados.
* `DemoAccountService` hashea la contraseña configurada con **BCrypt** al
  arrancar, conserva solo el hash en memoria y verifica con
  `PasswordEncoder.matches()`. Correo desconocido y contraseña incorrecta
  devuelven el mismo `401 invalid_credentials`; la verificación se ejecuta en
  ambos casos y el correo se compara en tiempo constante.
* `JwtTokenService` emite JWT HS256 firmados con el secreto configurado
  (mínimo 32 bytes, validado al arrancar) con `sub`, `name`, `role`, `iat` y
  `exp`. La vida por defecto es 60 minutos.
* `AuthController` expone `POST /api/auth/login`, valida el cuerpo con Bean
  Validation y responde `200` con `{token, tokenType, expiresIn, user}` o `401`
  genérico. Añade `Cache-Control: no-store` y no devuelve contraseña ni hash.
* `SecurityConfig` valida el Bearer con el `JwtDecoder` de Spring Security.
  Público: login y `POST /api/audit/events`. Protegido: el resto de `/api/**`.
  La API es sin estado (`SessionCreationPolicy.STATELESS`), sin formularios ni
  HTTP Basic.
* Frontend: se eliminó `MOCK_USERS` y el acceso simulado con Google. `authApi`
  llama al backend; `authSession` guarda token y usuario en `sessionStorage`;
  `httpClient` añade `Authorization: Bearer`; `App` restaura la sesión y la borra
  al cerrar sesión; la tabla de auditoría también envía el token. El frontend
  nunca decide si unas credenciales son válidas ni almacena hashes.

### 3.3 Decisiones y límites

* Se reutilizó la dependencia `spring-security-oauth2-jose` ya presente; **no se
  añadieron librerías nuevas**. No se inventó un segundo formato de token.
* No se persisten usuarios nuevos: la cuenta demo vive en configuración. Si se
  añadiera persistencia, toda contraseña nueva debe pasar por el mismo
  `PasswordEncoder`.
* El `role` viaja en el token, pero todavía no se aplican reglas por rol.
* El "olvidé mi contraseña" del frontend sigue siendo un mensaje de UI sin envío
  real de correo; no concede acceso.
* CSRF sigue deshabilitado a propósito porque no hay cookies de sesión. CORS
  permanece abierto (`@CrossOrigin(origins = "*")`) como deuda a restringir.

### 3.4 Incidencias encontradas y corregidas

1. **Enlace de propiedades:** `app.auth.jwt.secret` no se enlazaba con el campo
   `jwtSecret` (la forma canónica es `jwt-secret`), lo que hacía fallar el
   contexto de Spring. Se corrigió en `application.properties`, en los perfiles
   de prueba y en `SecurityConfig`.
2. **Bypass de autenticación en el frontend:** el botón de Google concedía sesión
   sin backend. Se eliminó junto con `MOCK_USERS`.
3. **Auditoría sin token:** `AuditLogTable` usaba `fetch` directo; al proteger
   `/api/**` habría recibido `401`. Ahora envía el Bearer.

---

## 4. Pruebas unitarias y de integración

Las pruebas usan H2 en memoria en modo PostgreSQL con `create-drop` (perfil
`test`), sin Supabase ni datos sembrados. Suites y conteos verificados:

| Suite | Tipo | Enfoque | Pruebas |
| --- | --- | --- | --- |
| `BusinessRepositoryJpaTest` | `@DataJpaTest` | CRUD, búsquedas, FK | 9 |
| `BusinessServiceRulesTest` | Servicio/integración | Validación, PUT parcial, 404, borrado | 19 |
| `PersistenceAcrossRestartTest` | Dos contextos Spring | Durabilidad entre reinicios | 1 |
| `BusinessAuditInstrumentationTest` | Servicio + auditoría | Eventos de éxito/rechazo | 20 |
| `BusinessAuditTransactionIntegrationTest` | Integración | Rollback y fallos reales de persistencia | 4 |
| `AuditEventValidatorTest` | Unidad | Lista blanca y metadatos | 13 |
| `AuditServicePersistenceTest` | Persistencia | Registro y consultas de auditoría | 8 |
| `AuditWebFilterTest` | Web | Tamaño, tasa y sanitización | 9 |
| `AuditIngestionIntegrationTest` | HTTP | Ingesta válida/rechazada | 7 |
| `AuthApiTest` | HTTP | Login, tokens, 415 y acceso | 15 |
| `DemoAccountServiceTest` | Unidad | BCrypt, `matches()`, configuración | 6 |
| `ApiExceptionHandlerTest` | Unidad | 500/503/409 sin fugas | 3 |
| `BusinessApiSecurityTest` | HTTP | Inyección, validación, auth | 21 |
| `IlussionessBackendApplicationTests` | Contexto | Carga de la aplicación | 1 |
| **Total** | | | **136** |

**Resultado real:** `.\mvnw.cmd test` → **136 pruebas, 0 fallos, 0 errores,
0 omitidas — BUILD SUCCESS**. Antes de este checkpoint eran 109; se añadieron 27
(15 de autenticación HTTP, 6 de la cuenta demo, 3 del manejador de errores y 4 de
la frontera de autenticación en la suite de seguridad, menos la prueba que fijaba
el antiguo `permitAll()`).

Casos cubiertos por `AuthApiTest`: login correcto con token y usuario público;
contraseña incorrecta; correo inexistente con respuesta idéntica a contraseña
incorrecta; credenciales vacías; correo en blanco o inválido; JSON malformado;
`Content-Type` no soportado; SQLi en contraseña y en correo; endpoint protegido
sin token, con token inválido y con token manipulado; acceso autorizado a negocio
y a lectura de auditoría; credenciales inválidas que no conceden acceso; y
ausencia de contraseña, hash o internos en las respuestas.

---

## 5. Pruebas de seguridad HTTP

`BusinessApiSecurityTest` corre autenticado con `@WithMockUser` para concentrarse
en la entrada hostil, y tiene pruebas anónimas específicas:

| # | Escenario | Resultado esperado |
| --- | --- | --- |
| 1 | SQLi en cuerpo JSON (`'; DROP TABLE customer;--`) | `200`, dato inerte, tabla intacta |
| 2 | SQLi en path UUID | `400 invalid_request` / `malformed_parameter` |
| 3 | SQLi en id de pedido | `404`, sin filas afectadas |
| 4 | UUID malformado | `400 malformed_parameter` |
| 5 | JSON malformado | `400 malformed_payload` |
| 6 | Nombre de cliente vacío/faltante/excedido | `400 validation_failed` con campo |
| 7 | Cita con cliente inexistente | `400 customer_not_found` |
| 8 | Cita sin campos obligatorios | `400 validation_failed` |
| 9 | Pedido sin `eventType` | `400 validation_failed` |
| 10 | GET/DELETE/PUT con id inexistente | `404` |
| 11 | PUT completo y parcial de cliente | `200` |
| 12 | PUT parcial de cita y pedido | `200` |
| 13 | Errores sin filtrar internos | sin `Exception`, Spring, Hibernate ni SQL |
| 14 | Petición anónima a negocio | `401 unauthorized` |
| 15 | Token Bearer inválido | `401 unauthorized` |
| 16 | SQLi en login | `401 invalid_credentials` |
| 17 | Petición autenticada | `200` |

### 5.1 Verificación en vivo con `curl`

Se levantó la aplicación real con el perfil `httptest` (H2 aislado, puerto 18080)
y se ejecutaron las peticiones de la guía. Resultados observados:

* Login correcto → `200`, `tokenType: Bearer`, `expiresIn: 3600` y usuario
  público; sin contraseña ni hash en el cuerpo.
* Contraseña incorrecta y correo inexistente → `401` con el **mismo** cuerpo
  `{"error":"invalid_credentials"}`.
* Credenciales vacías → `400 validation_failed` con `fields.email` y
  `fields.password`.
* JSON malformado → `400 invalid_request` / `malformed_payload`.
* SQLi como contraseña → `401 invalid_credentials`.
* `GET /api/customer` sin token → `401 unauthorized`.
* Token inválido → `401 unauthorized`.
* Token válido → `200` con la lista de clientes.
* SQLi codificado en path UUID → `400 invalid_request` /
  `malformed_parameter`.
* Crear cliente con token → `200`; borrado posterior → `204` (limpieza).

### 5.2 Bug de contrato ya corregido (se mantiene)

El frontend envía PUT parciales; quitar `@Valid` de los PUT y hacer
merge-then-validate en los servicios evita el `400` espurio y conserva las reglas
de negocio. También se corrigió `AuditEventType.ORDER_UPDATED`, que no declaraba
la clave `PRODUCT_COUNT` que su propio evento exigía.

---

## 6. Verificación del frontend

* `npm run build` (`tsc -b && vite build`) → **correcto**, 42 módulos, sin errores.
* `npm run lint` → **4 errores preexistentes** de `react-hooks` en `App.tsx` y
  `App_fixed.tsx` (setState dentro de un efecto y `Date.now()` en render). No se
  introdujeron en este cambio y no se tocaron para no alterar comportamiento
  fuera del alcance de autenticación.
* Búsqueda de credenciales: no quedan `MOCK_USERS`, `123456`, comparaciones de
  contraseña ni `admin@ilussiones.com` en `Frontend/src`.

---

## 7. Cumplimiento del rubric (estimación)

> **Esta matriz es una estimación** de lo implementado y verificado en este árbol
> de trabajo; no es una calificación oficial.

| Ítem | Puntos | Estado | Evidencia |
| --- | --- | --- | --- |
| Modelo de BD + ERD | 8 | Hecho (transcrito, no verificado en vivo) | `docs/database/` |
| Pruebas unitarias | 6 | Hecho | 136 pruebas / 0 fallos |
| Pruebas de seguridad | 6 | Hecho | `BusinessApiSecurityTest` + `AuthApiTest` + `curl` |
| Autenticación y hashing | — | Hecho (cuenta demo) | `AuthController`, `DemoAccountService`, JWT |
| `SECURITY.md` / `report.md` | — | Hecho | este informe y `SECURITY.md` |
| Guía de pruebas | — | Hecho | `GUIA_PRUEBAS_SEGURIDAD.md` |

---

## 8. Limitaciones y recomendaciones

1. **Cuenta demo única** — no es un sistema de identidad. Migrar a Supabase Auth
   u OIDC y validar sus JWT.
2. **Sin autorización por rol** — añadir matchers `hasRole` y pruebas por rol.
3. **Sin rate limiting de login** — añadir límite por IP/cuenta.
4. **Sin revocación/refresh** — tokens de 60 minutos; valorar rotación.
5. **CORS abierto** — restringirlo al dominio del frontend.
6. **RLS sin políticas** — definirlas antes del acceso directo desde Supabase.
7. **Hash legado en `public."user"`** — migrar a bcrypt/argon2 si se usa esa
   tabla para login.
8. **Deriva de esquema** — alinear entidades JPA con el esquema verificado
   (subtipos, `Customer.status`, pedidos normalizados).
9. **Restauración no verificada** — instalar un cliente PostgreSQL para validar
   `schema.sql` contra una base de prueba.

---

## 9. Cómo reproducir

```powershell
# Suite completa (H2 aislada, no necesita Supabase)
cd Backend
.\mvnw.cmd test          # 136 pruebas, 0 fallos, 0 errores

# Instancia local para la guía de seguridad (H2, puerto 18080)
.\mvnw.cmd spring-boot:test-run "-Dspring-boot.run.profiles=httptest"

# Frontend
cd ..\Frontend
npm install
npm run build
npm run dev
```

Credenciales de la cuenta demo en `httptest`: `admin@ilussiones.com` /
`DemoPass123!`. En el perfil normal se leen de `APP_DEMO_AUTH_EMAIL` y
`APP_DEMO_AUTH_PASSWORD` (con valores de desarrollo si no se definen).

---

## 10. Archivos añadidos y modificados (alto nivel)

**Nuevos (backend):** `auth/{AuthProperties, AuthenticatedUser, DemoAccountService,
JwtTokenService, LoginRequest, LoginResponse, AuthController}.java`,
`auth/DemoAccountServiceTest.java`, `auth/AuthApiTest.java`,
`controllers/ApiExceptionHandlerTest.java`.

**Modificados (backend):** `config/SecurityConfig.java` (JWT, BCrypt, matchers),
`controllers/ApiExceptionHandler.java` (login incluido),
`src/main/resources/application.properties` y perfiles de prueba (propiedades de
autenticación), `controllers/BusinessApiSecurityTest.java` (frontera de
autenticación), `audit/api/AuditIngestionIntegrationTest.java` (lectura de
auditoría autenticada).

**Nuevos (frontend):** `api/authApi.ts`, `lib/authSession.ts`.

**Modificados (frontend):** `components/common/Login.tsx` (sin `MOCK_USERS` ni
Google simulado; llama al backend), `api/httpClient.ts` (Bearer),
`components/tables/AuditLogTable.tsx` y `lib/api.ts` (Bearer), `App.tsx`
(sesión persistida y cierre que limpia el token), `types/index.ts` (`role`).

**Nuevos (docs):** `GUIA_PRUEBAS_SEGURIDAD.md`.

**Actualizados (docs):** `SECURITY.md`, `report.md`, `presentation_script.md`.

**Previos conservados:** `docs/database/{schema.sql,erd.mmd,README.md}`,
validación/auditoría, pruebas de repositorio/servicio/persistencia y correcciones
de PUT parcial.
