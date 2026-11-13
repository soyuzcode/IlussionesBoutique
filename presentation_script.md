# Guion de presentación — Ilussiones Boutique

**Proyecto:** Ilussiones Boutique — sistema de gestión para boutique
**Equipo:** KEY CODE
**Stack:** React 19 + Vite + TypeScript · Spring Boot 4.1.1 (Java 25, Maven) · PostgreSQL (Supabase)
**Duración objetivo:** 11 min 30 s de exposición + 30 s de margen = **12 min máximo**
**Expositores:** exactamente 5
**Evidencia real de pruebas:** `.\mvnw.cmd test` → **136 pruebas, 0 fallos, 0 errores — BUILD SUCCESS**

> Nota: los nombres "Expositor 1…5" son marcadores. Reemplácenlos por los
> integrantes reales antes de ensayar. No asignen a una persona un tema que no
> pueda defender: quien expone debe haber leído el archivo que muestra.

---

## Reparto y cronograma

| # | Tema | Inicio | Fin | Duración |
|---|------|--------|-----|----------|
| 1 | Introducción, contexto y arquitectura | 0:00 | 2:00 | 2:00 |
| 2 | Modelo de base de datos y ERD | 2:00 | 4:30 | 2:30 |
| 3 | Repositorios, CRUD, persistencia y transacciones | 4:30 | 6:50 | 2:20 |
| 4 | Pruebas unitarias | 6:50 | 9:10 | 2:20 |
| 5 | Pruebas de seguridad y conclusión | 9:10 | 11:30 | 2:20 |
| — | Margen para preguntas / cierre | 11:30 | 12:00 | 0:30 |

**Regla de oro:** si un expositor se pasa 10 segundos, el siguiente recorta su
introducción, no su evidencia. Practiquen con cronómetro y con las diapositivas
reales.

---

## Expositor 1 — Introducción, contexto y arquitectura

**Tiempo estimado:** 2:00 (0:00–2:00)
**Objetivo:** que la audiencia entienda qué es la aplicación, qué problema
resuelve, qué se evalúa en este checkpoint y cómo está organizada la arquitectura.

**Diapositiva sugerida:** título del proyecto + diagrama simple:
`React (Vite/TS) → API REST (Spring Boot) → PostgreSQL (Supabase)`.

### Guion (palabras exactas)

> Buenos días. Somos el equipo KEY CODE y les presentamos Ilussiones Boutique, un
> sistema de gestión para una boutique de vestidos y detalles personalizados.
>
> El problema es concreto: la boutique maneja clientes, citas de prueba, pedidos,
> inventario y pagos, y antes mucho de eso vivía en hojas de cálculo y mensajes
> sueltos. Eso provoca citas duplicadas, pedidos perdidos y poca trazabilidad.
> Nuestra aplicación centraliza esos procesos en una sola interfaz web.
>
> El objetivo de este checkpoint es demostrar tres cosas: el modelo de base de
> datos, las pruebas unitarias y las pruebas de seguridad. Vamos a mostrar lo que
> está implementado y también lo que falta.
>
> La arquitectura es desacoplada. El frontend es React 19 con Vite y TypeScript:
> la interfaz donde se registran clientes, citas y pedidos. El backend es una API
> REST en Spring Boot 4.1.1 con Java 25 y Maven. La base de datos es PostgreSQL en
> Supabase.
>
> El flujo siempre es el mismo: el navegador hace una petición HTTP, el
> controlador la recibe, el servicio aplica las reglas de negocio y el repositorio
> habla con la base. [Señalar el diagrama] Tenemos tres controladores de negocio
> —Customer, Appointment y Order—, un módulo de auditoría aparte y un
> controlador de autenticación que emite el token de acceso.
>
> Esa separación importa: el controlador no toca la base de datos; solo el
> servicio y el repositorio lo hacen. El frontend se comunica por HTTPS y hoy
> apunta a la URL de producción en Railway.
>
> Con eso, le paso a mi compañero para el modelo de datos.

### Demostración

No hay demo en vivo en esta parte. Solo se señala el diagrama de arquitectura.

**Transición:** "Le paso a mi compañero para el modelo de datos."

---

## Expositor 2 — Modelo de base de datos y ERD

**Tiempo estimado:** 2:30 (2:00–4:30)
**Objetivo:** explicar las 16 tablas, los cuatro bloques del dominio, las
relaciones, las llaves primarias y foráneas, y cómo se verificó el esquema.

