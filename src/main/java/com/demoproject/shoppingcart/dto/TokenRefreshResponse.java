package com.demoproject.shoppingcart.dto;

import lombok.Getter;

/** Returned by /auth/refresh – contains the new access token and the rotated refresh token. */
@Getter
public class TokenRefreshResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType = "Bearer";

    public TokenRefreshResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
