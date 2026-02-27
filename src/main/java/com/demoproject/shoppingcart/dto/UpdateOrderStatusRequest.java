package com.demoproject.shoppingcart.dto;

import com.demoproject.shoppingcart.model.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderStatusRequest {
    private OrderStatus status;
}

