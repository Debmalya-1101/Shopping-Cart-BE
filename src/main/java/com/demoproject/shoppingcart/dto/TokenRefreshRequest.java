package com.demoproject.shoppingcart.dto;

import lombok.Getter;
import lombok.Setter;

/** Sent by the client to request a new access token using an existing refresh token. */
@Getter
@Setter
public class TokenRefreshRequest {

    private String refreshToken;
}
