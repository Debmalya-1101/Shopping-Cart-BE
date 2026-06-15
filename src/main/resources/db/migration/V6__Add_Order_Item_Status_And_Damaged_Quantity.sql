ALTER TABLE order_items
ADD COLUMN status VARCHAR(50);

ALTER TABLE inventory
ADD COLUMN damaged_quantity INT NOT NULL DEFAULT 0;
