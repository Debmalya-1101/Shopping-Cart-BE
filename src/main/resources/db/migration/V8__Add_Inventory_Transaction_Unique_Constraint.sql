ALTER TABLE inventory_transactions 
ADD CONSTRAINT uk_inv_tx_ref UNIQUE (reference_type, reference_id, transaction_type);
