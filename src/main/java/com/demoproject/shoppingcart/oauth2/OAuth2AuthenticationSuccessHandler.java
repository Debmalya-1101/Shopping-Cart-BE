package com.demoproject.shoppingcart.oauth2;

import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.RefreshToken;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.security.JwtUtil;
import com.demoproject.shoppingcart.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Handles a successful OAuth2 authentication by:
 * <ol>
 *   <li>Resolving the local {@link AppUser} from the authenticated principal's email.</li>
 *   <li>Issuing a short-lived JWT access token (via {@link JwtUtil}).</li>
 *   <li>Creating a long-lived refresh token (via {@link RefreshTokenService}).</li>
 *   <li>Clearing the OAuth2 state cookie.</li>
 *   <li>Redirecting the browser to the configured frontend callback URL with
 *       both tokens as query parameters.</li>
 * </ol>
 *
 * <h2>Frontend integration</h2>
 * The browser lands on: {@code <frontendRedirectUri>?accessToken=<jwt>&refreshToken=<opaqueToken>}
 * The frontend should immediately read and securely store both tokens, then clear the URL bar.
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepo;

    @Value("${app.oauth2.frontend-redirect-uri:http://localhost:3000/oauth2/callback}")
    private String frontendRedirectUri;

    public OAuth2AuthenticationSuccessHandler(
            JwtUtil jwtUtil,
            RefreshTokenService refreshTokenService,
            UserRepository userRepository,
            HttpCookieOAuth2AuthorizationRequestRepository cookieRepo) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.cookieRepo = cookieRepo;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Email is the reliable cross-provider identifier we use to look up AppUser
        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            log.error("OAuth2 success but no email returned by provider. Cannot issue tokens.");
            cookieRepo.removeAuthorizationRequest(request, response);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Email not provided by OAuth2 provider.");
            return;
        }

        AppUser appUser = userRepository.findByEmailId(email)
                .orElseThrow(() -> new IllegalStateException(
                        "User not found after OAuth2 login for email: " + email));

        // Issue tokens
        String accessToken  = jwtUtil.generateToken(appUser.getUserName(), appUser.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(appUser.getId());

        // Clean up the OAuth2 state cookie
        cookieRepo.removeAuthorizationRequest(request, response);

        // Build the frontend redirect URL (tokens passed as query params)
        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken.getToken())
                .build().toUriString();

        log.info("OAuth2 login successful for '{}' via {}. Redirecting to frontend.",
                appUser.getUserName(), appUser.getAuthProvider());

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
