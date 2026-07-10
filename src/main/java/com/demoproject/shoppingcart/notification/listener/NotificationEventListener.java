package com.demoproject.shoppingcart.notification.listener;

import com.demoproject.shoppingcart.event.OrderPlacedEvent;
import com.demoproject.shoppingcart.event.PaymentSuccessEvent;
import com.demoproject.shoppingcart.notification.model.NotificationChannel;
import com.demoproject.shoppingcart.notification.model.NotificationMetadata;
import com.demoproject.shoppingcart.notification.model.NotificationRequest;
import com.demoproject.shoppingcart.notification.model.NotificationType;
import com.demoproject.shoppingcart.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Handling OrderPlacedEvent for order: {}", event.getOrderId());
        
        NotificationMetadata metadata = NotificationMetadata.builder()
                .correlationId(UUID.randomUUID().toString())
                .eventId("order-" + event.getOrderId())
                .sourceService("shopping-cart-order-service")
                .build();
                
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", event.getOrderId());
        payload.put("totalAmount", event.getTotalAmount());
        payload.put("orderedAt", event.getOrderedAt());
        payload.put("items", event.getItems());

        NotificationRequest request = NotificationRequest.builder()
                .type(NotificationType.ORDER_PLACED)
                .channel(NotificationChannel.EMAIL)
                .referenceId(String.valueOf(event.getOrderId()))
                .userId(event.getUserId())
                .to(Collections.singletonList(event.getUserEmail()))
                .subject("Order Confirmation #" + event.getOrderId())
                .payload(payload)
                .metadata(metadata)
                .build();

        notificationService.send(request);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentSuccessEvent(PaymentSuccessEvent event) {
        log.info("Handling PaymentSuccessEvent for payment reference: {}", event.getPaymentReferenceId());
        
        String userEmail = event.getUserEmail();

        NotificationMetadata metadata = NotificationMetadata.builder()
                .correlationId(UUID.randomUUID().toString())
                .eventId("payment-" + event.getPaymentReferenceId())
                .sourceService("shopping-cart-payment-service")
                .build();
                
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", event.getOrderId());
        payload.put("amountPaid", event.getAmount());
        payload.put("transactionId", event.getPaymentReferenceId());
        payload.put("paidAt", event.getPaymentCompletedAt());

        NotificationRequest request = NotificationRequest.builder()
                .type(NotificationType.PAYMENT_SUCCESS)
                .channel(NotificationChannel.EMAIL)
                .referenceId(event.getPaymentReferenceId())
                .userId(event.getUserId())
                .to(Collections.singletonList(userEmail))
                .subject("Payment Successful for Order #" + event.getOrderId())
                .payload(payload)
                .metadata(metadata)
                .build();

        notificationService.send(request);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentFailedEvent(com.demoproject.shoppingcart.event.PaymentFailedEvent event) {
        log.info("Handling PaymentFailedEvent for order: {}", event.getOrderId());

        NotificationMetadata metadata = NotificationMetadata.builder()
                .correlationId(UUID.randomUUID().toString())
                .eventId("payment-failed-" + event.getOrderId())
                .sourceService("shopping-cart-payment-service")
                .build();

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", event.getOrderId());
        payload.put("retryCount", event.getRetryCount());
        payload.put("maxRetries", event.getMaxRetries());
        payload.put("failedAt", event.getFailedAt());

        NotificationRequest request = NotificationRequest.builder()
                .type(NotificationType.PAYMENT_FAILED)
                .channel(NotificationChannel.EMAIL)
                .referenceId("failed-" + event.getOrderId())
                .userId(event.getUserId())
                .to(Collections.singletonList(event.getUserEmail()))
                .subject("Action Required: Payment Failed for Order #" + event.getOrderId())
                .payload(payload)
                .metadata(metadata)
                .build();

        notificationService.send(request);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleShipmentOutForDeliveryEvent(com.demoproject.shoppingcart.event.ShipmentOutForDeliveryEvent event) {
        log.info("Handling ShipmentOutForDeliveryEvent for shipment: {}", event.getShipmentId());

        NotificationMetadata metadata = NotificationMetadata.builder()
                .correlationId(UUID.randomUUID().toString())
                .eventId("shipment-out-" + event.getShipmentId())
                .sourceService("shopping-cart-logistics-service")
                .build();

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", event.getOrderId());
        payload.put("trackingNumber", event.getTrackingNumber());
        payload.put("expectedDeliveryDate", event.getExpectedDeliveryDate());
        payload.put("outForDeliveryAt", event.getOutForDeliveryAt());

        NotificationRequest request = NotificationRequest.builder()
                .type(NotificationType.SHIPMENT_OUT_FOR_DELIVERY)
                .channel(NotificationChannel.EMAIL)
                .referenceId(String.valueOf(event.getShipmentId()))
                .userId(event.getUserId())
                .to(Collections.singletonList(event.getUserEmail()))
                .subject("Your Order #" + event.getOrderId() + " is out for delivery!")
                .payload(payload)
                .metadata(metadata)
                .build();

        notificationService.send(request);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleShipmentPickedUpEvent(com.demoproject.shoppingcart.event.ShipmentPickedUpEvent event) {
        log.info("Handling ShipmentPickedUpEvent for shipment: {}", event.getShipmentId());

        NotificationMetadata metadata = NotificationMetadata.builder()
                .correlationId(UUID.randomUUID().toString())
                .eventId("shipment-picked-up-" + event.getShipmentId())
                .sourceService("shopping-cart-logistics-service")
                .build();

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", event.getOrderId());
        payload.put("trackingNumber", event.getTrackingNumber());
        payload.put("pickedUpAt", event.getPickedUpAt());

        NotificationRequest request = NotificationRequest.builder()
                .type(NotificationType.SHIPMENT_PICKED_UP)
                .channel(NotificationChannel.EMAIL)
                .referenceId(String.valueOf(event.getShipmentId()))
                .userId(event.getUserId())
                .to(Collections.singletonList(event.getUserEmail()))
                .subject("Your Order #" + event.getOrderId() + " has been shipped!")
                .payload(payload)
                .metadata(metadata)
                .build();

        notificationService.send(request);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderDeliveredEvent(com.demoproject.shoppingcart.event.OrderDeliveredEvent event) {
        log.info("Handling OrderDeliveredEvent for order: {}", event.getOrderId());

        NotificationMetadata metadata = NotificationMetadata.builder()
                .correlationId(UUID.randomUUID().toString())
                .eventId("order-delivered-" + event.getOrderId())
                .sourceService("shopping-cart-logistics-service")
                .build();

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", event.getOrderId());
        payload.put("trackingNumber", event.getTrackingNumber());
        payload.put("deliveredAt", event.getDeliveredAt());

        NotificationRequest request = NotificationRequest.builder()
                .type(NotificationType.ORDER_DELIVERED)
                .channel(NotificationChannel.EMAIL)
                .referenceId(String.valueOf(event.getOrderId()))
                .userId(event.getUserId())
                .to(Collections.singletonList(event.getUserEmail()))
                .subject("Order Delivered: #" + event.getOrderId())
                .payload(payload)
                .metadata(metadata)
                .build();

        notificationService.send(request);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCancelledEvent(com.demoproject.shoppingcart.event.OrderCancelledEvent event) {
        log.info("Handling OrderCancelledEvent for order: {}", event.getOrderId());

        NotificationMetadata metadata = NotificationMetadata.builder()
                .correlationId(UUID.randomUUID().toString())
                .eventId("order-cancelled-" + event.getOrderId())
                .sourceService("shopping-cart-order-service")
                .build();

        Map<String, Object> payload = new HashMap<>();
        payload.put("orderId", event.getOrderId());
        payload.put("cancelledBy", event.getCancelledBy().name());
        payload.put("cancelReason", event.getCancelReason());
        payload.put("refundRequired", event.isRefundRequired());
        payload.put("refundAmount", event.getRefundAmount());
        payload.put("cancelledAt", event.getCancelledAt());

        NotificationRequest request = NotificationRequest.builder()
                .type(NotificationType.ORDER_CANCELLED)
                .channel(NotificationChannel.EMAIL)
                .referenceId(String.valueOf(event.getOrderId()))
                .userId(event.getUserId())
                .to(Collections.singletonList(event.getUserEmail()))
                .subject("Update on your Order #" + event.getOrderId())
                .payload(payload)
                .metadata(metadata)
                .build();

        notificationService.send(request);
    }
}
