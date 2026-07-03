package com.demoproject.shoppingcart.oauth2.userinfo;

import java.util.Map;

/**
 * Extracts user info from Google's OpenID Connect (OIDC) token claims.
 *
 * <p>Google OIDC attribute keys:
 * <ul>
 *   <li>{@code sub}     – unique user identifier</li>
 *   <li>{@code name}    – full display name</li>
 *   <li>{@code email}   – verified email address</li>
 *   <li>{@code picture} – profile picture URL</li>
 * </ul>
 */
public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("picture");
    }
}
