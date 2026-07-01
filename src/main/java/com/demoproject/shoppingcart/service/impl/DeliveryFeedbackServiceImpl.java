package com.demoproject.shoppingcart.service.impl;

import com.demoproject.shoppingcart.dto.AdminDeliveryFeedbackResponseDTO;
import com.demoproject.shoppingcart.dto.DeliveryFeedbackRequestDTO;
import com.demoproject.shoppingcart.dto.DeliveryFeedbackResponseDTO;
import com.demoproject.shoppingcart.dto.DeliveryPartnerRatingSummaryDTO;
import com.demoproject.shoppingcart.model.*;
import com.demoproject.shoppingcart.repository.*;
import com.demoproject.shoppingcart.service.DeliveryFeedbackService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DeliveryFeedbackServiceImpl implements DeliveryFeedbackService {

    private final DeliveryFeedbackRepository deliveryFeedbackRepository;
    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;

    public DeliveryFeedbackServiceImpl(DeliveryFeedbackRepository deliveryFeedbackRepository,
                                       OrderRepository orderRepository,
                                       ShipmentRepository shipmentRepository,
                                       UserRepository userRepository,
                                       DeliveryPartnerRepository deliveryPartnerRepository) {
        this.deliveryFeedbackRepository = deliveryFeedbackRepository;
        this.orderRepository = orderRepository;
        this.shipmentRepository = shipmentRepository;
        this.userRepository = userRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
    }

    @Override
    @Transactional
    public DeliveryFeedbackResponseDTO submitFeedback(Long orderId, String username, DeliveryFeedbackRequestDTO request) {
        AppUser customer = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(customer.getId())) {
            throw new RuntimeException("Unauthorized: You can only rate your own orders");
        }

        Shipment shipment = shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Shipment not found for this order"));

        if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
            throw new IllegalStateException("Feedback can only be submitted after the order has been DELIVERED.");
        }

        if (deliveryFeedbackRepository.findByOrderId(orderId).isPresent()) {
            throw new IllegalStateException("Feedback has already been submitted for this order.");
        }

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        DeliveryPartner partner = shipment.getDeliveryPartner();
        if (partner == null) {
            throw new IllegalStateException("No delivery partner was assigned to this shipment.");
        }

        DeliveryFeedback feedback = new DeliveryFeedback();
        feedback.setOrder(order);
        feedback.setShipment(shipment);
        feedback.setCustomer(customer);
        feedback.setDeliveryPartner(partner);
        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());

        feedback = deliveryFeedbackRepository.save(feedback);

        return new DeliveryFeedbackResponseDTO(
                feedback.getId(),
                feedback.getOrder().getId(),
                feedback.getRating(),
                feedback.getComment(),
                feedback.getCreatedAt()
        );
    }

    @Override
    public List<DeliveryFeedbackResponseDTO> getFeedbackForPartner(Long partnerId) {
        return deliveryFeedbackRepository.findByDeliveryPartnerId(partnerId).stream()
                .map(f -> new DeliveryFeedbackResponseDTO(
                        f.getId(),
                        f.getOrder().getId(),
                        f.getRating(),
                        f.getComment(),
                        f.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public DeliveryPartnerRatingSummaryDTO getRatingSummaryForPartner(Long partnerId) {
        Double avgRating = deliveryFeedbackRepository.getAverageRatingForPartner(partnerId);
        Long totalReviews = deliveryFeedbackRepository.countFeedbackForPartner(partnerId);

        return new DeliveryPartnerRatingSummaryDTO(
                partnerId,
                avgRating != null ? avgRating : 0.0,
                totalReviews != null ? totalReviews : 0L
        );
    }

    @Override
    public List<AdminDeliveryFeedbackResponseDTO> getAdminFeedbackForPartner(Long partnerId) {
        return deliveryFeedbackRepository.findByDeliveryPartnerId(partnerId).stream()
                .map(f -> new AdminDeliveryFeedbackResponseDTO(
                        f.getId(),
                        f.getOrder().getId(),
                        f.getCustomer().getId(),
                        f.getCustomer().getUserName(),
                        f.getCustomer().getEmailId(),
                        f.getDeliveryPartner().getId(),
                        f.getDeliveryPartner().getFullName(),
                        f.getRating(),
                        f.getComment(),
                        f.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<DeliveryPartnerRatingSummaryDTO> getAllPartnerRatings() {
        List<Object[]> results = deliveryFeedbackRepository.getAllPartnerRatingSummaries();
        List<DeliveryPartnerRatingSummaryDTO> summaries = new ArrayList<>();

        for (Object[] row : results) {
            Long partnerId = (Long) row[0];
            Double avgRating = (Double) row[1];
            Long totalReviews = (Long) row[2];

            summaries.add(new DeliveryPartnerRatingSummaryDTO(
                    partnerId,
                    avgRating != null ? avgRating : 0.0,
                    totalReviews != null ? totalReviews : 0L
            ));
        }

        // Include partners that have no ratings yet
        List<Long> ratedPartnerIds = summaries.stream().map(DeliveryPartnerRatingSummaryDTO::getDeliveryPartnerId).collect(Collectors.toList());
        List<DeliveryPartner> allPartners = deliveryPartnerRepository.findAll();
        for (DeliveryPartner partner : allPartners) {
            if (!ratedPartnerIds.contains(partner.getId())) {
                summaries.add(new DeliveryPartnerRatingSummaryDTO(partner.getId(), 0.0, 0L));
            }
        }

        return summaries;
    }

    @Override
    @Transactional(readOnly = true)
    public com.demoproject.shoppingcart.dto.DeliveryFeedbackStatusDTO getFeedbackStatus(Long orderId, String username) {
        AppUser customer = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getUser().getId().equals(customer.getId())) {
            throw new RuntimeException("Unauthorized: You can only view feedback status for your own orders");
        }

        java.util.Optional<DeliveryFeedback> feedbackOpt = deliveryFeedbackRepository.findByOrderId(orderId);
        
        if (feedbackOpt.isPresent()) {
            DeliveryFeedback feedback = feedbackOpt.get();
            return new com.demoproject.shoppingcart.dto.DeliveryFeedbackStatusDTO(
                    true,
                    feedback.getRating(),
                    feedback.getComment()
            );
        } else {
            return new com.demoproject.shoppingcart.dto.DeliveryFeedbackStatusDTO(false, null, null);
        }
    }
}
