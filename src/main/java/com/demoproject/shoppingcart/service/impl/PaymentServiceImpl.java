package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.PaymentConfirmRequestDTO;
import com.demoproject.shoppingcart.dto.PaymentInitiateResponseDTO;
import com.demoproject.shoppingcart.event.OrderConfirmedEvent;
import com.demoproject.shoppingcart.event.PaymentFailedEvent;
import com.demoproject.shoppingcart.event.PaymentSuccessEvent;
import com.demoproject.shoppingcart.model.*;
import com.demoproject.shoppingcart.repository.OrderRepository;
import com.demoproject.shoppingcart.repository.ProductRepository;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.service.PaymentService;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
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

    private static final int MAX_PAYMENT_RETRIES = 3;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final com.demoproject.shoppingcart.service.InventoryService inventoryService;
    private final com.demoproject.shoppingcart.service.ShipmentService shipmentService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public PaymentServiceImpl(OrderRepository orderRepository,
                              ProductRepository productRepository,
                              UserRepository userRepository,
                              com.demoproject.shoppingcart.service.InventoryService inventoryService,
                              com.demoproject.shoppingcart.service.ShipmentService shipmentService,
                              ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
        this.shipmentService = shipmentService;
        this.eventPublisher = eventPublisher;
    }

    private AppUser getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public PaymentInitiateResponseDTO initiatePayment(Long orderId) {

        // Step 1: Transactional pre-checks
        Order order = prepareOrderForPayment(orderId);

        // Step 2: External HTTP Call (outside DB Tx)
        String paymentReferenceId = "REF_" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String token = "";

        try {
            RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", order.getTotal() * 100); // paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", paymentReferenceId);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            token = razorpayOrder.get("id"); // razorpay_order_id
        } catch (RazorpayException e) {
            throw new RuntimeException("Error while creating Razorpay order: " + e.getMessage(), e);
        }

        // Step 3: Persist payment initiation state
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

        if (order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new RuntimeException("Payment already completed for this order");
        }

        if (order.getRetryCount() >= MAX_PAYMENT_RETRIES) {
            throw new RuntimeException("Maximum payment retries exceeded. Please create a new order.");
        }

        // If this is a retry after PAYMENT_FAILED: re-reserve stock and reset to PENDING_PAYMENT
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
            order.setStatus(OrderStatus.PENDING_PAYMENT);
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

        // Idempotency: absorb duplicate success webhooks silently
        if (order.getPaymentStatus() == PaymentStatus.SUCCESS ||
                order.getPaymentStatus() == PaymentStatus.SUCCESS_REQUIRES_REFUND ||
                order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            if (request.isSuccess()) {
                return "Payment already completed. Ignored.";
            }
        }

        // ── Late success webhook recovery (order was PAYMENT_FAILED by cron) ──
        if (order.getPaymentStatus() == PaymentStatus.FAILED) {
            if (!request.isSuccess()) {
                return "Payment already failed. Ignored.";
            }

            try {
                // Re-reserve stock to check availability
                for (OrderItem item : order.getItems()) {
                    inventoryService.reserveStock(
                            item.getProduct().getId(),
                            item.getQuantity().intValue(),
                            "ORDER",
                            order.getId().toString(),
                            "Late webhook recovery"
                    );
                }

                // Stock available — resurrect order to CONFIRMED
                order.setStatus(OrderStatus.CONFIRMED);
                order.setPaymentStatus(PaymentStatus.SUCCESS);
                order.setPaymentCompletedAt(LocalDateTime.now());
                orderRepository.save(order);

                // Consume stock
                for (OrderItem item : order.getItems()) {
                    inventoryService.consumeStock(
                            item.getProduct().getId(),
                            item.getQuantity().intValue(),
                            "ORDER",
                            order.getId().toString(),
                            "Late webhook success"
                    );
                }

                // Create shipment and publish confirmation event
                var shipmentDTO = shipmentService.createShipmentForOrder(order);
                publishConfirmedEvents(order, user, shipmentDTO);

                return "Payment successful (Late Recovery).";

            } catch (Exception e) {
                // Stock gone — money taken but cannot fulfil; flag for refund
                order.setPaymentStatus(PaymentStatus.SUCCESS_REQUIRES_REFUND);
                order.setPaymentCompletedAt(LocalDateTime.now());
                orderRepository.save(order);
                return "Payment successful but stock unavailable. Requires refund.";
            }
        }

        if (order.getPaymentStatus() != PaymentStatus.INITIATED) {
            throw new RuntimeException("Invalid order state for payment confirmation.");
        }

        // ── Payment failed ────────────────────────────────────────────────────
        if (!request.isSuccess()) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            order.setRetryCount(order.getRetryCount() + 1);
            orderRepository.save(order);

            // Release reserved stock
            for (OrderItem item : order.getItems()) {
                inventoryService.releaseStock(
                        item.getProduct().getId(),
                        item.getQuantity().intValue(),
                        "ORDER",
                        order.getId().toString(),
                        "Payment failed"
                );
            }

            // Notify user of failure and remaining retries
            int remaining = MAX_PAYMENT_RETRIES - order.getRetryCount();
            eventPublisher.publishEvent(new PaymentFailedEvent(
                    order.getId(),
                    user.getId(),
                    user.getEmailId(),
                    order.getRetryCount(),
                    MAX_PAYMENT_RETRIES,
                    LocalDateTime.now()
            ));

            return "Payment failed. You can retry payment up to " + remaining + " more time(s).";
        }

        // ── Payment success ───────────────────────────────────────────────────
        // Verify Razorpay signature before proceeding
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

        // Consume inventory (stock was reserved at checkout)
        for (OrderItem item : order.getItems()) {
            inventoryService.consumeStock(
                    item.getProduct().getId(),
                    item.getQuantity().intValue(),
                    "ORDER",
                    order.getId().toString(),
                    "Payment success"
            );
        }

        order.setPaymentStatus(PaymentStatus.SUCCESS);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentCompletedAt(LocalDateTime.now());
        order.setRetryCount(0);
        orderRepository.save(order);

        // Create shipment record
        var shipmentDTO = shipmentService.createShipmentForOrder(order);

        // Publish all relevant events
        publishConfirmedEvents(order, user, shipmentDTO);

        return "Payment successful";
    }

    /**
     * Publishes PaymentSuccessEvent (for backwards compat) and the richer OrderConfirmedEvent
     * (with tracking number + ETA) that is used for the order confirmation email.
     */
    private void publishConfirmedEvents(Order order, AppUser user,
                                        com.demoproject.shoppingcart.dto.ShipmentResponseDTO shipmentDTO) {
        eventPublisher.publishEvent(new PaymentSuccessEvent(
                order.getId(),
                user.getId(),
                user.getEmailId(),
                order.getPaymentReferenceId(),
                order.getTotal(),
                order.getPaymentCompletedAt(),
                "Razorpay"
        ));

        eventPublisher.publishEvent(new OrderConfirmedEvent(
                order.getId(),
                user.getId(),
                user.getEmailId(),
                order.getTotal(),
                shipmentDTO.getTrackingNumber(),
                shipmentDTO.getExpectedDeliveryDate(),
                order.getPaymentCompletedAt()
        ));
    }
}
