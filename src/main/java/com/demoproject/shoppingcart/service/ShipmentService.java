package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.DeliveryPartnerDashboardDTO;
import com.demoproject.shoppingcart.dto.ShipmentResponseDTO;
import com.demoproject.shoppingcart.model.Order;
import com.demoproject.shoppingcart.model.ShipmentStatus;

import com.demoproject.shoppingcart.dto.PageResponse;
import java.util.List;

public interface ShipmentService {
    ShipmentResponseDTO createShipmentForOrder(Order order);
    PageResponse<ShipmentResponseDTO> getUnassignedShipments(int page, int size);
    ShipmentResponseDTO assignDeliveryPartner(Long shipmentId, Long partnerId);
    ShipmentResponseDTO updateShipmentStatus(Long shipmentId, ShipmentStatus status);
    ShipmentResponseDTO getShipmentByOrderId(Long orderId);
    ShipmentResponseDTO getShipmentById(Long shipmentId);

    // Delivery Partner APIs
    PageResponse<ShipmentResponseDTO> getActiveShipmentsForPartner(Long partnerId, int page, int size);
    PageResponse<ShipmentResponseDTO> getShipmentHistoryForPartner(Long partnerId, int page, int size);
    ShipmentResponseDTO getShipmentDetailsForPartner(Long shipmentId, Long partnerId);
    ShipmentResponseDTO updateShipmentStatusByPartner(Long shipmentId, Long partnerId, ShipmentStatus newStatus, String failureReason);
    DeliveryPartnerDashboardDTO getDashboardMetricsForPartner(Long partnerId);
}
