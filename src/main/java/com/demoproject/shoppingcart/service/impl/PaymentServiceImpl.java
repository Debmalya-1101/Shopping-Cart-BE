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

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final com.demoproject.shoppingcart.service.InventoryService inventoryService;
    private final com.demoproject.shoppingcart.service.ShipmentService shipmentService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public PaymentServiceImpl(OrderRepository orderRepository,
                              ProductRepository productRepository,
                              UserRepository userRepository,
                              com.demoproject.shoppingcart.service.InventoryService inventoryService,
                              com.demoproject.shoppingcart.service.ShipmentService shipmentService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
        this.shipmentService = shipmentService;
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

        // Step 1: Transactional pre-checks
        Order order = prepareOrderForPayment(orderId);

        // Step 2: External HTTP Call (No DB Tx)
        // Generate unique payment reference ID (used as receipt in Razorpay)
        String paymentReferenceId = "REF_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String token = "";

        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            // Amount in paise (multiply by 100)
            orderRequest.put("amount", order.getTotal() * 100); 
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", paymentReferenceId);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            token = razorpayOrder.get("id"); // razorpay_order_id
        } catch (RazorpayException e) {
            throw new RuntimeException("Error while creating Razorpay order: " + e.getMessage(), e);
        }

        // Step 3: Transactional save state
        return savePaymentInitiationState(orderId, paymentReferenceId, token);
    }

    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    protected Order prepareOrderForPayment(Long orderId) {

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

        // Block if maximum retries exceeded
        if (order.getRetryCount() >= 3) {
            throw new RuntimeException("Maximum payment retries exceeded. Please create a new order.");
        }

        // If this is a retry (status FAILED), we must re-reserve the stock before allowing retry
        if (order.getPaymentStatus() == PaymentStatus.FAILED) {
            for (OrderItem item : order.getItems()) {
                inventoryService.reserveStock(
                        item.getProduct().getId(),
                        item.getQuantity().intValue(),
                        "ORDER",
                        order.getId().toString(),
                        "Payment retry reservation"
                );
            }
            // Transition back to INITIATED
            order.setPaymentStatus(PaymentStatus.INITIATED);
        }

        return orderRepository.save(order);
    }

    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    protected PaymentInitiateResponseDTO savePaymentInitiationState(Long orderId, String paymentReferenceId, String token) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

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
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public String confirmPayment(PaymentConfirmRequestDTO request) {

        AppUser user = getLoggedInUser();

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        // Safe Idempotency: Silently absorb duplicate webhooks
        if (order.getPaymentStatus() == PaymentStatus.SUCCESS || 
            order.getPaymentStatus() == PaymentStatus.SUCCESS_REQUIRES_REFUND || 
            order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            if (request.isSuccess()) {
                return "Payment already completed. Ignored.";
            }
        }

        if (order.getPaymentStatus() == PaymentStatus.FAILED) {
            if (!request.isSuccess()) {
                return "Payment already failed. Ignored.";
            }
            
            // LATE SUCCESS WEBHOOK RECOVERY
            // The order was failed (e.g. by 30-min cron), but we just got a late success webhook!
            try {
                // Step 1: Attempt to re-reserve stock
                for (OrderItem item : order.getItems()) {
                    inventoryService.reserveStock(
                            item.getProduct().getId(),
                            item.getQuantity().intValue(),
                            "ORDER",
                            order.getId().toString(),
                            "Late webhook recovery"
                    );
                }
                
                // Step 2: Stock is available. Resurrect order!
                order.setStatus(OrderStatus.PLACED);
                order.setPaymentStatus(PaymentStatus.SUCCESS);
                order.setPaymentCompletedAt(LocalDateTime.now());
                orderRepository.save(order);
                
                // Call consumeStock
                for (OrderItem item : order.getItems()) {
                    inventoryService.consumeStock(
                            item.getProduct().getId(),
                            item.getQuantity().intValue(),
                            "ORDER",
                            order.getId().toString(),
                            "Late webhook success"
                    );
                }

                // Create shipment for resurrected order
                shipmentService.createShipmentForOrder(order);

                return "Payment successful (Late Recovery).";
                
            } catch (Exception e) {
                // Step 3: Stock is gone. Record money taken but needs refund.
                order.setPaymentStatus(PaymentStatus.SUCCESS_REQUIRES_REFUND);
                order.setPaymentCompletedAt(LocalDateTime.now());
                orderRepository.save(order);
                return "Payment successful but stock unavailable. Requires refund.";
            }
        }

        if (order.getPaymentStatus() != PaymentStatus.INITIATED) {
            throw new RuntimeException("Invalid order state for payment confirmation.");
        }

        if (!request.isSuccess()) {
            // Handle failed payment
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setRetryCount(order.getRetryCount() + 1);
            orderRepository.save(order);

            // Release stock on payment failure
            for (OrderItem item : order.getItems()) {
                inventoryService.releaseStock(
                        item.getProduct().getId(),
                        item.getQuantity().intValue(),
                        "ORDER",
                        order.getId().toString(),
                        "Payment failed"
                );
            }

            return "Payment failed. You can retry payment up to 3 times.";
        }

        // Verify Razorpay signature
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getPaymentToken());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);

            if (!isValid) {
                throw new RuntimeException("Invalid Razorpay signature. Payment verification failed.");
            }
        } catch (RazorpayException e) {
            throw new RuntimeException("Error verifying Razorpay signature: " + e.getMessage(), e);
        }

        // Consume inventory ONLY after success
        for (OrderItem item : order.getItems()) {
            inventoryService.consumeStock(
                    item.getProduct().getId(),
                    item.getQuantity().intValue(),
                    "ORDER",
                    order.getId().toString(),
                    "Payment success"
            );
        }

        // Payment successful
        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setPaymentCompletedAt(LocalDateTime.now());
        order.setRetryCount(0); // Reset retry count on success
        orderRepository.save(order);

        // Create shipment
        shipmentService.createShipmentForOrder(order);

        return "Payment successful";
    }
}

