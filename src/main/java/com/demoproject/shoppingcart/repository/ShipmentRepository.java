package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.Shipment;
import com.demoproject.shoppingcart.model.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByOrderId(Long orderId);
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    Page<Shipment> findByDeliveryPartnerId(Long deliveryPartnerId, Pageable pageable);
    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);
    Page<Shipment> findByStatusIn(List<ShipmentStatus> statuses, Pageable pageable);
    Page<Shipment> findByDeliveryPartnerIdAndStatusIn(Long partnerId, List<ShipmentStatus> statuses, Pageable pageable);

    @Query("SELECT s.status, COUNT(s) FROM Shipment s WHERE s.deliveryPartner.id = :partnerId GROUP BY s.status")
    List<Object[]> countShipmentsByStatusForPartner(@Param("partnerId") Long partnerId);
}
