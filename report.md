# Ilussiones Boutique — Engineering Report

**Project:** Ilussiones Boutique (boutique management app)
**Stack:** Spring Boot 4.1.1 · Java 25 · Maven backend; React 19 frontend; Supabase PostgreSQL
**Branch:** `feature/security`
**Date:** 2026-11-11

This report covers the database-model/ERD deliverable, the unit-test and
security-test suites, and the supporting fixes and documentation produced for
the checkpoint. All figures below come from commands actually executed in this
environment; where something could not be verified, it is stated explicitly.

---

## 1. Objective

Deliver, without changing the existing architecture or behaviour:

1. A documented **database model + ERD** (8-point item).
2. **Unit tests** for the business logic (6-point item).
3. **Security tests** for the API surface (6-point item).
4. A concise **`SECURITY.md`** and this **`report.md`**.

Constraints respected: `ddl-auto=validate` unchanged, no DB constraints/RLS/auth
disabled or bypassed, tests run through Maven/JUnit only, tests use an isolated
database (no live Supabase or seeded records), and no credentials/secrets/dumps
or real customer data were committed.

---

## 2. Database model & ERD

### 2.1 Source and method

The model was extracted from the plain-SQL backup
`docs/database/db_cluster-03-11-2026@16-11-56.backup` (read-only; never modified,
renamed, deleted, or committed; now git-ignored). `pg_restore`/`psql`/`docker`
are **not** installed here, so the schema was transcribed by hand into
documentation — it was **not executed against a live PostgreSQL instance**.

Deliverables in `docs/database/`:

| File | Purpose |
| --- | --- |
| `schema.sql` | Consolidated `public` DDL: enums, tables, PK/FK, checks, indexes, RLS, triggers. Documentation only. |
| `erd.mmd` | Mermaid ER diagram. |
| `README.md` | Table/relationship overview, integrity rules, RLS notes, Java↔DB gaps. |

### 2.2 Model summary

Three layers plus cross-cutting concerns:

1. **Catalogue** — `product` + JOINED subtype tables (`album`, `card`, `cup`,
   `dress`, `plush`), each keyed by `product_id`.
2. **Ordering** — normalised aggregate `"order"` (header) + `order_item`,
   `order_dress`, `payment`. The legacy `orders` / `order_products` tables also
   exist and are what the `Order` entity maps.
3. **People & scheduling** — `customer`, `"user"` (staff), `appointment`.
4. **Cross-cutting** — append-only `audit_log`.

Enums: `appointment_status`, `order_status`, `payment_status`,
`product_status`, `product_type`, `user_role`.

Key relationships:

* `appointment.customer_id → customer.id` **ON DELETE CASCADE**
* `"order".customer_id → customer.id` and `"order".user_id → "user".id` **RESTRICT**
* `order_item.order_id → "order".id` **CASCADE**, `order_item.product_id → product.id` **RESTRICT**
* `payment.order_id → "order".id` **CASCADE**, `payment.user_id → "user".id` **SET NULL**
* subtype tables cascade from `product`

### 2.3 Integrity enforced in the database

* `audit_log` is append-only via `audit_log_no_update` / `audit_log_no_delete` triggers.
* Check constraints bound every `audit_log` text column (format regexes,
  enumerated `actor_type`/`entity_type`/`event_source`/`outcome`, JSON-object
  metadata capped at 4 KB).
* `order_item` must not reference a `DRESS` product (`trg_prevent_dress_in_order_item`).
* Non-negative/positive checks on `price`, `stock`, `total_amount`,
  `agreed_price`, `unit_price`, `quantity`, `payment.amount`.
* `customer.status` ∈ {`ACTIVO`, `FINALIZADO`} (default `ACTIVO`).

### 2.4 Row-Level Security

RLS is **enabled on all 16 `public` tables**, but the backup contains **no
`CREATE POLICY` statements**. The Spring backend connects with a privileged role
that bypasses RLS, which is why the API works; any direct Supabase (anon /
authenticated) access will need explicit policies.

### 2.5 Java model vs. database (documented gaps, not "fixed")