**Diapositiva sugerida:** el ERD renderizado + una tabla corta de PK/FK. Tener
también `docs/database/schema.sql` abierto por si preguntan.

### Guion (palabras exactas)

> Gracias. Voy a explicar el modelo de datos y el ERD.
>
> La base tiene 16 tablas en el esquema público, agrupadas en cuatro bloques.
> Primero, el catálogo: `product` y cinco subtipos —`album`, `card`, `cup`,
> `dress` y `plush`— con herencia JOINED. Cada subtipo comparte la llave primaria
> de `product` con `product_id`, que además es foránea con borrado en cascada. Un
> vestido y un álbum son productos, pero guardan sus atributos propios.
>
> Segundo, personas y agenda: `customer`, la tabla `"user"` para el personal y
> `appointment`. Una cita pertenece a un cliente con `customer_id`, y esa foránea
> es `ON DELETE CASCADE`: si se borra el cliente, se borran sus citas.
>
> Tercero, los pedidos, y aquí quiero ser honesto: conviven dos modelos. El
> normalizado tiene `"order"` como cabecera, `order_item`, `order_dress` y
> `payment`. Además existen `orders` y `order_products`, las tablas legacy que hoy
> mapea la entidad `Order` de Java. En el modelo normalizado, `customer_id` y
> `user_id` son `ON DELETE RESTRICT`, y `order_item` cuelga de la orden en
> cascada. Hay un trigger que impide meter un vestido en `order_item`: los
> vestidos van en `order_dress`.
>
> Cuarto, `audit_log`, que es append-only: triggers que rechazan `UPDATE` y
> `DELETE`, y restricciones que limitan formato y el JSON de metadatos a 4 KB.
>
> Sobre llaves: la mayoría usa UUID con `gen_random_uuid()`. Las tablas puente
> como `order_item` usan llave compuesta, y `orders` usa un `order_id` de texto.
>
> [Señalar el ERD] Aquí está el ERD. Fíjense en las cardinalidades: un cliente
> tiene muchas citas y pedidos; un pedido tiene muchas líneas; un producto
> aparece en muchas líneas.
>
> ¿Cómo lo verificamos? Extrajimos el esquema del backup en SQL plano en
> `docs/database/` y lo documentamos en `schema.sql` y `erd.mmd`. No lo
> restauramos contra PostgreSQL porque no tenemos `psql` ni `pg_restore` en el
> entorno. Mantenemos `ddl-auto=validate`: Hibernate valida contra la base real,
> no crea ni modifica tablas.
>
> Dejamos documentada una deuda técnica: los subtipos de `Product` no son
> `@Entity`, `Customer` no mapea `status` y `Order` usa las tablas legacy. No lo
> cambiamos para no alterar el comportamiento.

### Demostración — ERD

- **Qué mostrar:** `docs/database/erd.mmd` renderizado.
- **Cómo:** abrir <https://mermaid.live> y pegar el contenido del archivo, o usar
  la vista previa Mermaid de VS Code. Tener un PNG de respaldo por si falla el
  internet.
- **Qué debe observar la audiencia:** las cardinalidades y las etiquetas de las
  foráneas; que `PRODUCT` se conecta con sus cinco subtipos, y que `CUSTOMER`
  apunta a `APPOINTMENT` y a `ORDER`.
- **Qué decir mientras se muestra:** "Aquí pueden ver que `CUSTOMER` es uno a
  muchos con `APPOINTMENT`, y que cada subtipo comparte la llave de `PRODUCT`."

**Transición:** "Con eso, le paso a mi compañero para hablar de repositorios y
persistencia."

---

## Expositor 3 — Repositorios, CRUD, persistencia y transacciones

**Tiempo estimado:** 2:20 (4:30–6:50)
**Objetivo:** explicar la capa DAO/repositorios, el CRUD, la separación
controlador-servicio-persistencia, las transacciones y la prueba de durabilidad.

**Diapositiva sugerida:** diagrama de capas + fragmentos de
`AppointmentRepository` y de un método de servicio con `@Transactional`.

### Guion (palabras exactas)

