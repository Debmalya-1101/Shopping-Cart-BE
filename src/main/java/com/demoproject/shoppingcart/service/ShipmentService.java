package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.DeliveryPartnerDashboardDTO;
import com.demoproject.shoppingcart.dto.ShipmentResponseDTO;
import com.demoproject.shoppingcart.model.Order;
import com.demoproject.shoppingcart.model.ShipmentStatus;

import java.util.List;

public interface ShipmentService {
    ShipmentResponseDTO createShipmentForOrder(Order order);
    List<ShipmentResponseDTO> getUnassignedShipments();
    ShipmentResponseDTO assignDeliveryPartner(Long shipmentId, Long partnerId);
    ShipmentResponseDTO updateShipmentStatus(Long shipmentId, ShipmentStatus status);
    ShipmentResponseDTO getShipmentByOrderId(Long orderId);
    ShipmentResponseDTO getShipmentById(Long shipmentId);

    // Delivery Partner APIs
    List<ShipmentResponseDTO> getActiveShipmentsForPartner(Long partnerId);
    List<ShipmentResponseDTO> getShipmentHistoryForPartner(Long partnerId);
    ShipmentResponseDTO getShipmentDetailsForPartner(Long shipmentId, Long partnerId);
    ShipmentResponseDTO updateShipmentStatusByPartner(Long shipmentId, Long partnerId, ShipmentStatus newStatus, String failureReason);
    DeliveryPartnerDashboardDTO getDashboardMetricsForPartner(Long partnerId);
}
