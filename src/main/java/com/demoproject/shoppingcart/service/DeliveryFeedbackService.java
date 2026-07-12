package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.AdminDeliveryFeedbackResponseDTO;
import com.demoproject.shoppingcart.dto.DeliveryFeedbackRequestDTO;
import com.demoproject.shoppingcart.dto.DeliveryFeedbackResponseDTO;
import com.demoproject.shoppingcart.dto.DeliveryPartnerRatingSummaryDTO;

import com.demoproject.shoppingcart.dto.PageResponse;
import java.util.List;

public interface DeliveryFeedbackService {
    DeliveryFeedbackResponseDTO submitFeedback(Long orderId, String username, DeliveryFeedbackRequestDTO request);
    PageResponse<DeliveryFeedbackResponseDTO> getFeedbackForPartner(Long partnerId, int page, int size);
    DeliveryPartnerRatingSummaryDTO getRatingSummaryForPartner(Long partnerId);
    PageResponse<AdminDeliveryFeedbackResponseDTO> getAdminFeedbackForPartner(Long partnerId, int page, int size);
    List<DeliveryPartnerRatingSummaryDTO> getAllPartnerRatings();
    com.demoproject.shoppingcart.dto.DeliveryFeedbackStatusDTO getFeedbackStatus(Long orderId, String username);
}