> Gracias. Ahora bajo una capa: cómo se guarda y se recupera la información.
>
> Usamos Spring Data JPA. Los repositorios son interfaces que extienden
> `JpaRepository`. `CustomerRepository` extiende `JpaRepository<Customer, UUID>`;
> `AppointmentRepository` también, y le agregamos `findByCustomerId`, un método
> derivado que Spring traduce a una consulta. `OrderRepository` extiende
> `JpaRepository<Order, String>` porque en las tablas legacy el id es texto. Con
> eso tenemos CRUD completo sin escribir SQL: `save`, `findById`, `findAll`,
> `existsById`, `deleteById` y `count`.
>
> La separación es estricta. Los controladores solo reciben la petición y
> devuelven la respuesta; no conocen repositorios. Los servicios —`CustomerService`,
> `AppointmentService`, `OrderService`— son dueños de la lógica y de las
> transacciones. Por ejemplo, `createOrder` valida, guarda con `saveAndFlush`,
> registra la auditoría y devuelve la entidad. Si algo falla, hay rollback.
>
> [Mostrar `CustomerService`] Aquí se ve el patrón: `@Transactional` en escritura
> y `@Transactional(readOnly = true)` en lectura. Usamos `saveAndFlush` a
> propósito: obliga a la base a aplicar las restricciones dentro de la transacción
> y no al final.
>
> Un detalle que corregimos: el frontend manda actualizaciones parciales. Al
> editar un pedido solo envía `eventType` y `deliveryDate`, no el `orderId`. Con
> `@Valid` en el PUT, eso fallaba con 400. Quitamos `@Valid` y el servicio ahora
> mezcla solo los campos no nulos sobre la entidad existente y valida el
> resultado. Así se mantiene la regla de negocio y el contrato real del frontend.
>
> También hay una prueba de durabilidad: `PersistenceAcrossRestartTest` levanta
> dos contextos de Spring contra la misma H2 en archivo. El primero crea un
> cliente y se cierra; el segundo arranca desde cero y lo vuelve a leer. Eso
> demuestra que los datos sobreviven a un reinicio, lo más parecido a un
> despliegue sin tocar Supabase.
>
> Para cerrar: no hay SQL concatenado; todo pasa por JPA e Hibernate con
> parámetros enlazados. Eso es clave para la seguridad.

### Demostración — repositorio y servicio

- **Qué mostrar:** `Backend/src/main/java/.../repositories/AppointmentRepository.java`
  y `.../services/CustomerService.java` (métodos `createCustomer` y
  `updateCustomer`).
- **Cómo:** abrir los archivos en el editor; no escribir código en vivo.
- **Qué debe observar la audiencia:** `extends JpaRepository`, el método derivado
  `findByCustomerId`, las anotaciones `@Transactional` y el uso de
  `saveAndFlush`.
- **Qué decir:** "Esta es toda la implementación del repositorio: Spring genera el
  CRUD. Y aquí, en el servicio, está la transacción y la validación."

**Transición:** "Le paso a mi compañero para las pruebas unitarias."

---

## Expositor 4 — Pruebas unitarias

**Tiempo estimado:** 2:20 (6:50–9:10)
**Objetivo:** explicar el framework, el comando de ejecución, el aislamiento de la
base de prueba, la cobertura CRUD, las reglas de negocio, los casos límite y el
resultado real.

**Diapositiva sugerida:** tabla de suites y conteos + el comando
`.\mvnw.cmd test` + el resultado "136 pruebas, 0 fallos".

### Guion (palabras exactas)

