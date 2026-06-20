package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.DeliveryPartnerDashboardDTO;
import com.demoproject.shoppingcart.dto.ShipmentResponseDTO;
import com.demoproject.shoppingcart.model.*;
import com.demoproject.shoppingcart.repository.DeliveryPartnerRepository;
import com.demoproject.shoppingcart.repository.OrderRepository;
import com.demoproject.shoppingcart.repository.ShipmentRepository;
import com.demoproject.shoppingcart.service.ShipmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Service
public class ShipmentServiceImpl implements ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;

    public ShipmentServiceImpl(ShipmentRepository shipmentRepository,
                               OrderRepository orderRepository,
                               DeliveryPartnerRepository deliveryPartnerRepository) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
    }

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

        // Generate tracking number: SHP-YYYYMMDD-ORDERID(6 padded)
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String trackingNumber = String.format("SHP-%s-%06d", datePart, order.getId());
        shipment.setTrackingNumber(trackingNumber);

        shipment.setExpectedDeliveryDate(LocalDate.now().plusDays(3));

        shipment = shipmentRepository.save(shipment);
        return mapToDTO(shipment);
    }

    @Override
    public List<ShipmentResponseDTO> getUnassignedShipments() {
        return shipmentRepository.findByStatus(ShipmentStatus.CREATED)
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
        if (currentStatus != ShipmentStatus.CREATED && 
            currentStatus != ShipmentStatus.ASSIGNED && 
            currentStatus != ShipmentStatus.FAILED) {
            throw new IllegalStateException("Cannot assign delivery partner. Shipment is currently in " + currentStatus + " status.");
        }

        if (currentStatus == ShipmentStatus.ASSIGNED && shipment.getDeliveryPartner() != null && shipment.getDeliveryPartner().getId().equals(partnerId)) {
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
        return mapToDTO(shipment);
    }

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

        boolean isValid = false;
        switch (currentStatus) {
            case CREATED:
                // Only allowed to transition to ASSIGNED via assignDeliveryPartner
                break;
            case ASSIGNED:
                isValid = (newStatus == ShipmentStatus.PICKED_UP);
                break;
            case PICKED_UP:
                isValid = (newStatus == ShipmentStatus.OUT_FOR_DELIVERY);
                break;
            case OUT_FOR_DELIVERY:
                isValid = (newStatus == ShipmentStatus.DELIVERED || newStatus == ShipmentStatus.FAILED);
                break;
            case FAILED:
                isValid = (newStatus == ShipmentStatus.OUT_FOR_DELIVERY || newStatus == ShipmentStatus.RETURNED);
                break;
            case DELIVERED:
            case RETURNED:
                // Terminal states
                break;
        }

        if (!isValid) {
            throw new IllegalStateException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        shipment.setStatus(newStatus);
        shipment = shipmentRepository.save(shipment);

        Order order = shipment.getOrder();
        if (newStatus == ShipmentStatus.OUT_FOR_DELIVERY) {
            order.setStatus(OrderStatus.SHIPPED);
            orderRepository.save(order);
        } else if (newStatus == ShipmentStatus.DELIVERED) {
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);
        } else if (newStatus == ShipmentStatus.RETURNED) {
            order.setStatus(OrderStatus.RETURNED);
            orderRepository.save(order);
        }

        return mapToDTO(shipment);
    }

    @Override
    public ShipmentResponseDTO getShipmentByOrderId(Long orderId) {
        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Shipment not found for order id: " + orderId));
        return mapToDTO(shipment);
    }

    @Override
    public ShipmentResponseDTO getShipmentById(Long shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));
        return mapToDTO(shipment);
    }

    @Override
    public List<ShipmentResponseDTO> getActiveShipmentsForPartner(Long partnerId) {
        return shipmentRepository.findByDeliveryPartnerIdAndStatusIn(partnerId, 
                List.of(ShipmentStatus.ASSIGNED, ShipmentStatus.PICKED_UP, ShipmentStatus.OUT_FOR_DELIVERY))
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ShipmentResponseDTO> getShipmentHistoryForPartner(Long partnerId) {
        return shipmentRepository.findByDeliveryPartnerIdAndStatusIn(partnerId, 
                List.of(ShipmentStatus.DELIVERED, ShipmentStatus.FAILED, ShipmentStatus.RETURNED))
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ShipmentResponseDTO getShipmentDetailsForPartner(Long shipmentId, Long partnerId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        if (shipment.getDeliveryPartner() == null || !shipment.getDeliveryPartner().getId().equals(partnerId)) {
            throw new RuntimeException("Unauthorized: Shipment not assigned to this delivery partner");
        }

        return mapToDTO(shipment);
    }

    @Override
    @Transactional
    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public ShipmentResponseDTO updateShipmentStatusByPartner(Long shipmentId, Long partnerId, ShipmentStatus newStatus, String failureReason) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        if (shipment.getDeliveryPartner() == null || !shipment.getDeliveryPartner().getId().equals(partnerId)) {
            throw new RuntimeException("Unauthorized: Shipment not assigned to this delivery partner");
        }

        ShipmentStatus currentStatus = shipment.getStatus();
        if (currentStatus == newStatus) {
            throw new IllegalStateException("Shipment is already in " + currentStatus + " status.");
        }

        boolean isValid = false;
        switch (currentStatus) {
            case ASSIGNED:
                isValid = (newStatus == ShipmentStatus.PICKED_UP);
                break;
            case PICKED_UP:
                isValid = (newStatus == ShipmentStatus.OUT_FOR_DELIVERY);
                break;
            case OUT_FOR_DELIVERY:
                isValid = (newStatus == ShipmentStatus.DELIVERED || newStatus == ShipmentStatus.FAILED);
                break;
            default:
                break;
        }

        if (!isValid) {
            throw new IllegalStateException("Invalid status transition by partner from " + currentStatus + " to " + newStatus);
        }

        if (newStatus == ShipmentStatus.FAILED) {
            if (failureReason == null || failureReason.trim().isEmpty()) {
                throw new IllegalArgumentException("Failure reason must be provided when marking shipment as FAILED.");
            }
            shipment.setFailureReason(failureReason);
        }

        shipment.setStatus(newStatus);
        shipment = shipmentRepository.save(shipment);

        // Map status changes to Order — partner path only updates on meaningful milestones
        Order order = shipment.getOrder();
        if (newStatus == ShipmentStatus.OUT_FOR_DELIVERY) {
            order.setStatus(OrderStatus.SHIPPED);
            orderRepository.save(order);
        } else if (newStatus == ShipmentStatus.DELIVERED) {
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);
        }
        // FAILED does not change order status (admin will decide next action)

        return mapToDTO(shipment);
    }

    @Override
    public DeliveryPartnerDashboardDTO getDashboardMetricsForPartner(Long partnerId) {
        List<Object[]> counts = shipmentRepository.countShipmentsByStatusForPartner(partnerId);

        long totalAssigned = 0;
        long totalPickedUp = 0;
        long totalOutForDelivery = 0;
        long totalDelivered = 0;
        long totalFailed = 0;

        for (Object[] row : counts) {
            ShipmentStatus status = (ShipmentStatus) row[0];
            Long count = (Long) row[1];
            switch (status) {
                case ASSIGNED: totalAssigned = count; break;
                case PICKED_UP: totalPickedUp = count; break;
                case OUT_FOR_DELIVERY: totalOutForDelivery = count; break;
                case DELIVERED: totalDelivered = count; break;
                case FAILED: totalFailed = count; break;
                default: break;
            }
        }

        return new DeliveryPartnerDashboardDTO(
                totalAssigned, totalPickedUp, totalOutForDelivery, totalDelivered, totalFailed
        );
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
