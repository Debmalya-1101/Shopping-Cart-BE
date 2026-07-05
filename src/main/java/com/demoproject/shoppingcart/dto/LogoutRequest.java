package com.demoproject.shoppingcart.dto;

import lombok.Getter;

/** Sent by the client to the /auth/logout endpoint. */
@Getter
public class LogoutRequest {

    private String refreshToken;
}
