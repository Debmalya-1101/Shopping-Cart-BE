package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.PaymentConfirmRequestDTO;
import com.demoproject.shoppingcart.dto.PaymentInitiateResponseDTO;
import com.demoproject.shoppingcart.model.*;
import com.demoproject.shoppingcart.repository.OrderRepository;
import com.demoproject.shoppingcart.repository.ProductRepository;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.service.PaymentService;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public PaymentServiceImpl(OrderRepository orderRepository,
                              ProductRepository productRepository,
                              UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    private AppUser getLoggedInUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public PaymentInitiateResponseDTO initiatePayment(Long orderId) {

        AppUser user = getLoggedInUser();

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        // Prevent duplicate payment if already successful
        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new RuntimeException("Payment already completed for this order");
        }

        // Generate unique payment reference ID
        String paymentReferenceId = "REF_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();

        // Mock token (like Razorpay orderId)
        String token = "PAY_" + System.currentTimeMillis();

        // Set payment initiation details
        order.setPaymentReferenceId(paymentReferenceId);
        order.setPaymentInitiatedAt(LocalDateTime.now());

        orderRepository.save(order);

        return new PaymentInitiateResponseDTO(
                order.getId(),
                order.getTotal(),
                "INR",
                token,
                paymentReferenceId,
                order.getPaymentInitiatedAt()
        );
    }

    @Override
    public String confirmPayment(PaymentConfirmRequestDTO request) {

        AppUser user = getLoggedInUser();

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        // Prevent duplicate payment confirmation
        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new RuntimeException("Payment already completed. Duplicate payment attempt prevented.");
        }

        if (!request.isSuccess()) {
            // Handle failed payment
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setRetryCount(order.getRetryCount() + 1);
            orderRepository.save(order);

            // Kafka Event: PaymentFailedEvent
            // paymentEventProducer.sendPaymentFailedEvent(order);

            return "Payment failed. You can retry payment up to 3 times.";
        }

        // 🔥 Reduce inventory ONLY after success
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();

            if (product.getStock() < item.getQuantity()) {
                // Mark as failed if stock no longer available
                order.setPaymentStatus(PaymentStatus.FAILED);
                order.setRetryCount(order.getRetryCount() + 1);
                orderRepository.save(order);

                throw new RuntimeException(
                        "Stock not available during payment for product: " + product.getName()
                );
            }

            product.setStock((int) (product.getStock() - item.getQuantity()));
            productRepository.save(product);
        }

        // Payment successful
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setPaymentCompletedAt(LocalDateTime.now());
        order.setRetryCount(0); // Reset retry count on success
        orderRepository.save(order);

        // Kafka Event: PaymentSuccessEvent
        // paymentEventProducer.sendPaymentSuccessEvent(order);
        // Kafka Event: OrderPlacedEvent
        // orderEventProducer.sendOrderPlacedEvent(order);

        return "Payment successful";
    }
}

