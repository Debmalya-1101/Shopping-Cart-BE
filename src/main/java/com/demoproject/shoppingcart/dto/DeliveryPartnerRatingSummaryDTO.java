package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPartnerRatingSummaryDTO {
    private Long deliveryPartnerId;
    private Double averageRating;
    private Long totalReviews;
}
