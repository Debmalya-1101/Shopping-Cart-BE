package com.demoproject.shoppingcart.oauth2;

import com.demoproject.shoppingcart.oauth2.userinfo.OAuth2UserInfo;
import com.demoproject.shoppingcart.oauth2.userinfo.OAuth2UserInfoFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom OIDC user service for <b>Google</b> (which uses OpenID Connect, a superset of OAuth2).
 *
 * <p>Spring Security routes Google logins through {@link OidcUserService} rather than
 * {@link org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService}.
 * This class wraps the standard service and delegates the find-or-create user logic to
 * {@link CustomOAuth2UserService#processOAuth2User}, keeping the logic in one place.
 */
@Service
public class CustomOidcUserService extends OidcUserService {

    private final CustomOAuth2UserService customOAuth2UserService;

    public CustomOidcUserService(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // Let Spring Security handle the OIDC token validation and user-info fetch
        OidcUser oidcUser = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // "google"
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId, oidcUser.getAttributes());

        // Persist / update local AppUser record
        customOAuth2UserService.processOAuth2User(registrationId, userInfo);

        return oidcUser;
    }
}
