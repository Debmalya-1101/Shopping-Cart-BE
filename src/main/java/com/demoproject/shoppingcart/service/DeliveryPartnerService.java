package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.DeliveryPartnerResponseDTO;
import com.demoproject.shoppingcart.dto.DeliveryPartnerSignupRequest;
import com.demoproject.shoppingcart.dto.DeliveryPartnerStatusUpdateRequest;
import com.demoproject.shoppingcart.model.DeliveryPartnerStatus;

import java.util.List;

public interface DeliveryPartnerService {
    void registerDeliveryPartner(DeliveryPartnerSignupRequest request);
    DeliveryPartnerResponseDTO getDeliveryPartnerById(Long id);
    DeliveryPartnerResponseDTO getDeliveryPartnerByUserId(Long userId);
    List<DeliveryPartnerResponseDTO> getAllDeliveryPartners(DeliveryPartnerStatus status);
    DeliveryPartnerResponseDTO updateDeliveryPartnerStatus(Long id, DeliveryPartnerStatusUpdateRequest request, Long adminUserId, String adminUsername);
}
