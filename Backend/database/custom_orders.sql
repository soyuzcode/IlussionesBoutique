-- Ilussiones Boutique - custom (made-to-measure) dress orders with deposit.
-- Execute manually in the Supabase SQL Editor BEFORE deploying the backend
-- (Hibernate runs with ddl-auto=validate). The script is idempotent and only
-- adds columns, so the currently deployed backend keeps working.

ALTER TABLE public.orders
    ADD COLUMN IF NOT EXISTS custom_order boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS customization varchar(120),
    ADD COLUMN IF NOT EXISTS customization_description varchar(1000),
    ADD COLUMN IF NOT EXISTS custom_price numeric(10,2),
    -- Advance payment (abono); the pending balance is computed by the backend.
    ADD COLUMN IF NOT EXISTS deposit numeric(10,2),
    -- Body measurements in centimetres, stored as entered.
    ADD COLUMN IF NOT EXISTS measure_shoulder varchar(20),
    ADD COLUMN IF NOT EXISTS measure_bust varchar(20),
    ADD COLUMN IF NOT EXISTS measure_hip varchar(20),
    ADD COLUMN IF NOT EXISTS measure_waist varchar(20),
    ADD COLUMN IF NOT EXISTS measure_skirt_length varchar(20),
    ADD COLUMN IF NOT EXISTS measure_blouse_length varchar(20),
    ADD COLUMN IF NOT EXISTS measure_neckline varchar(20);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'orders_custom_amounts_check') THEN
        ALTER TABLE public.orders ADD CONSTRAINT orders_custom_amounts_check
            CHECK ((custom_price IS NULL OR custom_price >= 0) AND (deposit IS NULL OR deposit >= 0));
    END IF;
END $$;