> Gracias. Vamos a las pruebas.
>
> Usamos JUnit 5, Spring Boot Test, AssertJ para las aserciones y MockMvc para las
> pruebas HTTP. Todo se ejecuta con Maven con un solo comando: en Windows
> `.\mvnw.cmd test`; en Linux o Mac, `./mvnw test`.
>
> La base de prueba está aislada. El perfil `test` usa H2 en memoria en modo
> PostgreSQL con `create-drop`: se crea al arrancar y se borra al terminar. Cada
> clase usa una URL propia para que no se pisen, y no tocamos Supabase ni datos
> sembrados a mano. En las pruebas de repositorio usamos `@DataJpaTest` con
> `@AutoConfigureTestDatabase(replace = NONE)` para que use nuestra H2.
>
> ¿Qué cubrimos? Primero, CRUD a nivel de repositorio en `BusinessRepositoryJpaTest`,
> con 9 pruebas: crear, buscar, actualizar y borrar clientes; persistir una cita
> con su cliente y su enum; filtrar citas por cliente; y guardar pedidos con id de
> texto. Por ejemplo, `customerIsPersistedAndFoundById` guarda un cliente, limpia
> el `EntityManager` y lo vuelve a buscar: así demostramos que viene de la base y
> no de la caché.
>
> Segundo, reglas de negocio en `BusinessServiceRulesTest`, con 19 pruebas:
> recorte de espacios, nombre y teléfono obligatorios, longitudes fuera de límite,
> cita con cliente existente y fecha y hora, estado por defecto `SCHEDULED`,
> pedido con `eventType`, y actualizar un id inexistente que devuelve vacío. Cada
> rechazo comprueba que no quedó ninguna fila.
>
> Tercero, casos límite: nombre en blanco, teléfono nulo, 121 caracteres, UUID
> inexistente, y un fallo real de persistencia cuando un `googleEventId` de 300
> caracteres excede el `varchar(255)`.
>
> Además hay pruebas de auditoría: validación de eventos, persistencia, filtros de
> tamaño y rate limit, e instrumentación de los servicios. Y para la
> autenticación añadimos 15 pruebas HTTP y 6 de la cuenta demo: hash BCrypt,
> `matches()`, token válido, token inválido, credenciales incorrectas y ausencia
> de fugas.
>
> [Mostrar la salida] El resultado real es 136 pruebas, 0 fallos, 0 errores,
> BUILD SUCCESS. No es una estimación: es la salida de Maven.

### Demostración — ejecución de pruebas

- **Recomendado:** mostrar una **captura o salida ya ejecutada** con las últimas
  líneas: `Tests run: 136, Failures: 0, Errors: 0` y `BUILD SUCCESS`. Es lo más
  seguro dentro de 12 minutos.
- **Si se hace en vivo:** `cd Backend` y `.\mvnw.cmd test`. Tarda alrededor de
  uno o dos minutos; **no lo hagan sin haberlo cronometrado**. Filtrar la salida
  para mostrar solo el resumen final.
- **Qué debe observar la audiencia:** el total de pruebas y los ceros en fallos y
  errores.
- **Qué decir:** "Esta es la salida de Maven: 136 pruebas, cero fallos."

**Transición:** "Le paso a mi compañero para la parte de seguridad."

---

## Expositor 5 — Pruebas de seguridad y conclusión

**Tiempo estimado:** 2:20 (9:10–11:30)
**Objetivo:** explicar prevención de inyección SQL, el payload `' OR '1'='1`,
validación de entrada, autenticación con BCrypt y JWT, protección de endpoints,
manejo de errores, evidencia de pruebas y conclusiones honestas.

**Diapositiva sugerida:** tabla "amenaza → resultado observado" + captura del
login/token + lista de limitaciones.

### Guion (palabras exactas)

> Gracias. Cierro con seguridad y conclusiones.
>
> Empezamos por inyección SQL. No concatenamos SQL: usamos JPA con parámetros
> enlazados. En `BusinessApiSecurityTest`, `'; DROP TABLE customer;--` se guarda
> como dato inerte y la tabla sigue existiendo. `' OR '1'='1` en una ruta devuelve
> 400 o 404, nunca una consulta válida.
>
> Usamos Bean Validation y reglas de servicio con códigos como `name_required`.
> `ApiExceptionHandler` traduce a JSON controlado: 400, 404, 409, 415, 503 y 500,
> sin stack traces, paquetes ni SQL.
>
> Ahora la autenticación. Antes el login era simulado con un usuario en texto
> plano; eso ya no existe. El frontend envía correo y contraseña a
> `POST /api/auth/login`. El backend tiene una cuenta demo configurada por
> variables de entorno, hashea su contraseña con BCrypt al arrancar y la verifica
> con `matches()`. Nunca guarda ni devuelve la contraseña o el hash. Correo
> inexistente y contraseña incorrecta dan el mismo 401, para no revelar si la
> cuenta existe.
>
> Si son válidas emite un JWT HS256 de 60 minutos. El login es público; el resto
> de `/api/**` exige `Authorization: Bearer`: sin token o manipulado, 401; con
> token válido, 200. `' OR '1'='1` como contraseña también da 401.
>
> Limitaciones honestas: es una cuenta demo, no un sistema de identidad; no hay
> autorización por rol, rate limiting ni revocación; RLS sigue sin políticas y el
> hash legado de `"user"` no se usa.
>
> En resumen: autenticación con BCrypt y JWT, validación, errores controlados,
> auditoría y 136 pruebas en verde. Gracias. ¿Preguntas?

