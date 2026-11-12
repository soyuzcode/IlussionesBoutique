-- ============================================================================
-- Ilussiones Boutique - public schema (PostgreSQL / Supabase)
--
-- Derived by reading the plain-SQL backup at
--   docs/database/db_cluster-03-11-2026@16-11-56.backup
-- and keeping only the `public` schema objects owned by the application.
-- Supabase-managed schemas (auth, storage, realtime, extensions, ...) are
-- intentionally omitted.
--
-- This file is documentation, not a migration. The live database is the source
-- of truth and Hibernate runs with ddl-auto=validate against it.
-- No table data is included.
-- ============================================================================

-- gen_random_uuid() is provided by the built-in pgcrypto function on Supabase.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ----------------------------------------------------------------------------
-- Enum types
-- ----------------------------------------------------------------------------

CREATE TYPE public.appointment_status AS ENUM ('SCHEDULED', 'COMPLETED', 'CANCELLED');
CREATE TYPE public.order_status       AS ENUM ('PENDING', 'IN_PROGRESS', 'READY', 'DELIVERED', 'CANCELLED');
CREATE TYPE public.payment_status     AS ENUM ('UNPAID', 'PARTIAL', 'PAID');
CREATE TYPE public.product_status     AS ENUM ('AVAILABLE', 'OUT_OF_STOCK', 'DISCONTINUED');
CREATE TYPE public.product_type       AS ENUM ('DRESS', 'CARD', 'ALBUM', 'CUP', 'PLUSH');
CREATE TYPE public.user_role          AS ENUM ('ADMIN', 'EMPLOYEE');

-- ----------------------------------------------------------------------------
-- Tables
-- ----------------------------------------------------------------------------

CREATE TABLE public.product (
    id          uuid DEFAULT gen_random_uuid() NOT NULL,
    type        public.product_type NOT NULL,
    name        character varying(120) NOT NULL,
    price       numeric(10,2) NOT NULL,
    photo_url   text,
    materials   text,
    status      public.product_status DEFAULT 'AVAILABLE'::public.product_status NOT NULL,
    stock       integer DEFAULT 0 NOT NULL,
    created_at  timestamp with time zone DEFAULT now() NOT NULL,
    updated_at  timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT product_price_check CHECK (price >= 0),
    CONSTRAINT product_stock_check CHECK (stock >= 0)
);

-- Product subtypes share the product primary key (JOINED inheritance).
CREATE TABLE public.album (
    product_id      uuid NOT NULL,
    dimensions      character varying(50),
    cover_material  character varying(100),
    size            character varying(50),
    design          character varying(100)
);

CREATE TABLE public.card (
    product_id  uuid NOT NULL,
    quantity    integer DEFAULT 1 NOT NULL,
    design      character varying(100)
);

CREATE TABLE public.cup (
    product_id  uuid NOT NULL,
    quantity    integer DEFAULT 1 NOT NULL,
    capacity    numeric(8,2),
    design      character varying(100)
);

CREATE TABLE public.dress (
    product_id       uuid NOT NULL,
    measurements     character varying(100),
    color            character varying(50),
    cut_type         character varying(50),
    requires_changes boolean DEFAULT false
);

CREATE TABLE public.plush (
    product_id  uuid NOT NULL,
    size        character varying(50),
    model       character varying(100)
);

CREATE TABLE public.customer (
    id          uuid DEFAULT gen_random_uuid() NOT NULL,
    name        character varying(120) NOT NULL,
    phone       character varying(20) NOT NULL,
    created_at  timestamp with time zone DEFAULT now() NOT NULL,
    updated_at  timestamp with time zone DEFAULT now() NOT NULL,
    status      text DEFAULT 'ACTIVO' NOT NULL,
    CONSTRAINT customer_status_check CHECK (status = ANY (ARRAY['ACTIVO', 'FINALIZADO']))
);
COMMENT ON COLUMN public.customer.status IS 'Customer lifecycle status: ACTIVO or FINALIZADO';

