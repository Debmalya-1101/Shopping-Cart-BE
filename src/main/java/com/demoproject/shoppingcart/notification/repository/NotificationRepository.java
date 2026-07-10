package com.demoproject.shoppingcart.notification.repository;

import com.demoproject.shoppingcart.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByIdempotencyKey(String idempotencyKey);
    
    long countByStatus(com.demoproject.shoppingcart.notification.model.NotificationStatus status);
    
    org.springframework.data.domain.Page<Notification> findByStatus(com.demoproject.shoppingcart.notification.model.NotificationStatus status, org.springframework.data.domain.Pageable pageable);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Notification n WHERE n.createdAt < :cutoffDate")
    int deleteOlderThan(@org.springframework.data.repository.query.Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}
