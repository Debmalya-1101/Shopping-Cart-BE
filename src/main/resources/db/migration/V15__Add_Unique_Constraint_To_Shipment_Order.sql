ALTER TABLE shipments ADD CONSTRAINT uk_shipment_order UNIQUE (order_id);
