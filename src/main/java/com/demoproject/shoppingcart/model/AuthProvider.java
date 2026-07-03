package com.demoproject.shoppingcart.model;

/**
 * Identifies which authentication provider was used to create / last-link an account.
 * <ul>
 *   <li>{@code LOCAL} – traditional email + password registration.</li>
 *   <li>{@code GOOGLE} – authenticated via Google (OpenID Connect).</li>
 *   <li>{@code FACEBOOK} – authenticated via Facebook (OAuth2).</li>
 * </ul>
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    FACEBOOK
}
