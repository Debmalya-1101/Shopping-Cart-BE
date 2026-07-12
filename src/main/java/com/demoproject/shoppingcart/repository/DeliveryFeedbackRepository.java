package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.DeliveryFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryFeedbackRepository extends JpaRepository<DeliveryFeedback, Long> {
    Optional<DeliveryFeedback> findByOrderId(Long orderId);
    Page<DeliveryFeedback> findByDeliveryPartnerId(Long deliveryPartnerId, Pageable pageable);
    
    @Query("SELECT AVG(f.rating) FROM DeliveryFeedback f WHERE f.deliveryPartner.id = :partnerId")
    Double getAverageRatingForPartner(@Param("partnerId") Long partnerId);
    
    @Query("SELECT COUNT(f) FROM DeliveryFeedback f WHERE f.deliveryPartner.id = :partnerId")
    Long countFeedbackForPartner(@Param("partnerId") Long partnerId);

    @Query("SELECT f.deliveryPartner.id, AVG(f.rating), COUNT(f) FROM DeliveryFeedback f GROUP BY f.deliveryPartner.id")
    List<Object[]> getAllPartnerRatingSummaries();
}
