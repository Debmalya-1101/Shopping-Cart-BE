package com.demoproject.shoppingcart.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Returned on a successful login.
 * Contains the short-lived access token only.
 *
 * <p>The long-lived refresh token is no longer returned in the response body.
 * It is stored securely in an HttpOnly cookie by the server,
 * making it inaccessible to JavaScript and protected from XSS attacks.
 */
@Getter
@Setter
public class AuthResponse {

    /** Short-lived JWT used to authorize API calls. */
    private String accessToken;

    private String tokenType = "Bearer";

    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
