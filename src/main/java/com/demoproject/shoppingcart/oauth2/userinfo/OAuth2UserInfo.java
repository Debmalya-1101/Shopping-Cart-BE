package com.demoproject.shoppingcart.oauth2.userinfo;

import java.util.Map;

/**
 * Normalises the user-info attributes returned by different OAuth2 providers into a
 * common interface. Each provider returns slightly different JSON field names.
 */
public abstract class OAuth2UserInfo {

    protected final Map<String, Object> attributes;

    protected OAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    /** Raw attributes map as returned by the provider. */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /** The provider-issued unique identifier for this user (e.g. Google's {@code sub}). */
    public abstract String getId();

    /** The user's full display name. */
    public abstract String getName();

    /**
     * The user's verified email address.
     * May be {@code null} if the user declined to share it (rare but possible with Facebook).
     */
    public abstract String getEmail();

    /** URL of the user's profile picture, or {@code null} if unavailable. */
    public abstract String getImageUrl();
}
