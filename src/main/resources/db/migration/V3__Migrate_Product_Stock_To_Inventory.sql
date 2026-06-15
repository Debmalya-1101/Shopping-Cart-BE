INSERT INTO inventory (product_id, available_quantity, reserved_quantity, version, created_at, updated_at)
SELECT id, COALESCE(stock, 0), 0, 0, NOW(), NOW() FROM products;
