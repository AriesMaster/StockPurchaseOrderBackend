-- No purchase order write endpoint existed before this migration.
-- If manually inserted orders exist, fail rather than invent supplier or price history.
ALTER TABLE purchase_order ADD COLUMN supplier_id bigint NOT NULL REFERENCES supplier(id);
ALTER TABLE purchase_order_line ADD COLUMN unit_price numeric(12,2) NOT NULL CHECK (unit_price >= 0);
ALTER TABLE purchase_order_line ADD COLUMN product_name text NOT NULL CHECK (btrim(product_name) <> '');
CREATE INDEX ix_purchase_order_supplier ON purchase_order(supplier_id);
