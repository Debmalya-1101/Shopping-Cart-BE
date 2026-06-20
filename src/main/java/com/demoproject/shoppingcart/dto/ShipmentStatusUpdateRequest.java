package com.demoproject.shoppingcart.dto;

import com.demoproject.shoppingcart.model.ShipmentStatus;
import lombok.Data;

@Data
public class ShipmentStatusUpdateRequest {
    private ShipmentStatus status;
    private String failureReason;
}
