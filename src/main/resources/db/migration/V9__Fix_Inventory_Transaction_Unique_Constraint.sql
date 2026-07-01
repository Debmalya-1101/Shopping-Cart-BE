ALTER TABLE inventory_transactions DROP INDEX uk_inv_tx_ref;
ALTER TABLE inventory_transactions ADD CONSTRAINT uk_inv_tx_ref UNIQUE (inventory_id, reference_type, reference_id, transaction_type);