### Demostración — login y control de acceso

- **Qué mostrar:** `AuthApiTest.java`, los métodos
  `successfulLoginReturnsABearerTokenAndPublicUser` y
  `protectedEndpointRejectsMissingToken`; y una captura del resultado.
- **Cómo:** abrir el archivo en el editor. Si ya está probado, mostrar una
  captura del login con `curl` (200 con token) y del mismo endpoint sin token
  (401). **No** improvisar `curl` en vivo salvo que ya esté probado.
- **Qué debe observar la audiencia:** que el login devuelve `tokenType: Bearer`
  sin contraseña ni hash, y que sin token el endpoint responde 401.
- **Qué decir:** "El backend valida las credenciales y emite el token; sin token,
  el endpoint de negocio no responde."
- **Nota de honestidad:** el `curl` con la URL de inyección **sin codificar**
  devuelve `000` por el propio cliente, no por la aplicación. Para la demo usen
  la prueba automatizada o la URL codificada.

**Transición final:** "Gracias. ¿Preguntas?" (No hay expositor 6.)

---

## Estrategia de demostraciones (resumen)

| Momento | Qué mostrar | Cómo abrirlo | Qué debe observar la audiencia |
|---|---|---|---|
| 3:50 aprox. | ERD | `docs/database/erd.mmd` en mermaid.live o vista previa de VS Code | Cardinalidades, subtipos de `PRODUCT`, foráneas |
| 6:20 aprox. | Repositorio y servicio | `AppointmentRepository.java`, `CustomerService.java` | `JpaRepository`, `findByCustomerId`, `@Transactional`, `saveAndFlush` |
| 8:50 aprox. | Resultado de pruebas | Captura de `.\mvnw.cmd test` | 136 pruebas, 0 fallos, BUILD SUCCESS |
| 10:50 aprox. | Login y control de acceso | `AuthApiTest.java` + captura de `curl` | Login 200 con token; sin token 401 |

**Regla:** máximo una demostración por expositor, de 10 a 20 segundos. Si algo no
está abierto antes de empezar, se muestra la captura y se sigue. Nunca se escribe
código en vivo.

---

## Checklist de ensayo

### 1. Tiempos

| Expositor | Tema | Tiempo | Ensayo 1 | Ensayo 2 |
|---|---|---|---|---|
| 1 | Introducción y arquitectura | 2:00 | ☐ | ☐ |
| 2 | Base de datos y ERD | 2:30 | ☐ | ☐ |
| 3 | Repositorios y persistencia | 2:20 | ☐ | ☐ |
| 4 | Pruebas unitarias | 2:20 | ☐ | ☐ |
| 5 | Seguridad y conclusión | 2:20 | ☐ | ☐ |
| — | **Total** | **11:30** | ☐ | ☐ |

### 2. Comandos para preparar

```powershell
# Ejecutar toda la suite (dejar la salida lista para mostrar)
cd Backend
.\mvnw.cmd test

# Opcional: instancia local con H2 para curl (no toca Supabase)
.\mvnw.cmd spring-boot:test-run "-Dspring-boot.run.profiles=httptest"
# credenciales demo: admin@ilussiones.com / DemoPass123!
# luego: http://127.0.0.1:18080/api/customer (sin token -> 401)
```

Para renderizar el ERD: abrir <https://mermaid.live> y pegar
`docs/database/erd.mmd`, o usar la vista previa Mermaid de VS Code.

### 3. Archivos y capturas que deben estar listos

- `docs/database/erd.mmd` (y un PNG de respaldo).
- `docs/database/schema.sql` y `docs/database/README.md`.
- `Backend/.../repositories/AppointmentRepository.java`.
- `Backend/.../services/CustomerService.java`.
- `BusinessRepositoryJpaTest.java`, `BusinessServiceRulesTest.java`,
  `PersistenceAcrossRestartTest.java`.
- `BusinessApiSecurityTest.java`.
- `SECURITY.md` y `report.md`.
- Captura del resumen de `.\mvnw.cmd test` (136 / 0 / 0 / BUILD SUCCESS).
- Captura del login con token y del endpoint protegido sin token; y de la prueba
  de inyección si se usa.
- `AuthApiTest.java`, `DemoAccountServiceTest.java` y `SECURITY.md`.
- `GUIA_PRUEBAS_SEGURIDAD.md` por si piden el procedimiento completo.

