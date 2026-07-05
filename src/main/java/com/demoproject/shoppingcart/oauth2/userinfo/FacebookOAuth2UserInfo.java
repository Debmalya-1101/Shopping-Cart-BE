package com.demoproject.shoppingcart.oauth2.userinfo;

import java.util.Map;

/**
 * Extracts user info from Facebook's Graph API response.
 *
 * <p>The user-info-uri is configured to request {@code fields=id,name,email,picture}
 * (see application.properties). Facebook attribute keys:
 * <ul>
 *   <li>{@code id}      – unique numeric user ID</li>
 *   <li>{@code name}    – full display name</li>
 *   <li>{@code email}   – email address (only present if the user granted the
 *                         {@code email} permission)</li>
 *   <li>{@code picture} – nested object: {@code { data: { url: "...", ... } }}</li>
 * </ul>
 */
public class FacebookOAuth2UserInfo extends OAuth2UserInfo {

    public FacebookOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    /**
     * Facebook returns picture as a nested map:
     * {@code { "data": { "url": "https://...", ... } }}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public String getImageUrl() {
        Object pictureObj = attributes.get("picture");
        if (pictureObj instanceof Map) {
            Map<String, Object> pictureMap = (Map<String, Object>) pictureObj;
            Object dataObj = pictureMap.get("data");
            if (dataObj instanceof Map) {
                Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                return (String) dataMap.get("url");
            }
        }
        return null;
    }
}
