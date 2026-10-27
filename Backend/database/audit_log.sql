-- Ilussioness Boutique - audit log schema
-- Execute manually in Supabase SQL Editor.
-- Do not execute from Spring Boot. Keep ddl-auto=validate.
-- Entity: com.keycode.ilussioness_backend.audit.domain.AuditLog

CREATE TABLE IF NOT EXISTS public.audit_log (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    occurred_at timestamptz NOT NULL DEFAULT now(),
    event_type varchar(64) NOT NULL,
    action varchar(32) NOT NULL,
    actor_type varchar(16) NOT NULL,
    actor_id varchar(128),
    entity_type varchar(32) NOT NULL,
    entity_id varchar(128),
    event_source varchar(16) NOT NULL,
    outcome varchar(16) NOT NULL,
    session_id varchar(64),
    request_id varchar(64) NOT NULL,
    metadata jsonb NOT NULL DEFAULT '{}'::jsonb,

    CONSTRAINT audit_log_event_type_format
        CHECK (event_type ~ '^[A-Z][A-Z0-9_]{2,63}$'),

    CONSTRAINT audit_log_action_format
        CHECK (action ~ '^[A-Z][A-Z0-9_]{2,31}$'),

    CONSTRAINT audit_log_actor_type_check
        CHECK (actor_type IN ('ANONYMOUS', 'USER', 'SYSTEM')),

    CONSTRAINT audit_log_actor_id_format
        CHECK (
            actor_id IS NULL
            OR (
                length(actor_id) BETWEEN 1 AND 128
                AND actor_id !~ '[[:cntrl:]]'
            )
        ),

    CONSTRAINT audit_log_entity_type_check
        CHECK (
            entity_type IN (
                'APPLICATION',
                'AUTH',
                'CUSTOMER',
                'APPOINTMENT',
                'ORDER',
                'PRODUCT'
            )
        ),

    CONSTRAINT audit_log_entity_id_format
        CHECK (
            entity_id IS NULL
            OR (
                length(entity_id) BETWEEN 1 AND 128
                AND entity_id !~ '[[:cntrl:]]'
            )
        ),

    CONSTRAINT audit_log_source_check
        CHECK (event_source IN ('FRONTEND', 'BACKEND', 'SYSTEM')),

    CONSTRAINT audit_log_outcome_check
        CHECK (outcome IN ('SUCCESS', 'FAILURE', 'REJECTED', 'INFO')),

    CONSTRAINT audit_log_session_id_format
        CHECK (
            session_id IS NULL
            OR session_id ~ '^[A-Za-z0-9_-]{8,64}$'
        ),

    CONSTRAINT audit_log_request_id_format
        CHECK (request_id ~ '^[A-Za-z0-9_-]{8,64}$'),

    CONSTRAINT audit_log_metadata_object
        CHECK (jsonb_typeof(metadata) = 'object'),

    CONSTRAINT audit_log_metadata_size
        CHECK (octet_length(metadata::text) <= 4096)
);

COMMENT ON TABLE public.audit_log IS
    'Append-oriented audit trail. Written by the application; no update/delete API exists.';

COMMENT ON COLUMN public.audit_log.occurred_at IS
    'Server-generated event time (UTC timestamptz).';

COMMENT ON COLUMN public.audit_log.event_type IS
    'Application-defined event type. The ingestion endpoint accepts only UI_* types.';

COMMENT ON COLUMN public.audit_log.actor_type IS
    'ANONYMOUS until a trusted authentication mechanism is integrated.';

COMMENT ON COLUMN public.audit_log.event_source IS
    'FRONTEND for UI interaction events, BACKEND for completed business operations.';

COMMENT ON COLUMN public.audit_log.outcome IS
    'INFO for frontend observations; SUCCESS/FAILURE/REJECTED only from the backend.';

COMMENT ON COLUMN public.audit_log.metadata IS
    'Bounded, non-sensitive metadata only. Never contains credentials, tokens, payment data or free-form form contents.';


-- Indexes

CREATE INDEX IF NOT EXISTS audit_log_occurred_at_idx
    ON public.audit_log (occurred_at DESC);

CREATE INDEX IF NOT EXISTS audit_log_event_type_occurred_at_idx
    ON public.audit_log (event_type, occurred_at DESC);

CREATE INDEX IF NOT EXISTS audit_log_source_occurred_at_idx
    ON public.audit_log (event_source, occurred_at DESC);

CREATE INDEX IF NOT EXISTS audit_log_entity_idx
    ON public.audit_log (entity_type, entity_id, occurred_at DESC)
    WHERE entity_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS audit_log_actor_idx
    ON public.audit_log (actor_type, actor_id, occurred_at DESC)
    WHERE actor_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS audit_log_session_idx
    ON public.audit_log (session_id, occurred_at DESC)
    WHERE session_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS audit_log_request_idx
    ON public.audit_log (request_id);

CREATE INDEX IF NOT EXISTS audit_log_metadata_gin_idx
    ON public.audit_log USING gin (metadata jsonb_path_ops);


-- Access control

ALTER TABLE public.audit_log ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_roles
        WHERE rolname = 'anon'
    ) THEN
        REVOKE ALL ON TABLE public.audit_log FROM anon;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_roles
        WHERE rolname = 'authenticated'
    ) THEN
        REVOKE ALL ON TABLE public.audit_log FROM authenticated;
    END IF;
END
$$;

REVOKE ALL ON TABLE public.audit_log FROM PUBLIC;


-- Optional: configure a dedicated application role if one is used.
-- Replace your_application_role with the actual database role.
-- GRANT INSERT ON TABLE public.audit_log TO your_application_role;
-- CREATE POLICY audit_log_app_insert
--     ON public.audit_log
--     FOR INSERT
--     TO your_application_role
--     WITH CHECK (true);


-- Append-only protection

CREATE OR REPLACE FUNCTION public.audit_log_prevent_mutation()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'audit_log is append-only; update/delete is not allowed';
END;
$$;

DROP TRIGGER IF EXISTS audit_log_no_update
    ON public.audit_log;

CREATE TRIGGER audit_log_no_update
    BEFORE UPDATE ON public.audit_log
    FOR EACH ROW
    EXECUTE FUNCTION public.audit_log_prevent_mutation();

DROP TRIGGER IF EXISTS audit_log_no_delete
    ON public.audit_log;

CREATE TRIGGER audit_log_no_delete
    BEFORE DELETE ON public.audit_log
    FOR EACH ROW
    EXECUTE FUNCTION public.audit_log_prevent_mutation();


-- Refresh query planner statistics

ANALYZE public.audit_log;