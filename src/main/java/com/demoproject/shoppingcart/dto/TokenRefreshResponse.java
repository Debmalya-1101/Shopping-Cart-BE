package com.demoproject.shoppingcart.dto;

import lombok.Getter;

/**
 * Returned by /auth/refresh.
 * Contains only the new access token.
 *
 * <p>The rotated refresh token is no longer returned in the response body.
 * It is stored in a new HttpOnly cookie by the server.
 */
@Getter
public class TokenRefreshResponse {

    private final String accessToken;
    private final String tokenType = "Bearer";

    public TokenRefreshResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
