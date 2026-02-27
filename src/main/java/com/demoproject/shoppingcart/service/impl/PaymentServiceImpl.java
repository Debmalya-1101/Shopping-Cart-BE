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

        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new RuntimeException("Payment already completed");
        }

        // Mock token (like Razorpay orderId)
        String token = "PAY_" + System.currentTimeMillis();

        return new PaymentInitiateResponseDTO(
                order.getId(),
                order.getTotal(),
                "INR",
                token
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

        if (!request.isSuccess()) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            return "Payment failed";
        }

        // 🔥 Reduce inventory ONLY after success
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();

            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException(
                        "Stock not available during payment for product: " + product.getName()
                );
            }

            product.setStock((int) (product.getStock() - item.getQuantity()));
            productRepository.save(product);
        }

        order.setPaymentStatus(PaymentStatus.SUCCESS);
        orderRepository.save(order);

        // Kafka later:
        // paymentEventProducer.sendPaymentSuccessEvent(order);

        return "Payment successful";
    }
}

