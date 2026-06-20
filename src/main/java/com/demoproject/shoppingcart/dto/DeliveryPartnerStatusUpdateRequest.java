package com.demoproject.shoppingcart.dto;

import com.demoproject.shoppingcart.model.DeliveryPartnerStatus;
import lombok.Data;

@Data
public class DeliveryPartnerStatusUpdateRequest {
    private DeliveryPartnerStatus status;
}
