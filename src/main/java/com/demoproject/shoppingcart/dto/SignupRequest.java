package com.demoproject.shoppingcart.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {
    private String emailId;
    private String userName;
    private String password;
}
