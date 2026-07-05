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
 *   <li>Creating a long-lived refresh token (via {@link RefreshTokenService}) and
 *       storing it in a secure HttpOnly cookie (never exposed to JavaScript).</li>
 *   <li>Clearing the OAuth2 state cookie.</li>
 *   <li>Redirecting the browser to the configured frontend callback URL with
 *       only the access token as a query parameter.</li>
 * </ol>
 *
 * <h2>Frontend integration</h2>
 * The browser lands on: {@code <frontendRedirectUri>?accessToken=<jwt>}
 * The frontend should immediately read and store the access token, then clear the URL bar.
 * The refresh token is handled automatically via the HttpOnly cookie.
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    /** Name of the HttpOnly cookie that carries the refresh token — must match AuthController. */
    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 7; // 7 days

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepo;
    private final boolean isProduction;

    @Value("${app.oauth2.frontend-redirect-uri:http://localhost:4200/oauth2/callback}")
    private String frontendRedirectUri;

    public OAuth2AuthenticationSuccessHandler(
            JwtUtil jwtUtil,
            RefreshTokenService refreshTokenService,
            UserRepository userRepository,
            HttpCookieOAuth2AuthorizationRequestRepository cookieRepo,
            @Value("${app.production:false}") boolean isProduction) {
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.cookieRepo = cookieRepo;
        this.isProduction = isProduction;
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

        // Store refresh token in a secure HttpOnly cookie — never in the URL
        String cookieValue = String.format(
                "%s=%s; Max-Age=%d; Path=/; HttpOnly; %sSameSite=%s",
                REFRESH_COOKIE_NAME,
                refreshToken.getToken(),
                COOKIE_MAX_AGE_SECONDS,
                isProduction ? "Secure; " : "",
                isProduction ? "None" : "Strict"
        );
        response.addHeader("Set-Cookie", cookieValue);

        // Clean up the OAuth2 state cookie
        cookieRepo.removeAuthorizationRequest(request, response);

        // Build the frontend redirect URL — only the access token in the query param
        String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUri)
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        log.info("OAuth2 login successful for '{}' via {}. Redirecting to frontend.",
                appUser.getUserName(), appUser.getAuthProvider());

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
