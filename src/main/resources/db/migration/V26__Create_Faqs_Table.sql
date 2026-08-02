CREATE TABLE faqs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO faqs (question, answer, active) VALUES
('What is Nexis Store?', 'Nexis Store is an enterprise-grade E-Commerce platform offering a wide variety of products with secure payments and reliable delivery.', true),
('How do I track my order?', 'You can track your order by going to the Orders section in your profile and clicking on the specific order.', true),
('What payment methods are accepted?', 'We currently accept payments securely via Razorpay, which supports Credit/Debit cards, UPI, and Netbanking.', true),
('Do you offer international shipping?', 'Currently, we only ship within the selected regions. Please check the delivery availability at checkout.', true);
