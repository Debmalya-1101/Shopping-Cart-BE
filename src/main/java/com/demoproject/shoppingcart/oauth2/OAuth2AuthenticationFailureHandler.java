package com.demoproject.shoppingcart.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handles an OAuth2 authentication failure by clearing the state cookie and redirecting
 * the browser to the frontend with an {@code error} query parameter so the frontend
 * can display a meaningful message.
 *
 * <p>Example redirect: {@code http://localhost:3000/oauth2/callback?error=access_denied}
 */
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepo;

    @Value("${app.oauth2.frontend-redirect-uri:http://localhost:3000/oauth2/callback}")
    private String frontendRedirectUri;

    public OAuth2AuthenticationFailureHandler(
            HttpCookieOAuth2AuthorizationRequestRepository cookieRepo) {
        this.cookieRepo = cookieRepo;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("OAuth2 authentication failed: {}", exception.getMessage());

        // Clean up the OAuth2 state cookie
        cookieRepo.removeAuthorizationRequest(request, response);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("error", exception.getLocalizedMessage())
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
