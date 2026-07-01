package com.demoproject.shoppingcart.repository;

import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.DeliveryPartner;
import com.demoproject.shoppingcart.model.DeliveryPartnerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, Long> {
    Optional<DeliveryPartner> findByUser(AppUser user);
    Optional<DeliveryPartner> findByUserId(Long userId);
    List<DeliveryPartner> findByStatus(DeliveryPartnerStatus status);
}
