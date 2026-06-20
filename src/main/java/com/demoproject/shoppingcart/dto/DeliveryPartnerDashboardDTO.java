package com.demoproject.shoppingcart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPartnerDashboardDTO {
    private long totalAssigned;
    private long totalPickedUp;
    private long totalOutForDelivery;
    private long totalDelivered;
    private long totalFailed;
}
