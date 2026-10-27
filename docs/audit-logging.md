# Audit logging

Append-oriented audit trail for Ilussiones Boutique.

- Backend: Spring Boot (`Backend/src/main/java/com/keycode/ilussioness_backend/audit`)
- Frontend: typed, failure-safe client (`Frontend/src/lib/audit.ts`)
- Database: one manual SQL script (`Backend/database/audit_log.sql`)

The audit table is **never** created or modified by Hibernate. Production keeps
`spring.jpa.hibernate.ddl-auto=validate`; the SQL script is executed manually in
the Supabase SQL editor.

---

## 1. Manual database step

1. Open the Supabase project SQL editor.
2. Execute `Backend/database/audit_log.sql` once.
3. Restart the backend. Hibernate validates the new table and fails fast if the
   script and the entity mapping disagree.
4. Do not change `spring.jpa.hibernate.ddl-auto` and do not let Hibernate update
   the schema.

The script creates `public.audit_log`, indexes, RLS with no `anon`/`authenticated`
access, and `BEFORE UPDATE`/`BEFORE DELETE` triggers that reject mutation.

The Spring Boot JDBC user (`postgres.<project-ref>`) is the table owner and
bypasses RLS. The triggers apply to the owner as well. If a non-owner application
role is introduced later, grant it only `INSERT` (plus `SELECT` for operational
queries) and add an explicit RLS policy; see the comments in the SQL script.

---

## 2. What is recorded

| Field | Meaning |
| --- | --- |
| `id` | Server-generated UUID. |
| `occurred_at` | Server-generated UTC timestamp (`timestamptz`). Never accepted from a client. |
| `event_type` | One of the application-defined event types below. |
| `action` | Coarse action derived from `event_type` on the server. |
| `actor_type` | Always `ANONYMOUS` until trusted authentication exists. |
| `actor_id` | Always `NULL`; the application has no trusted user identity yet. |
| `entity_type` | Bounded business entity category. |
| `entity_id` | Optional opaque business id (max 128 safe characters). |
| `event_source` | `FRONTEND` for UI observations, `BACKEND` for completed/failed business operations. |
| `outcome` | `INFO` for UI observations; `SUCCESS`, `FAILURE` or `REJECTED` only from the backend. |
| `session_id` | Optional browser-tab correlation id (8-64 URL-safe characters). Not an identity. |
| `request_id` | Server-generated correlation id, also returned as `X-Request-Id`. |
| `metadata` | Small, allow-listed, non-sensitive JSON object (max 10 entries / 2 KiB client-side, 4 KiB database check). |

### Event catalog

UI events (accepted from the browser, stored as `FRONTEND` / `INFO` /
`ANONYMOUS`):

| Event type | Action | Entity | Metadata | Emitted when |
| --- | --- | --- | --- | --- |
| `UI_SESSION_STARTED` | `LOGIN` | `AUTH` | – | Interface login succeeds (the current login is UI-only). |
| `UI_LOGIN_SUBMITTED` | `LOGIN` | `AUTH` | `AUTH_METHOD` (`PASSWORD`, `GOOGLE`) | Login form or Google button is submitted. No email/password is sent. |
| `UI_LOGOUT_CLICKED` | `LOGOUT` | `AUTH` | – | Logout button is clicked. |
| `UI_SECTION_VIEWED` | `NAVIGATE` | `APPLICATION` | `SECTION` | A main section is opened. |
| `UI_PRODUCT_CATALOG_VIEWED` | `VIEW` | `PRODUCT` | `SECTION` | The inventory/catalog section is opened. |
| `UI_APPOINTMENT_DETAIL_VIEWED` | `VIEW` | `APPOINTMENT` | – | An appointment detail modal is opened. |
| `UI_APPOINTMENT_FORM_OPENED` | `OPEN` | `APPOINTMENT` | – | The new-appointment form is opened. |
| `UI_APPOINTMENT_FORM_VALIDATION_FAILED` | `VALIDATION_FAILED` | `APPOINTMENT` | `FIELD` (`CLIENT`, `TIME`), `REASON` (`REQUIRED`, `FORMAT`) | Client-side form validation rejects the form. |
| `UI_APPOINTMENT_FORM_SUBMITTED` | `SUBMIT` | `APPOINTMENT` | – | The validated form is submitted. |
| `UI_APPOINTMENT_CREATE_FAILED` | `ERROR` | `APPOINTMENT` | `REASON` | A non-HTTP workflow error prevents appointment creation. |
| `UI_API_REQUEST_FAILED` | `REQUEST_FAILED` | `CUSTOMER`, `APPOINTMENT`, `ORDER` | `OPERATION`, `ERROR_CODE`, optional `HTTP_STATUS` | A frontend API request fails (network, timeout, HTTP error, invalid response). |
| `UI_UNEXPECTED_ERROR` | `ERROR` | `APPLICATION` | `ERROR_NAME` | Uncaught window error or unhandled promise rejection. |

Business events (backend only; the ingestion endpoint rejects them):