| Gap | Detail |
| --- | --- |
| Subtypes not entities | `Product` is `@Inheritance(JOINED)` but `Album`/`Card`/`Cup`/`Dress`/`Plush` are not `@Entity`, so subtype tables are unreachable through JPA. |
| `Product` columns | Maps only `id`, `price`, `photo_url`, `materials`, `status`. `type`, `name`, `stock`, `created_at`, `updated_at` are unmapped. |
| `Customer.status` | Present in DB (default `ACTIVO`), absent from the `Customer` entity. |
| Two order models | `Order` maps legacy `orders`/`order_products`; normalised `"order"`, `order_item`, `order_dress`, `payment` are unmapped. |
| `User` not an entity | `"user"` has no JPA mapping. |
| `password_hash` format | The single stored value is **not** a bcrypt/argon2 modular hash; staff password verification is unverified (value never printed). |

These are intentionally left as documentation rather than silently remodelled,
to avoid changing behaviour.

---

## 3. Unit tests (business logic)

The business tests use an isolated H2 database (`test` profile), so they need
neither Supabase nor seeded data. Key suites:

| Suite | Type | Focus |
| --- | --- | --- |
| `BusinessRepositoryJpaTest` | `@DataJpaTest` | Repository CRUD, lookups, FK behaviour |
| `BusinessServiceRulesTest` | Service unit/integration | Validation rules, partial updates, not-found, delete, metadata |
| `PersistenceAcrossRestartTest` | Two programmatic Spring contexts | Data survives a restart against a file-backed H2 |
| `BusinessAuditInstrumentationTest` | Service + audit | Success/failure/rejection events are recorded |
| `BusinessAuditTransactionIntegrationTest` | Integration | Rollback semantics and genuine persistence failures |
| `IlussionessBackendApplicationTests` | Context | Application context loads |

Highlights of the rules exercised:

* Customer create/update/delete, name/phone trimming, blank/over-long rejection,
  merge-on-partial-PUT (unspecified fields preserved).
* Appointment requires an existing customer and date/time; unknown customer is
  rejected (`customer_not_found`); status defaults to `SCHEDULED`.
* Order requires `orderId` and `eventType`; negative prices rejected; partial
  update keeps existing values.
* Delete returns `false` → controller returns `404`; genuine persistence failure
  surfaces (e.g. 300-char `googleEventId` exceeding `varchar(255)`).

**Result:** `.\mvnw.cmd test` → **109 tests, 0 failures, 0 errors** (BUILD SUCCESS).

---

## 4. Security tests (API surface)

`BusinessApiSecurityTest` is an HTTP-level MockMvc suite. Because
`SecurityConfig` uses `anyRequest().permitAll()`, these tests document the
reachable surface and prove hostile input is rejected safely.

| # | Scenario | Expected |
| --- | --- | --- |
| 1 | SQLi payload in JSON body (`'; DROP TABLE customer;--`) | `200`, stored inert, table intact |
| 2 | SQLi in UUID path variable | `400 invalid_request` / `malformed_parameter` |
| 3 | SQLi in order id path | `404`, no rows touched |
| 4 | Malformed UUID path | `400 malformed_parameter` |
| 5 | Malformed JSON body | `400 malformed_payload` |
| 6 | Blank / missing / oversized customer name | `400 validation_failed` with field detail |
| 7 | Appointment with unknown customer | `400 customer_not_found` |
| 8 | Appointment missing required fields | `400 validation_failed` |
| 9 | Order without `eventType` | `400 validation_failed` |
| 10 | Unknown ids on GET/DELETE/PUT | `404` |
| 11 | Full + partial customer PUT | `200` |
| 12 | Partial appointment/order PUT | `200` (regression coverage) |
| 13 | Error bodies do not leak internals | no `Exception`, Spring/Hibernate names, SQL |
| 14 | Unauthenticated request | allowed (pins documented `permitAll()` posture) |

### 4.1 Live `curl` confirmation

A running instance (H2, profile `httptest`, port 18080) was exercised with
`curl`. Observed, unauthenticated:

* `GET /api/customer` → `200`; `POST` valid customer → `200`.
* Blank name → `400` (`validation_failed`, `fields.name`); missing phone → `400`
  (`fields.phone`); oversized name → `400` "name must be at most 120 characters".
