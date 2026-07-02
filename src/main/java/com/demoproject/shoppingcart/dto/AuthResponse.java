package com.demoproject.shoppingcart.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Returned on a successful login.
 * Contains both the short-lived access token and the long-lived refresh token.
 */
@Getter
@Setter
public class AuthResponse {

    /** Short-lived JWT used to authorize API calls. */
    private String accessToken;

    /**
     * Long-lived opaque token used to obtain a new access token without re-logging in.
     * Store this securely (e.g., HttpOnly cookie or secure storage) on the client.
     */
    private String refreshToken;

    private String tokenType = "Bearer";

    public AuthResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