| Event type | Action | Entity | Outcome | Metadata |
| --- | --- | --- | --- | --- |
| `CUSTOMER_CREATED` | `CREATE` | `CUSTOMER` | `SUCCESS` | – |
| `CUSTOMER_CREATE_FAILED` | `CREATE` | `CUSTOMER` | `FAILURE` | `REASON` |
| `CUSTOMER_UPDATED` | `UPDATE` | `CUSTOMER` | `SUCCESS` | – |
| `CUSTOMER_UPDATE_REJECTED` | `UPDATE` | `CUSTOMER` | `REJECTED` | `REASON=NOT_FOUND` |
| `CUSTOMER_UPDATE_FAILED` | `UPDATE` | `CUSTOMER` | `FAILURE` | `REASON` |
| `CUSTOMER_DELETED` | `DELETE` | `CUSTOMER` | `SUCCESS` | – |
| `CUSTOMER_DELETE_REJECTED` | `DELETE` | `CUSTOMER` | `REJECTED` | `REASON=NOT_FOUND` |
| `CUSTOMER_DELETE_FAILED` | `DELETE` | `CUSTOMER` | `FAILURE` | `REASON` |
| `APPOINTMENT_CREATED` | `CREATE` | `APPOINTMENT` | `SUCCESS` | `CUSTOMER_ID`, `APPOINTMENT_STATUS` |
| `APPOINTMENT_CREATE_FAILED` | `CREATE` | `APPOINTMENT` | `FAILURE` | `REASON` |
| `ORDER_CREATED` | `CREATE` | `ORDER` | `SUCCESS` | `PRODUCT_COUNT`, `HAS_DELIVERY_DATE` |
| `ORDER_CREATE_FAILED` | `CREATE` | `ORDER` | `FAILURE` | `REASON` |
| `ORDER_DELETED` | `DELETE` | `ORDER` | `SUCCESS` | – |
| `ORDER_DELETE_REJECTED` | `DELETE` | `ORDER` | `REJECTED` | `REASON=NOT_FOUND` |
| `ORDER_DELETE_FAILED` | `DELETE` | `ORDER` | `FAILURE` | `REASON` |

### Metadata allow-list

| Key | Allowed value |
| --- | --- |
| `SECTION` | `INICIO`, `CLIENTES`, `PEDIDOS`, `INVENTARIO` |
| `OPERATION` | `LOAD_APPOINTMENTS`, `LOAD_CUSTOMERS`, `CREATE_CUSTOMER`, `CREATE_APPOINTMENT`, `RELOAD_APPOINTMENTS`, `LOAD_ORDERS`, `CREATE_ORDER`, `DELETE_ORDER`, `DELETE_CUSTOMER` |
| `ERROR_CODE` | `NETWORK_ERROR`, `HTTP_ERROR`, `TIMEOUT`, `INVALID_RESPONSE`, `VALIDATION_ERROR`, `UNKNOWN_ERROR` |
| `HTTP_STATUS` | Integer `100`-`599` |
| `ERROR_NAME` | 1-40 alphanumeric characters starting with a letter |
| `FIELD` | `CLIENT`, `TIME`, `PERIOD`, `TYPE` |
| `REASON` | `REQUIRED`, `FORMAT`, `NOT_FOUND`, `INVALID_DATA`, `PERSISTENCE_ERROR`, `DUPLICATE`, `DATABASE_ERROR` |
| `AUTH_METHOD` | `PASSWORD`, `GOOGLE` |
| `CUSTOMER_ID` | Safe identifier, max 64 characters |
| `APPOINTMENT_STATUS` | `SCHEDULED`, `COMPLETED`, `CANCELLED` |
| `PRODUCT_COUNT` | Integer `0`-`10000` |
| `HAS_DELIVERY_DATE` | Boolean |

Unknown keys, nested objects/arrays, control characters, empty values and
out-of-range values are rejected. Metadata must never contain names, phone
numbers, email addresses, passwords, tokens, payment data, request bodies,
free-form form text, DOM text or database error messages.

---

## 3. Ingestion endpoint

`POST /api/audit/events`

```json
{
  "eventType": "UI_SECTION_VIEWED",
  "entityType": "APPLICATION",
  "entityId": "optional-safe-id",
  "metadata": {
    "SECTION": "INICIO"
  }
}
```

Successful response: `202 Accepted`, `Cache-Control: no-store`.

```json
{
  "eventId": "a2f4...",
  "occurredAt": "2026-10-27T02:28:30.1108326Z"
}
```

The client cannot submit `actorType`, `actorId`, `action`, `eventSource`,
`outcome`, `occurredAt`, `requestId` or `sessionId`. Unknown JSON fields cause a
`400`, and the server derives or captures those fields itself. Session
correlation is accepted only from the validated `X-Audit-Session-Id` header,
never from the JSON body.

