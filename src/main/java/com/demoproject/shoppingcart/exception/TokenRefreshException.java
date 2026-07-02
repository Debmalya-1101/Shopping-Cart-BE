package com.demoproject.shoppingcart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a refresh token is:
 * <ul>
 *   <li>Not found in the database</li>
 *   <li>Expired</li>
 *   <li>Already revoked (possible reuse attack)</li>
 * </ul>
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenRefreshException extends RuntimeException {

    public TokenRefreshException(String token, String reason) {
        super(String.format("Refresh token rejected [%s]: %s", token, reason));
    }
}