* Malformed UUID → `400 invalid_request` / `malformed_parameter`; unknown UUID → `404`.
* Appointment unknown customer → `400 customer_not_found`; missing fields → `400`.
* Order missing `eventType` → `400`.
* SQLi body stored as inert data (visible unchanged via `GET`).
* Malformed JSON → `400 malformed_payload`.
* Order POST/GET → `200`; DELETE → `204`; subsequent GET → `404`.
* Audit `UI_SECTION_VIEWED` → `202`; backend-only business event sent by a client
  → `400 event_type_not_allowed`.

### 4.2 Bug found and fixed

Live testing surfaced a real contract bug: the frontend sends **partial** PUT
bodies (e.g. orders send only `{eventType, deliveryDate}`; appointments send only
`{reason, appointmentDate, appointmentTime}`), but the controllers had `@Valid`
on the PUT body, so a partial payload failed validation with `400`. Fix:

* Removed `@Valid` from all three PUT endpoints.
* Services merge non-null fields into the stored entity, then validate the
  merged result (`CustomerService`, `AppointmentService`, `OrderService`).
* A separate bug in `AuditEventType.ORDER_UPDATED` (missing `PRODUCT_COUNT`
  metadata key) was also fixed — otherwise a successful order update was logged
  as a rejected event.

**Re-verified live after the fix:** partial `PUT /api/order/{id}` `{eventType}` →
`200`; partial `PUT /api/appointment/{id}` `{reason}` → `200`; partial
`PUT /api/customer/{id}` `{name}` → `200` with phone preserved; cleanup DELETEs →
`204`.

---

## 5. Rubric compliance (estimate)

> **This matrix is an estimate.** It reflects what was implemented and verified
> in this working tree; it is not an official grade.

| Rubric item | Points | Status | Evidence |
| --- | --- | --- | --- |
| Database model + ERD | 8 | Done (transcribed, not live-verified) | `docs/database/schema.sql`, `erd.mmd`, `README.md` |
| Unit tests | 6 | Done | 5 business suites; full run 109 tests / 0 failures |
| Security tests | 6 | Done | `BusinessApiSecurityTest` (18 tests) + live `curl` |
| `SECURITY.md` | — | Done | `SECURITY.md` |
| `report.md` | — | Done | this file |

Caveats: the ERD/schema are a faithful transcription of the backup, not a
verified restore (no PostgreSQL client available). Authentication is not
implemented (documented limitation). RLS has no policies.

---

## 6. Limitations & recommendations

1. **No authentication/authorization** — `anyRequest().permitAll()`, CSRF
   disabled. Add Supabase JWT verification and authenticated matchers.
2. **RLS enabled, no policies** — define policies before exposing Supabase
   directly to `anon`/`authenticated`.
3. **Non-standard staff password hash** — migrate to bcrypt/argon2.
4. **Schema drift** — align the JPA entities with the verified schema (subtype
   entities, `Customer.status`, normalised order tables) in a dedicated change.
5. **No live-DB restore verification** — install a PostgreSQL client to validate
   `schema.sql` against a scratch database.

---

## 7. How to reproduce

```powershell
# Full test suite (isolated H2, no Supabase needed)
cd Backend
.\mvnw.cmd test          # 109 tests, 0 failures

# Optional live HTTP run (H2, port 18080)
.\mvnw.cmd spring-boot:test-run "-Dspring-boot.run.profiles=httptest"
# then curl http://127.0.0.1:18080/api/customer
```

---

## 8. Files added / changed (high level)

**New:** `docs/database/{schema.sql,erd.mmd,README.md}`,
`ApiExceptionHandler`, `BusinessValidationException`,
`BusinessRepositoryJpaTest`, `BusinessServiceRulesTest`,
`BusinessApiSecurityTest`, `PersistenceAcrossRestartTest`,
`application-httptest.properties`, `SECURITY.md`, `report.md`.

**Changed:** `Customer`/`Appointment`/`Order`/`Product` models (validation),
`CustomerService`/`AppointmentService`/`OrderService` (CRUD + validation +
auditing), the three controllers (partial PUT), `AuditEventType`
(`ORDER_UPDATED` metadata), existing audit tests, `.gitignore` (dump rules).