| Condition | Response |
| --- | --- |
| Valid UI event | `202` |
| Invalid/unknown field, enum, metadata key or value | `400 invalid_audit_event` |
| Business event type submitted by a client | `400` (`event_type_not_allowed`) |
| `sessionId` or another unknown field in the body | `400 invalid_audit_event` |
| Body larger than 4096 bytes | `413 payload_too_large` |
| More than 60 requests/minute per remote socket address | `429 rate_limited` + `Retry-After` |
| Audit database unavailable | `503 audit_unavailable` |
| No read/list endpoint exists | `405` for `GET /api/audit/events` |

Limits are configured by `audit.ingestion.*` in
`Backend/src/main/resources/application.properties`.

`X-Request-Id` is always generated/overwritten by the server.
`X-Audit-Session-Id` is accepted only as optional correlation and is validated;
it is never treated as identity.

---

## 4. Transaction semantics

| Write | Propagation | Behaviour |
| --- | --- | --- |
| Frontend ingestion | `REQUIRES_NEW` | Audit failure never changes a UI request outcome; the endpoint returns an error to the fire-and-forget client. |
| Backend success (`*_CREATED`, `*_UPDATED`, `*_DELETED`) | `MANDATORY` | Joins the business transaction. Business rollback removes the success audit. If the audit insert fails, the business transaction fails: no success record means no reported success. |
| Backend failure/rejection | `REQUIRES_NEW` | Survives rollback of the failed business transaction. An audit write failure is logged by exception class and never replaces the original business exception. |

Business services use `saveAndFlush`/`flush` so persistence failures happen
before a success audit is written. No audit code reads or writes the audit
endpoint recursively.

---

## 5. Privacy and security limitations

Implemented:

- No trusted client actor id; all events are `ANONYMOUS`.
- No public audit read/list API and no update/delete API.
- Append-only database triggers and RLS with no anon/authenticated access.
- Strict server-side event, entity, metadata and size validation.
- Server-authoritative timestamp, request id, source, outcome and action.
- Body limit and a best-effort per-instance rate limit.
- Frontend logging is fire-and-forget, time-bounded, deduplicated, locally
  rate-limited (40 events/minute per tab) and never surfaces an error or
  retries.
- No secrets, credentials, tokens, contact details or raw response bodies are
  written to audit metadata.

Known limitations:

- **No authentication yet.** The UI login is not a real identity check; actor is
  anonymous and `UI_*` events are client-reported.
- **UI events are not trustworthy evidence of business success.** Only backend
  events prove that an operation completed.
- **The ingestion endpoint is public.** A malicious client can emit allowed UI
  event types. Server-side validation limits the shape, not the truthfulness, of
  those observations.
- **The rate limiter is per instance and in memory.** It resets on restart and
  does not coordinate across replicas. Put a gateway/distributed limiter in
  front of the endpoint before scaling out.
- **The rate-limit key is the socket remote address.** If the application sits
  behind a reverse proxy without trusted forwarded-header configuration, all
  users may appear to share the proxy address. Configure client-address
  resolution at the edge rather than trusting arbitrary `X-Forwarded-For`
  values inside the application.
- **Append-only is enforced in the database, not cryptographically.** A database
  owner/maintainer can disable triggers or alter/delete rows. This is not a WORM
  store or a tamper-evident ledger.
- **Retention is manual.** No scheduled archival/purge job is included; define a
  retention policy before production growth becomes an issue.
- **PostgreSQL-specific schema.** Tests run on H2; the production script,
  `jsonb`, RLS and triggers must be validated in the target Supabase project.
- Audit tables grow without bound if no retention policy is applied.

---

## 6. Verification

Run from `Backend/`:

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
```

Current result: **56 tests, 0 failures, 0 errors, 0 skipped**. The package
command also completes successfully.

Run from `Frontend/`:

```powershell
npm test
npm run build
npm run lint
```

Current results: **6 tests passed**, production build succeeded, ESLint
reported no errors.

Test coverage includes:

- Valid/invalid client payloads, unknown fields (including a body `sessionId`),
  forbidden business event types, metadata bounds and nested/unsafe values.
- API ingestion (`202`, `400`, `413`, `405`) and server-derived fields.
- Audit persistence, client-supplied identity rejection, `MANDATORY` success
  semantics, business rollback, and `REQUIRES_NEW` failure survival.
- Request id/session correlation, size filter and rate-limit behavior.
- Customer create/update/delete, appointment and order
  success/failure/rejection instrumentation, original-exception preservation
  and audit-failure isolation.
- Frontend audit client metadata allow-listing (PII/credential exclusion),
  entity-id filtering, metadata-aware dedupe, local rate limiting, the session
  header, and non-recursive failure handling.

---

## 7. Remaining manual steps

1. Execute `Backend/database/audit_log.sql` in Supabase.
2. Start the backend against Supabase and confirm Hibernate `validate` passes.
3. Confirm the application JDBC role can insert into `audit_log` and that
   `anon`/`authenticated` cannot select from it.
4. Smoke-test `POST /api/audit/events` from the deployed frontend origin,
   including CORS preflight.
5. Decide on authentication, retention/archival, and a shared rate limiter
   before treating the audit trail as production compliance evidence.
