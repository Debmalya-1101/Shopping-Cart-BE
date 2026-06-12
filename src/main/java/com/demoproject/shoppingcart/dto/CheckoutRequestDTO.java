package com.demoproject.shoppingcart.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequestDTO {
    private String name;
    private Long phoneNo;
    private String email;
    private String address;
    private Long addressId;
}

