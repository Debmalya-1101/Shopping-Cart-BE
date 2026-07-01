CREATE INDEX idx_inventory_quantities ON inventory (available_quantity, reorder_level);
CREATE INDEX idx_inv_tx_type_date ON inventory_transactions (transaction_type, created_at);
CREATE INDEX idx_inv_tx_inv_id ON inventory_transactions (inventory_id);
