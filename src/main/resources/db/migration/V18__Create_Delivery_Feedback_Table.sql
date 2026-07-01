CREATE TABLE delivery_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    shipment_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    delivery_partner_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_delivery_feedback_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_delivery_feedback_shipment FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    CONSTRAINT fk_delivery_feedback_customer FOREIGN KEY (customer_id) REFERENCES users(id),
    CONSTRAINT fk_delivery_feedback_partner FOREIGN KEY (delivery_partner_id) REFERENCES delivery_partners(id),
    UNIQUE (order_id)
);
