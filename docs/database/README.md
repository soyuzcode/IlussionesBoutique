# Database model

This folder documents the **`public`** schema of the Ilussiones Boutique
Supabase (PostgreSQL) database.

| File | Purpose |
| --- | --- |
| `schema.sql` | Consolidated `public` DDL (types, tables, keys, indexes, RLS, triggers). Documentation only - not a migration. |
| `erd.mmd` | Mermaid ER diagram. |
| `db_cluster-*.backup` | The raw plain-SQL backup the schema was read from. **Git-ignored; never commit or share it** (it can contain customer data). |

The live database is the source of truth. Hibernate runs with
`spring.jpa.hibernate.ddl-auto=validate` and never changes the schema.

## How this was produced

The backup is a **plain SQL** dump (`pg_dump` default format), not the custom
`-Fc` format, so it was read directly as text. `public`-schema objects were
extracted by hand into `schema.sql` and `erd.mmd`. `pg_restore`/`psql` are not
installed in this environment, so the schema was **not** executed against a
PostgreSQL instance - it is a faithful transcription, not a verified restore.

## Entity overview

The model has three layers:

1. **Catalogue** - `product` plus one subtype table per product kind
   (`album`, `card`, `cup`, `dress`, `plush`). Each subtype shares the
   `product` primary key (JOINED inheritance).
2. **Ordering** - a normalised aggregate: `"order"` (header), `order_item`
   (generic products), `order_dress` (custom dress orders), and `payment`.
3. **People & scheduling** - `customer`, `"user"` (staff), `appointment`.
4. **Cross-cutting** - `audit_log`, an append-only audit trail protected by
   triggers.

### Tables

| Table | Key | Purpose | Mapped by JPA? |
| --- | --- | --- | --- |
| `product` | `id` uuid | Base catalogue row (`type`, `name`, `price`, `status`, `stock`). | Partly (`Product` entity; see gaps below) |
| `album`, `card`, `cup`, `dress`, `plush` | `product_id` uuid FK | Per-type product attributes. | No (classes exist but are not `@Entity`) |
| `customer` | `id` uuid | Boutique customer (`name`, `phone`, `status`). | Yes (`Customer`) |
| `"user"` | `id` uuid | Staff account (`email`, `password_hash`, `role`, `active`). | No |
| `appointment` | `id` uuid | Fitting/consultation booking. | Yes (`Appointment`) |
| `"order"` | `id` uuid | Normalised order header. | No |
| `order_item` | `(order_id, product_id)` | Order lines for non-dress products. | No |
| `order_dress` | `order_id` | Custom dress order details. | No |
| `payment` | `id` uuid | Payments against an order. | No |
| `orders` | `order_id` text | Legacy order table used by the `Order` entity. | Yes (`Order`) |
| `order_products` | `(order_id, product_id)` | Join table for `orders` <-> `product`. | Yes (`Order.products`) |
| `audit_log` | `id` uuid | Append-only audit trail. | Yes (`AuditLog`) |

Enums: `appointment_status`, `order_status`, `payment_status`,
`product_status`, `product_type`, `user_role`.

### Important relationships

* `appointment.customer_id -> customer.id` **ON DELETE CASCADE**.
* `"order".customer_id -> customer.id` and `"order".user_id -> "user".id`
  **ON DELETE RESTRICT**.
* `order_item.order_id -> "order".id` **CASCADE**;
  `order_item.product_id -> product.id` **RESTRICT**.
* `order_dress.base_product_id -> dress.product_id`.
* `payment.order_id -> "order".id` **CASCADE**;
  `payment.user_id -> "user".id` **SET NULL**.
* `orders`/`order_products` are linked by text `order_id` and cascade.
* Subtype tables cascade from `product`.

## Integrity rules enforced in the database

* `audit_log` is **append-only**: `audit_log_no_update` and
  `audit_log_no_delete` triggers raise on any `UPDATE`/`DELETE`.
* `audit_log` check constraints bound every text column (format regexes,
  enumerated `actor_type`/`entity_type`/`event_source`/`outcome`, JSON object
  and 4 KB metadata cap).
* `order_item` must not reference a `DRESS` product; the
  `trg_prevent_dress_in_order_item` trigger enforces this at the database level.
* Non-negative / positive checks on `price`, `stock`, `total_amount`,
  `agreed_price`, `unit_price`, `quantity`, `payment.amount`.
* `customer.status` is constrained to `ACTIVO` / `FINALIZADO`.

## Row level security

RLS is **enabled on all 16 `public` tables**, but the backup contains **no
`CREATE POLICY` statements**. With RLS enabled and no policies, non-owner roles
(including `anon` and `authenticated`) see no rows through PostgREST. The Spring
backend connects with a privileged database role that bypasses RLS, which is why
the API can read and write without policies. Any future direct Supabase client
access will need explicit policies.

## Java model vs. database (known gaps)

These are real mismatches between the JPA entities and the verified schema. They
are documented rather than "fixed" here to avoid changing behaviour.

| Gap | Detail |
| --- | --- |
| Subtypes not entities | `Product` is annotated `@Inheritance(JOINED)` but `Album`/`Card`/`Cup`/`Dress`/`Plush` are **not** `@Entity`, so the subtype tables are unreachable through JPA. |
| `Product` columns | The entity maps only `id`, `price`, `photo_url`, `materials`, `status` (as `String`). `type`, `name`, `stock`, `created_at`, `updated_at` are **not** mapped. |
| `Customer.status` | Present in the database (default `ACTIVO`) but absent from the `Customer` entity. |
| Two order models | The `Order` entity maps the legacy `orders`/`order_products` tables, while the normalised `"order"`, `order_item`, `order_dress`, `payment` tables are unmapped. |
| `User` not an entity | The `"user"` table has no JPA mapping (`User` is a plain abstract class). |
| `password_hash` format | The single stored value is **not** a bcrypt/argon2 modular hash (`$2...`/`$argon2...`). It looks like a custom or truncated encoding. Treat staff password verification as unverified. |

## Regenerating

`pg_restore`/`psql` are not available here. With a PostgreSQL client installed,
a schema-only dump could be produced with:

```bash
pg_dump --schema-only --schema=public "$SUPABASE_DB_URL" > schema.sql
```

Do not commit any dump that contains data.
