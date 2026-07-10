package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.DeliveryPartnerDashboardDTO;
import com.demoproject.shoppingcart.dto.ShipmentResponseDTO;
import com.demoproject.shoppingcart.event.*;
import com.demoproject.shoppingcart.model.*;
import com.demoproject.shoppingcart.repository.DeliveryPartnerRepository;
import com.demoproject.shoppingcart.repository.OrderRepository;
import com.demoproject.shoppingcart.repository.ShipmentRepository;
import com.demoproject.shoppingcart.service.ShipmentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

/**
 * Manages the lifecycle of Shipments and keeps the parent Order status in sync.
 *
 * ShipmentStatus → OrderStatus mapping (canonical source of truth for order tracking):
 *
 *   CREATED           → (no order status change; admin still needs to assign partner)
 *   ASSIGNED          → PROCESSING
 *   PICKED_UP         → SHIPPED
 *   OUT_FOR_DELIVERY  → OUT_FOR_DELIVERY
 *   DELIVERED         → DELIVERED
 *   DELIVERY_FAILED   → DELIVERY_FAILED
 *   RETURNED          → RETURNED
 */
@Service
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ShipmentServiceImpl(ShipmentRepository shipmentRepository,
                               OrderRepository orderRepository,
                               DeliveryPartnerRepository deliveryPartnerRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.eventPublisher = eventPublisher;
    }

    // ── Shipment Creation ─────────────────────────────────────────────────────

    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public ShipmentResponseDTO createShipmentForOrder(Order order) {
        if (shipmentRepository.findByOrderId(order.getId()).isPresent()) {
            throw new RuntimeException("Shipment already exists for order id: " + order.getId());
        }

        Shipment shipment = new Shipment();
        shipment.setOrder(order);
        shipment.setStatus(ShipmentStatus.CREATED);

        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        shipment.setTrackingNumber(String.format("SHP-%s-%06d", datePart, order.getId()));
        shipment.setExpectedDeliveryDate(LocalDate.now().plusDays(3));

        shipment = shipmentRepository.save(shipment);
        ShipmentResponseDTO dto = mapToDTO(shipment);

        // Notify admin/fulfilment that a new shipment needs partner assignment
        eventPublisher.publishEvent(new ShipmentCreatedEvent(
                shipment.getId(),
                order.getId(),
                shipment.getTrackingNumber(),
                shipment.getExpectedDeliveryDate(),
                shipment.getCreatedAt()
        ));

        return dto;
    }

    // ── Admin: Assign Delivery Partner ────────────────────────────────────────

    @Override
    public List<ShipmentResponseDTO> getUnassignedShipments() {
        return shipmentRepository.findByStatusIn(List.of(ShipmentStatus.CREATED, ShipmentStatus.DELIVERY_FAILED))
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public ShipmentResponseDTO assignDeliveryPartner(Long shipmentId, Long partnerId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        ShipmentStatus currentStatus = shipment.getStatus();
        // Allow assignment from CREATED, re-assignment from ASSIGNED, or re-assignment after DELIVERY_FAILED
        if (currentStatus != ShipmentStatus.CREATED &&
                currentStatus != ShipmentStatus.ASSIGNED &&
                currentStatus != ShipmentStatus.DELIVERY_FAILED) {
            throw new IllegalStateException(
                    "Cannot assign delivery partner. Shipment is in " + currentStatus + " status.");
        }

        if (currentStatus == ShipmentStatus.ASSIGNED &&
                shipment.getDeliveryPartner() != null &&
                shipment.getDeliveryPartner().getId().equals(partnerId)) {
            throw new IllegalStateException("Shipment is already assigned to this delivery partner.");
        }

        DeliveryPartner partner = deliveryPartnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Delivery Partner not found"));

        if (partner.getStatus() != DeliveryPartnerStatus.APPROVED) {
            throw new RuntimeException("Delivery Partner is not approved.");
        }

        shipment.setDeliveryPartner(partner);
        shipment.setStatus(ShipmentStatus.ASSIGNED);
        shipment = shipmentRepository.save(shipment);

        // Sync order status → PROCESSING
        syncOrderStatus(shipment.getOrder(), OrderStatus.PROCESSING);

        // Notify delivery partner of their new job
        eventPublisher.publishEvent(new ShipmentAssignedEvent(
                shipment.getId(),
                shipment.getOrder().getId(),
                partner.getId(),
                partner.getFullName(),
                shipment.getTrackingNumber(),
                LocalDateTime.now()
        ));

        return mapToDTO(shipment);
    }

    // ── Admin: General Status Update (internal use) ───────────────────────────

    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public ShipmentResponseDTO updateShipmentStatus(Long shipmentId, ShipmentStatus newStatus) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        ShipmentStatus currentStatus = shipment.getStatus();

        if (currentStatus == newStatus) {
            throw new IllegalStateException("Shipment is already in " + currentStatus + " status.");
        }

        boolean isValid = switch (currentStatus) {
            case CREATED -> false; // only via assignDeliveryPartner
            case ASSIGNED -> (newStatus == ShipmentStatus.PICKED_UP);
            case PICKED_UP -> (newStatus == ShipmentStatus.OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY ->
                    (newStatus == ShipmentStatus.DELIVERED || newStatus == ShipmentStatus.DELIVERY_FAILED);
            case DELIVERY_FAILED ->
                    (newStatus == ShipmentStatus.OUT_FOR_DELIVERY || newStatus == ShipmentStatus.RETURNED);
            default -> false; // DELIVERED, RETURNED are terminal
        };

        if (!isValid) {
            throw new IllegalStateException(
                    "Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        shipment.setStatus(newStatus);
        shipment = shipmentRepository.save(shipment);
        publishShipmentEvents(shipment, newStatus, null);

        return mapToDTO(shipment);
    }

    // ── Read operations ───────────────────────────────────────────────────────

    @Override
    public ShipmentResponseDTO getShipmentByOrderId(Long orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Shipment not found for order id: " + orderId));
        return mapToDTO(shipment);
    }

    @Override
    public ShipmentResponseDTO getShipmentById(Long shipmentId) {
        return mapToDTO(shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found")));
    }

    // ── Delivery Partner APIs ─────────────────────────────────────────────────

    @Override
    public List<ShipmentResponseDTO> getActiveShipmentsForPartner(Long partnerId) {
        return shipmentRepository.findByDeliveryPartnerIdAndStatusIn(partnerId,
                List.of(ShipmentStatus.ASSIGNED, ShipmentStatus.PICKED_UP, ShipmentStatus.OUT_FOR_DELIVERY))
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<ShipmentResponseDTO> getShipmentHistoryForPartner(Long partnerId) {
        return shipmentRepository.findByDeliveryPartnerIdAndStatusIn(partnerId,
                List.of(ShipmentStatus.DELIVERED, ShipmentStatus.DELIVERY_FAILED, ShipmentStatus.RETURNED))
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public ShipmentResponseDTO getShipmentDetailsForPartner(Long shipmentId, Long partnerId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        if (shipment.getDeliveryPartner() == null ||
                !shipment.getDeliveryPartner().getId().equals(partnerId)) {
            throw new RuntimeException("Unauthorized: Shipment not assigned to this delivery partner");
        }

        return mapToDTO(shipment);
    }

    /**
     * Delivery partner updates shipment status.
     * Allowed transitions: ASSIGNED → PICKED_UP → OUT_FOR_DELIVERY → DELIVERED | DELIVERY_FAILED
     * Partners cannot mark RETURNED (that is an admin action after irrecoverable failure).
     */
    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public ShipmentResponseDTO updateShipmentStatusByPartner(Long shipmentId, Long partnerId,
                                                              ShipmentStatus newStatus,
                                                              String failureReason) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        if (shipment.getDeliveryPartner() == null ||
                !shipment.getDeliveryPartner().getId().equals(partnerId)) {
            throw new RuntimeException("Unauthorized: Shipment not assigned to this delivery partner");
        }

        ShipmentStatus currentStatus = shipment.getStatus();
        if (currentStatus == newStatus) {
            throw new IllegalStateException("Shipment is already in " + currentStatus + " status.");
        }

        boolean isValid = switch (currentStatus) {
            case ASSIGNED -> (newStatus == ShipmentStatus.PICKED_UP);
            case PICKED_UP -> (newStatus == ShipmentStatus.OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY ->
                    (newStatus == ShipmentStatus.DELIVERED || newStatus == ShipmentStatus.DELIVERY_FAILED);
            default -> false; // partners cannot go backward or mark RETURNED
        };

        if (!isValid) {
            throw new IllegalStateException(
                    "Invalid status transition by partner from " + currentStatus + " to " + newStatus);
        }

        if (newStatus == ShipmentStatus.DELIVERY_FAILED) {
            if (failureReason == null || failureReason.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Failure reason must be provided when marking shipment as DELIVERY_FAILED.");
            }
            shipment.setFailureReason(failureReason);
        }

        shipment.setStatus(newStatus);
        shipment = shipmentRepository.save(shipment);
        publishShipmentEvents(shipment, newStatus, failureReason);

        return mapToDTO(shipment);
    }

    @Override
    public DeliveryPartnerDashboardDTO getDashboardMetricsForPartner(Long partnerId) {
        List<Object[]> counts = shipmentRepository.countShipmentsByStatusForPartner(partnerId);

        long totalAssigned = 0, totalPickedUp = 0, totalOutForDelivery = 0,
                totalDelivered = 0, totalFailed = 0;

        for (Object[] row : counts) {
            ShipmentStatus status = (ShipmentStatus) row[0];
            Long count = (Long) row[1];
            switch (status) {
                case ASSIGNED -> totalAssigned = count;
                case PICKED_UP -> totalPickedUp = count;
                case OUT_FOR_DELIVERY -> totalOutForDelivery = count;
                case DELIVERED -> totalDelivered = count;
                case DELIVERY_FAILED -> totalFailed = count;
                default -> { /* ignore */ }
            }
        }

        return new DeliveryPartnerDashboardDTO(
                totalAssigned, totalPickedUp, totalOutForDelivery, totalDelivered, totalFailed);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Syncs the parent Order's status based on the shipment milestone reached,
     * and publishes the appropriate domain event.
     */
    private void publishShipmentEvents(Shipment shipment, ShipmentStatus newStatus, String failureReason) {
        Order order = shipment.getOrder();
        AppUser user = order.getUser();

        switch (newStatus) {
            case PICKED_UP -> {
                syncOrderStatus(order, OrderStatus.SHIPPED);
                eventPublisher.publishEvent(new ShipmentPickedUpEvent(
                        shipment.getId(), order.getId(),
                        user.getId(), user.getEmailId(),
                        shipment.getTrackingNumber(), LocalDateTime.now()));
            }
            case OUT_FOR_DELIVERY -> {
                syncOrderStatus(order, OrderStatus.OUT_FOR_DELIVERY);
                eventPublisher.publishEvent(new ShipmentOutForDeliveryEvent(
                        shipment.getId(), order.getId(),
                        user.getId(), user.getEmailId(),
                        shipment.getTrackingNumber(),
                        shipment.getExpectedDeliveryDate(),
                        LocalDateTime.now()));
            }
            case DELIVERED -> {
                syncOrderStatus(order, OrderStatus.DELIVERED);
                Long dpId = shipment.getDeliveryPartner() != null ? shipment.getDeliveryPartner().getId() : null;
                eventPublisher.publishEvent(new OrderDeliveredEvent(
                        order.getId(), user.getId(), user.getEmailId(),
                        dpId, shipment.getTrackingNumber(), LocalDateTime.now()));
            }
            case DELIVERY_FAILED -> {
                syncOrderStatus(order, OrderStatus.DELIVERY_FAILED);
                eventPublisher.publishEvent(new DeliveryFailedEvent(
                        shipment.getId(), order.getId(),
                        user.getId(), user.getEmailId(),
                        failureReason, LocalDateTime.now()));
            }
            case RETURNED -> {
                syncOrderStatus(order, OrderStatus.RETURNED);
                // No dedicated ReturnedEvent yet — future iteration
            }
            default -> { /* CREATED, ASSIGNED handled elsewhere */ }
        }
    }

    /**
     * Persists the new OrderStatus on the parent Order entity.
     */
    private void syncOrderStatus(Order order, OrderStatus newOrderStatus) {
        order.setStatus(newOrderStatus);
        orderRepository.save(order);
    }

    private ShipmentResponseDTO mapToDTO(Shipment shipment) {
        Long dpId = shipment.getDeliveryPartner() != null ? shipment.getDeliveryPartner().getId() : null;
        return new ShipmentResponseDTO(
                shipment.getId(),
                shipment.getOrder().getId(),
                dpId,
                shipment.getStatus(),
                shipment.getTrackingNumber(),
                shipment.getExpectedDeliveryDate(),
                shipment.getFailureReason(),
                shipment.getCreatedAt(),
                shipment.getUpdatedAt()
        );
    }
}
