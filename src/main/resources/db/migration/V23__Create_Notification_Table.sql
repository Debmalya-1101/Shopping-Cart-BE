CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    reference_id VARCHAR(255),
    idempotency_key VARCHAR(255) UNIQUE,
    notification_type VARCHAR(100) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    recipient_to VARCHAR(1000) NOT NULL,
    recipient_cc VARCHAR(1000),
    recipient_bcc VARCHAR(1000),
    subject VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    retry_count INT DEFAULT 0,
    failure_reason TEXT,
    metadata TEXT,
    version BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP NULL
);

CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
