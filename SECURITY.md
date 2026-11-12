# SECURITY.md

Security posture of the Ilussiones Boutique backend (Spring Boot 4.1.1, Java 25).
Scope: the business REST API (`/api/customer`, `/api/appointment`, `/api/order`)
and the audit ingestion API (`/api/audit/events`).

## 1. Authentication & authorization — known limitation

`SecurityConfig` currently configures:

```java
.csrf(csrf -> csrf.disable())
.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
```

**Every endpoint is public.** Requests without any credentials are accepted, and
CSRF protection is disabled. This is a deliberate development posture: the React
frontend holds no session token, so adding authentication to the API alone would
break the app. It is documented here as a **known, unaddressed limitation**, and
`BusinessApiSecurityTest.requestsAreAllowedWithoutAuthenticationDocumentingCurrentConfig`
pins the behaviour so any future change is intentional. Recommended next step:
issue Supabase JWTs to the frontend and replace `permitAll()` with an
authenticated matcher, then re-enable CSRF for state-changing routes.

## 2. Input validation

Validation is defence-in-depth, applied before persistence:

* **Bean Validation** on entities (`@NotBlank`, `@NotNull`, `@Size`, bounds on
  `Product.price`) rejects malformed create payloads with `400`.
* **Service-level rules** (`BusinessValidationException`) re-validate the merged
  entity so PUT requests that carry only the changed fields are still checked as
  a whole, and reference integrity (e.g. `customer_not_found`) is enforced
  independently of the database.
* Errors are returned as short machine codes (`validation_failed`,
  `invalid_request`, `malformed_parameter`, `malformed_payload`) — never raw
  exception text, SQL, or the rejected payload.

## 3. Injection & data integrity

* All persistence goes through Spring Data JPA / Hibernate, which uses bind
  parameters; no string-concatenated SQL is used by the application.
* SQL-injection payloads (`'; DROP TABLE customer;--`) submitted in JSON bodies
  are stored as **inert data** and round-trip unchanged (verified live and in
  `BusinessApiSecurityTest`). SQL-injection strings in UUID/path variables are
  rejected with `400`.
* Database-level integrity (foreign keys, check constraints, `ON DELETE`
  behaviour) remains enabled and is never bypassed; `ddl-auto=validate` means
  Hibernate cannot alter the schema.

## 4. Auditing

Business operations and validation rejections are written to the append-only
`audit_log` table by `AuditService`. The table is protected by database triggers
(`audit_log_no_update`, `audit_log_no_delete`) so history cannot be rewritten
through the application. Audit metadata is restricted to a bounded set of keys
per event type, and only whitelisted, client-ingestible UI event types are
accepted from the frontend.

## 5. Secrets & sensitive data

* Database credentials live in `Backend/.env`, which is **git-ignored** (`.env`,
  `.env.*.local`). No secret, connection string, or password is committed.
* Database backups (`*.backup`, `*.dump`, `docs/database/*.backup`) are
  git-ignored because they can contain real customer data.
* Error responses are asserted not to contain `Exception`, Spring/Hibernate
  package names, or SQL fragments (`errorResponsesDoNotLeakInternals`).

## 6. Database access

Row-Level Security is enabled on all 16 `public` tables, but the dump contains
**no `CREATE POLICY` statements**. The backend connects with a privileged role
that bypasses RLS, which is why the API works without policies. Any future direct
Supabase client access (anon/authenticated) will require explicit policies.
Staff password hashes use a **non-standard encoding** (not bcrypt/argon2), so
password verification is treated as unverified — see `docs/database/README.md`.

## 7. Verification

```powershell
cd Backend
.\mvnw.cmd test    # 109 tests, 0 failures (includes BusinessApiSecurityTest)
```

Security tests are HTTP-level (MockMvc) and were additionally confirmed against
a running instance via `curl` (malformed input, injection, unknown references,
oversized fields, and the partial-PUT contract).

## 8. Summary

| Area | Status |
| --- | --- |
| Input validation & safe error handling | Implemented |
| Injection resistance | Implemented (JPA bind parameters) |
| Append-only audit trail | Implemented (DB triggers) |
| Secrets / dumps not committed | Implemented (git-ignored) |
| Authentication & authorization | **Not implemented** (documented limitation) |
| RLS policies | Enabled but **no policies defined** |
| Password hashing | Non-standard; **unverified** |
