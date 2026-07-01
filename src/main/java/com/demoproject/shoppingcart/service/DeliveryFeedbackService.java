package com.demoproject.shoppingcart.service;

import com.demoproject.shoppingcart.dto.AdminDeliveryFeedbackResponseDTO;
import com.demoproject.shoppingcart.dto.DeliveryFeedbackRequestDTO;
import com.demoproject.shoppingcart.dto.DeliveryFeedbackResponseDTO;
import com.demoproject.shoppingcart.dto.DeliveryPartnerRatingSummaryDTO;

import java.util.List;

public interface DeliveryFeedbackService {
    DeliveryFeedbackResponseDTO submitFeedback(Long orderId, String username, DeliveryFeedbackRequestDTO request);
    List<DeliveryFeedbackResponseDTO> getFeedbackForPartner(Long partnerId);
    DeliveryPartnerRatingSummaryDTO getRatingSummaryForPartner(Long partnerId);
    List<AdminDeliveryFeedbackResponseDTO> getAdminFeedbackForPartner(Long partnerId);
    List<DeliveryPartnerRatingSummaryDTO> getAllPartnerRatings();
    com.demoproject.shoppingcart.dto.DeliveryFeedbackStatusDTO getFeedbackStatus(Long orderId, String username);
}