CREATE TABLE public."user" (
    id            uuid DEFAULT gen_random_uuid() NOT NULL,
    name          character varying(120) NOT NULL,
    email         character varying(150),
    password_hash text NOT NULL,
    role          public.user_role DEFAULT 'EMPLOYEE'::public.user_role NOT NULL,
    active        boolean DEFAULT true NOT NULL,
    created_at    timestamp with time zone DEFAULT now() NOT NULL,
    updated_at    timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.appointment (
    id               uuid DEFAULT gen_random_uuid() NOT NULL,
    customer_id      uuid NOT NULL,
    appointment_date date NOT NULL,
    appointment_time time without time zone NOT NULL,
    status           public.appointment_status DEFAULT 'SCHEDULED'::public.appointment_status NOT NULL,
    google_event_id  character varying(255),
    created_at       timestamp with time zone DEFAULT now() NOT NULL,
    updated_at       timestamp with time zone DEFAULT now() NOT NULL,
    reason           text
);

-- Normalised order aggregate.
CREATE TABLE public."order" (
    id             uuid DEFAULT gen_random_uuid() NOT NULL,
    customer_id    uuid NOT NULL,
    user_id        uuid NOT NULL,
    event_type     character varying(60) NOT NULL,
    delivery_date  date NOT NULL,
    status         public.order_status DEFAULT 'PENDING'::public.order_status NOT NULL,
    payment_status public.payment_status DEFAULT 'UNPAID'::public.payment_status NOT NULL,
    total_amount   numeric(10,2) DEFAULT 0 NOT NULL,
    created_at     timestamp with time zone DEFAULT now() NOT NULL,
    updated_at     timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT order_total_amount_check CHECK (total_amount >= 0)
);

CREATE TABLE public.order_item (
    order_id    uuid NOT NULL,
    product_id  uuid NOT NULL,
    quantity    integer DEFAULT 1 NOT NULL,
    unit_price  numeric(10,2) NOT NULL,
    CONSTRAINT order_item_quantity_check CHECK (quantity > 0),
    CONSTRAINT order_item_unit_price_check CHECK (unit_price >= 0)
);

CREATE TABLE public.order_dress (
    order_id            uuid NOT NULL,
    base_product_id     uuid NOT NULL,
    measurements        text,
    color               text,
    cut_type            text,
    customization_notes text,
    agreed_price        numeric(12,2) NOT NULL,
    created_at          timestamp with time zone DEFAULT now() NOT NULL,
    updated_at          timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT order_dress_agreed_price_check CHECK (agreed_price >= 0)
);

CREATE TABLE public.payment (
    id       uuid DEFAULT gen_random_uuid() NOT NULL,
    order_id uuid NOT NULL,
    user_id  uuid,
    amount   numeric(10,2) NOT NULL,
    method   character varying(30) DEFAULT 'CASH'::character varying NOT NULL,
    paid_at  timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT payment_amount_check CHECK (amount > 0)
);

-- Legacy / JPA-facing order table (mapped by the Order entity).
CREATE TABLE public.orders (
    order_id      text NOT NULL,
    event_type    text,
    delivery_date timestamp without time zone
);

CREATE TABLE public.order_products (
    order_id   text NOT NULL,
    product_id uuid NOT NULL
);

-- ----------------------------------------------------------------------------
-- Audit trail (append-only, enforced by trigger)
-- ----------------------------------------------------------------------------

CREATE TABLE public.audit_log (
    id           uuid DEFAULT gen_random_uuid() NOT NULL,
    occurred_at  timestamp with time zone DEFAULT now() NOT NULL,
    event_type   character varying(64) NOT NULL,
    action       character varying(32) NOT NULL,
    actor_type   character varying(16) NOT NULL,
    actor_id     character varying(128),
    entity_type  character varying(32) NOT NULL,
    entity_id    character varying(128),
    event_source character varying(16) NOT NULL,
    outcome      character varying(16) NOT NULL,
    session_id   character varying(64),
    request_id   character varying(64) NOT NULL,
    metadata     jsonb DEFAULT '{}'::jsonb NOT NULL,
    CONSTRAINT audit_log_action_format CHECK (action ~ '^[A-Z][A-Z0-9_]{2,31}$'),
    CONSTRAINT audit_log_actor_id_format CHECK (actor_id IS NULL OR (length(actor_id) BETWEEN 1 AND 128 AND actor_id !~ '[[:cntrl:]]')),
    CONSTRAINT audit_log_actor_type_check CHECK (actor_type = ANY (ARRAY['ANONYMOUS', 'USER', 'SYSTEM'])),
    CONSTRAINT audit_log_entity_id_format CHECK (entity_id IS NULL OR (length(entity_id) BETWEEN 1 AND 128 AND entity_id !~ '[[:cntrl:]]')),
    CONSTRAINT audit_log_entity_type_check CHECK (entity_type = ANY (ARRAY['APPLICATION', 'AUTH', 'CUSTOMER', 'APPOINTMENT', 'ORDER', 'PRODUCT'])),
    CONSTRAINT audit_log_event_type_format CHECK (event_type ~ '^[A-Z][A-Z0-9_]{2,63}$'),
    CONSTRAINT audit_log_metadata_object CHECK (jsonb_typeof(metadata) = 'object'),
    CONSTRAINT audit_log_metadata_size CHECK (octet_length(metadata::text) <= 4096),
    CONSTRAINT audit_log_outcome_check CHECK (outcome = ANY (ARRAY['SUCCESS', 'FAILURE', 'REJECTED', 'INFO'])),
    CONSTRAINT audit_log_request_id_format CHECK (request_id ~ '^[A-Za-z0-9_-]{8,64}$'),
    CONSTRAINT audit_log_session_id_format CHECK (session_id IS NULL OR session_id ~ '^[A-Za-z0-9_-]{8,64}$'),
    CONSTRAINT audit_log_source_check CHECK (event_source = ANY (ARRAY['FRONTEND', 'BACKEND', 'SYSTEM']))
);

COMMENT ON TABLE public.audit_log IS 'Append-oriented audit trail. Written by the application; no update/delete API exists.';
COMMENT ON COLUMN public.audit_log.metadata IS 'Bounded, non-sensitive metadata only. Never contains credentials, tokens, payment data or free-form form contents.';

-- ----------------------------------------------------------------------------
-- Functions
-- ----------------------------------------------------------------------------

CREATE FUNCTION public.audit_log_prevent_mutation() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only; update/delete is not allowed';
END;
$$;

CREATE FUNCTION public.prevent_dress_in_order_item() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    selected_type public.product_type;
BEGIN
    SELECT type INTO selected_type FROM public.product WHERE id = NEW.product_id;
    IF selected_type IS NULL THEN
        RAISE EXCEPTION 'El producto % no existe', NEW.product_id;
    END IF;
    IF selected_type = 'DRESS' THEN
        RAISE EXCEPTION 'Los vestidos deben registrarse en order_dress, no en order_item';
    END IF;
    RETURN NEW;
END;
$$;

CREATE FUNCTION public.set_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path TO 'public'
    AS $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- ----------------------------------------------------------------------------
-- Primary keys and unique constraints
-- ----------------------------------------------------------------------------

ALTER TABLE ONLY public.album         ADD CONSTRAINT album_pkey PRIMARY KEY (product_id);
ALTER TABLE ONLY public.appointment   ADD CONSTRAINT appointment_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.audit_log     ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.card          ADD CONSTRAINT card_pkey PRIMARY KEY (product_id);
ALTER TABLE ONLY public.cup           ADD CONSTRAINT cup_pkey PRIMARY KEY (product_id);
ALTER TABLE ONLY public.customer      ADD CONSTRAINT customer_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.dress         ADD CONSTRAINT dress_pkey PRIMARY KEY (product_id);
ALTER TABLE ONLY public.order_dress   ADD CONSTRAINT order_dress_pkey PRIMARY KEY (order_id);
ALTER TABLE ONLY public.order_item    ADD CONSTRAINT order_item_pkey PRIMARY KEY (order_id, product_id);
ALTER TABLE ONLY public."order"       ADD CONSTRAINT order_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.order_products ADD CONSTRAINT order_products_pkey PRIMARY KEY (order_id, product_id);
ALTER TABLE ONLY public.orders        ADD CONSTRAINT orders_pkey PRIMARY KEY (order_id);
ALTER TABLE ONLY public.payment       ADD CONSTRAINT payment_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.plush         ADD CONSTRAINT plush_pkey PRIMARY KEY (product_id);
ALTER TABLE ONLY public.product       ADD CONSTRAINT product_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public."user"        ADD CONSTRAINT user_email_key UNIQUE (email);
ALTER TABLE ONLY public."user"        ADD CONSTRAINT user_pkey PRIMARY KEY (id);

-- ----------------------------------------------------------------------------
-- Foreign keys
-- ----------------------------------------------------------------------------

ALTER TABLE ONLY public.album          ADD CONSTRAINT fk_album_product  FOREIGN KEY (product_id) REFERENCES public.product(id) ON DELETE CASCADE;
ALTER TABLE ONLY public.card           ADD CONSTRAINT fk_card_product   FOREIGN KEY (product_id) REFERENCES public.product(id) ON DELETE CASCADE;
ALTER TABLE ONLY public.cup            ADD CONSTRAINT fk_cup_product    FOREIGN KEY (product_id) REFERENCES public.product(id) ON DELETE CASCADE;
ALTER TABLE ONLY public.dress          ADD CONSTRAINT fk_dress_product  FOREIGN KEY (product_id) REFERENCES public.product(id) ON DELETE CASCADE;
ALTER TABLE ONLY public.plush          ADD CONSTRAINT fk_plush_product  FOREIGN KEY (product_id) REFERENCES public.product(id) ON DELETE CASCADE;
ALTER TABLE ONLY public.appointment    ADD CONSTRAINT fk_appointment_customer FOREIGN KEY (customer_id) REFERENCES public.customer(id) ON DELETE CASCADE;
ALTER TABLE ONLY public."order"        ADD CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES public.customer(id) ON DELETE RESTRICT;
ALTER TABLE ONLY public."order"        ADD CONSTRAINT fk_order_user     FOREIGN KEY (user_id) REFERENCES public."user"(id) ON DELETE RESTRICT;
ALTER TABLE ONLY public.order_item     ADD CONSTRAINT fk_item_order     FOREIGN KEY (order_id) REFERENCES public."order"(id) ON DELETE CASCADE;
ALTER TABLE ONLY public.order_item     ADD CONSTRAINT fk_item_product   FOREIGN KEY (product_id) REFERENCES public.product(id) ON DELETE RESTRICT;
ALTER TABLE ONLY public.order_dress    ADD CONSTRAINT order_dress_order_id_fkey FOREIGN KEY (order_id) REFERENCES public."order"(id) ON DELETE CASCADE;
ALTER TABLE ONLY public.order_dress    ADD CONSTRAINT order_dress_base_product_id_fkey FOREIGN KEY (base_product_id) REFERENCES public.dress(product_id);
ALTER TABLE ONLY public.payment        ADD CONSTRAINT fk_payment_order  FOREIGN KEY (order_id) REFERENCES public."order"(id) ON DELETE CASCADE;
ALTER TABLE ONLY public.payment        ADD CONSTRAINT fk_payment_user   FOREIGN KEY (user_id) REFERENCES public."user"(id) ON DELETE SET NULL;
ALTER TABLE ONLY public.order_products ADD CONSTRAINT fk_order_products_order   FOREIGN KEY (order_id) REFERENCES public.orders(order_id) ON DELETE CASCADE;
ALTER TABLE ONLY public.order_products ADD CONSTRAINT fk_order_products_product FOREIGN KEY (product_id) REFERENCES public.product(id) ON DELETE CASCADE;

-- ----------------------------------------------------------------------------
-- Indexes
-- ----------------------------------------------------------------------------

CREATE INDEX audit_log_actor_idx                  ON public.audit_log USING btree (actor_type, actor_id, occurred_at DESC) WHERE actor_id IS NOT NULL;
CREATE INDEX audit_log_entity_idx                 ON public.audit_log USING btree (entity_type, entity_id, occurred_at DESC) WHERE entity_id IS NOT NULL;
CREATE INDEX audit_log_event_type_occurred_at_idx ON public.audit_log USING btree (event_type, occurred_at DESC);
CREATE INDEX audit_log_metadata_gin_idx           ON public.audit_log USING gin (metadata jsonb_path_ops);
CREATE INDEX audit_log_occurred_at_idx            ON public.audit_log USING btree (occurred_at DESC);
CREATE INDEX audit_log_request_idx                ON public.audit_log USING btree (request_id);
CREATE INDEX audit_log_session_idx                ON public.audit_log USING btree (session_id, occurred_at DESC) WHERE session_id IS NOT NULL;
CREATE INDEX audit_log_source_occurred_at_idx     ON public.audit_log USING btree (event_source, occurred_at DESC);

-- ----------------------------------------------------------------------------
-- Row level security
-- ----------------------------------------------------------------------------
-- RLS is enabled on every application table. The backup contains no CREATE
-- POLICY statements, so with RLS enabled and no policies, non-owner roles see
-- no rows. The Spring backend connects with a privileged role that bypasses
-- RLS, which is why the API works without policies today.

ALTER TABLE public.album          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.appointment    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_log      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.card           ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cup            ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.customer       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.dress          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public."order"        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.order_dress    ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.order_item     ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.order_products ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orders         ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.plush          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.product        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public."user"         ENABLE ROW LEVEL SECURITY;

-- ----------------------------------------------------------------------------
-- Triggers
-- ----------------------------------------------------------------------------

CREATE TRIGGER audit_log_no_delete BEFORE DELETE ON public.audit_log
    FOR EACH ROW EXECUTE FUNCTION public.audit_log_prevent_mutation();

CREATE TRIGGER audit_log_no_update BEFORE UPDATE ON public.audit_log
    FOR EACH ROW EXECUTE FUNCTION public.audit_log_prevent_mutation();

CREATE TRIGGER trg_prevent_dress_in_order_item BEFORE INSERT OR UPDATE OF product_id ON public.order_item
    FOR EACH ROW EXECUTE FUNCTION public.prevent_dress_in_order_item();