### 4. Preguntas probables de los evaluadores (y respuestas)

1. **¿Qué pasa si borro un cliente con citas y pedidos?**
   Las citas se borran en cascada (`appointment.customer_id ON DELETE CASCADE`).
   En el modelo normalizado, `"order".customer_id` es `ON DELETE RESTRICT`, así
   que la base impide borrar un cliente con pedidos. La tabla legacy `orders` no
   tiene columna de cliente, por lo que no impone esa restricción.

2. **¿Por qué no usaron `ddl-auto=update`?**
   En producción usamos `validate` para que Hibernate solo verifique que las
   entidades coinciden con el esquema, sin modificarlo. En pruebas usamos
   `create-drop` sobre una H2 desechable.

3. **¿Cómo garantizan que las pruebas no tocan producción?**
   El perfil `test` apunta a H2 en memoria en modo PostgreSQL, con URL única por
   clase y `@ActiveProfiles("test")`. No se usan credenciales de Supabase ni datos
   sembrados a mano.

4. **¿Por qué el ERD no coincide al 100 % con las entidades JPA?**
   Es deuda técnica documentada: los subtipos de `Product` no son `@Entity`,
   `Customer` no mapea `status`, `Order` usa las tablas legacy y `User` no es
   entidad. No lo cambiamos para no alterar el comportamiento; está en
   `docs/database/README.md`.

5. **¿Cómo evitan la inyección SQL en un campo de texto?**
   Todo pasa por JPA/Hibernate con parámetros enlazados; no concatenamos SQL. La
   prueba con `'; DROP TABLE customer;--` demuestra que el valor se guarda como
   dato inerte y la tabla sigue existiendo.

6. **¿Cómo manejan las contraseñas?**
   El backend hashea con BCrypt la contraseña de la cuenta demo al arrancar y la
   verifica con `matches()`. No se guarda en la base ni se devuelve en la
   respuesta. El login del frontend ya no compara contraseñas: siempre pregunta
   al backend. El hash legado de `public."user"` no es bcrypt/argon2 y no se usa;
   si se migra esa tabla, hay que rehashear.

7. **¿La API está autenticada?**
   Sí. `POST /api/auth/login` es público; el resto de `/api/**` exige
   `Authorization: Bearer <JWT>`, validado por Spring Security. La ingesta de
   auditoría sigue pública porque el frontend emite eventos antes del login.
   Queda pendiente la autorización por rol, el rate limiting y la revocación.

8. **¿Qué es RLS y por qué no hay políticas?**
   RLS es Row Level Security de PostgreSQL: filtra filas por rol. Está habilitado
   en las 16 tablas, pero el backup no trae `CREATE POLICY`. El backend entra con
   un rol privilegiado que lo salta; para acceso directo desde Supabase habría que
   definir políticas.

9. **¿Qué pasa si dos usuarios actualizan el mismo registro?**
   No hay bloqueo optimista (`@Version`) implementado; hoy gana la última
   escritura. Es una mejora pendiente que podemos agregar con control de versión.

10. **¿Cuántas pruebas tienen y qué cubren?**
    136 en total: 9 de repositorio, 19 de reglas de servicio, 1 de persistencia
    entre reinicios, 21 de seguridad HTTP, 15 de autenticación HTTP y 6 de la
    cuenta demo, más las suites de auditoría (validación, persistencia, filtros,
    ingesta e instrumentación). Resultado: 0 fallos, 0 errores, BUILD SUCCESS.

11. **¿Por qué una cuenta demo y no usuarios de la base?**
    Porque la tabla `"user"` no está mapeada por JPA y su hash legado no es
    bcrypt/argon2. Para no inventar un almacén de usuarios ni tocar Supabase, la
    cuenta demo vive en configuración del backend, con variables de entorno. Esa
    decisión está documentada; migrar a Supabase Auth es el siguiente paso.

### 5. Consejos finales

- No digan "todo está seguro": digan qué está probado y qué falta.
- Cada vez que mencionen un archivo, ténganlo abierto.
- Si una pregunta no se sabe, responder: "No lo implementamos; está documentado
  como pendiente en `SECURITY.md` / `report.md`." Eso suma credibilidad.
- Ensayen la transición de 5 segundos entre expositores: es lo que más se pierde
  en presentaciones de equipo.
