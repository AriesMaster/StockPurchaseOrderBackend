-- Retain existing SKU foreign keys and historical product references.
ALTER TABLE product ADD COLUMN unit_price numeric(12,2) NOT NULL DEFAULT 0 CHECK (unit_price >= 0);
ALTER TABLE product ADD COLUMN active boolean NOT NULL DEFAULT true;
ALTER TABLE product ADD COLUMN version bigint NOT NULL DEFAULT 0;
