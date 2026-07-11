package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.DeliveryPartnerResponseDTO;
import com.demoproject.shoppingcart.dto.DeliveryPartnerSignupRequest;
import com.demoproject.shoppingcart.dto.DeliveryPartnerStatusUpdateRequest;
import com.demoproject.shoppingcart.model.DeliveryPartnerStatus;

import com.demoproject.shoppingcart.dto.PageResponse;
import java.util.List;

public interface DeliveryPartnerService {
    void registerDeliveryPartner(DeliveryPartnerSignupRequest request);
    DeliveryPartnerResponseDTO getDeliveryPartnerById(Long id);
    DeliveryPartnerResponseDTO getDeliveryPartnerByUserId(Long userId);
    PageResponse<DeliveryPartnerResponseDTO> getAllDeliveryPartners(DeliveryPartnerStatus status, int page, int size);
    DeliveryPartnerResponseDTO updateDeliveryPartnerStatus(Long id, DeliveryPartnerStatusUpdateRequest request, Long adminUserId, String adminUsername);
}
