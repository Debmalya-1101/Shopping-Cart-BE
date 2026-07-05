package com.demoproject.shoppingcart.oauth2.userinfo;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.util.Map;

/**
 * Factory that returns the correct {@link OAuth2UserInfo} implementation
 * based on the OAuth2 registration ID (i.e. the provider name).
 */
public class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {}

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId,
                                                   Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google"   -> new GoogleOAuth2UserInfo(attributes);
            case "facebook" -> new FacebookOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException(
                    new OAuth2Error("unsupported_provider"),
                    "Social login with provider '" + registrationId + "' is not supported.");
        };
    }
}
