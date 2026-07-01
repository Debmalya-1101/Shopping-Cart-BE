CREATE TABLE inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    available_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_inventory_product_id UNIQUE (product_id),
    CONSTRAINT fk_inventory_product_id FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE inventory_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_id BIGINT NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    reference_type VARCHAR(50),
    reference_id VARCHAR(100),
    quantity INT NOT NULL,
    notes VARCHAR(255),
    created_at TIMESTAMP,
    CONSTRAINT fk_inventory_tx_inventory_id FOREIGN KEY (inventory_id) REFERENCES inventory(id)
);
