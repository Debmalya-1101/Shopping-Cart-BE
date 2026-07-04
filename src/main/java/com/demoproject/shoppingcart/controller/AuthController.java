package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.*;
import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.DeliveryPartner;
import com.demoproject.shoppingcart.model.DeliveryPartnerStatus;
import com.demoproject.shoppingcart.model.RefreshToken;
import com.demoproject.shoppingcart.model.Role;
import com.demoproject.shoppingcart.repository.DeliveryPartnerRepository;
import com.demoproject.shoppingcart.repository.UserRepository;
import com.demoproject.shoppingcart.security.JwtUtil;
import com.demoproject.shoppingcart.service.DeliveryPartnerService;
import com.demoproject.shoppingcart.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Login, signup, token refresh and logout")
public class AuthController {

    /** Name of the HttpOnly cookie that carries the refresh token. */
    private static final String REFRESH_COOKIE_NAME = "refreshToken";

    /** Cookie max-age: must match the refresh token TTL (7 days = 604 800 s). */
    private static final int COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 * 7;

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeliveryPartnerService deliveryPartnerService;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final RefreshTokenService refreshTokenService;
    private final boolean isProduction;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          DeliveryPartnerService deliveryPartnerService,
                          DeliveryPartnerRepository deliveryPartnerRepository,
                          RefreshTokenService refreshTokenService,
                          @Value("${app.production:false}") boolean isProduction) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.deliveryPartnerService = deliveryPartnerService;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.refreshTokenService = refreshTokenService;
        this.isProduction = isProduction;
    }

    // -----------------------------------------------------------------------
    //  Shared cookie helpers
    // -----------------------------------------------------------------------

    /**
     * Appends an HttpOnly, Secure (in production) refresh-token cookie to the response.
     *
     * <p>SameSite=None is required when the frontend and backend are on different origins
     * (e.g., Vercel + Render). Secure=true is mandatory with SameSite=None.
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String tokenValue) {
        // Use the Set-Cookie header directly to control SameSite attribute,
        // which is not supported by the Servlet Cookie API.
        String cookieValue = String.format(
                "%s=%s; Max-Age=%d; Path=/; HttpOnly; %sSameSite=%s",
                REFRESH_COOKIE_NAME,
                tokenValue,
                COOKIE_MAX_AGE_SECONDS,
                isProduction ? "Secure; " : "",
                isProduction ? "None" : "Strict"
        );
        response.addHeader("Set-Cookie", cookieValue);
    }

    /**
     * Appends a clear (Max-Age=0) cookie to tell the browser to delete the refresh-token cookie.
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        String cookieValue = String.format(
                "%s=; Max-Age=0; Path=/; HttpOnly; %sSameSite=%s",
                REFRESH_COOKIE_NAME,
                isProduction ? "Secure; " : "",
                isProduction ? "None" : "Strict"
        );
        response.addHeader("Set-Cookie", cookieValue);
    }

    /**
     * Reads the refresh token string from the request's cookies.
     *
     * @return the token value, or {@code null} if not present
     */
    private String readRefreshTokenCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
    }

    // -----------------------------------------------------------------------
    //  Login
    // -----------------------------------------------------------------------

    @Operation(summary = "Login",
            description = "Authenticate with username/email and password. "
                    + "Returns a short-lived access token in the JSON body and a long-lived "
                    + "refresh token stored in a secure HttpOnly cookie.")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()
                )
        );

        // Authentication successful
        User principal = (User) authentication.getPrincipal();

        // Resolve role (e.g. "ROLE_USER")
        String role = principal.getAuthorities().stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_USER");

        // Block delivery partners who are not yet approved
        if ("ROLE_DELIVERY_PARTNER".equals(role)) {
            AppUser appUser = userRepository.findByUserName(principal.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            DeliveryPartner dp = deliveryPartnerRepository.findByUser(appUser)
                    .orElseThrow(() -> new RuntimeException("Delivery Partner not found"));

            if (dp.getStatus() == DeliveryPartnerStatus.PENDING) {
                return ResponseEntity.status(403).body("Your account is pending admin approval.");
            } else if (dp.getStatus() == DeliveryPartnerStatus.REJECTED) {
                return ResponseEntity.status(403).body("Your account registration has been rejected.");
            } else if (dp.getStatus() == DeliveryPartnerStatus.SUSPENDED) {
                return ResponseEntity.status(403).body("Your account has been suspended.");
            }
        }

        // Issue tokens
        String accessToken = jwtUtil.generateToken(principal.getUsername(), role);

        AppUser appUser = userRepository.findByUserName(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(appUser.getId());

        // Store refresh token in secure HttpOnly cookie (not in response body)
        addRefreshTokenCookie(response, refreshToken.getToken());

        // Return only the access token in the JSON body
        return ResponseEntity.ok(new AuthResponse(accessToken));
    }

    // -----------------------------------------------------------------------
    //  Sign-up
    // -----------------------------------------------------------------------

    @Operation(summary = "Sign up (customer)", description = "Register a new customer account.")
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {

        if (userRepository.findByEmailId(request.getEmailId()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already in use");
        }

        if (userRepository.findByUserName(request.getUserName()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already in use");
        }

        AppUser user = new AppUser();
        user.setEmailId(request.getEmailId());
        user.setUserName(request.getUserName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_USER);

        userRepository.save(user);

        return ResponseEntity.ok("AppUser registered successfully");
    }

    @Operation(summary = "Sign up (delivery partner)", description = "Register a new delivery partner account. Requires admin approval before login.")
    @PostMapping("/delivery-partner/signup")
    public ResponseEntity<String> registerDeliveryPartner(@RequestBody DeliveryPartnerSignupRequest request) {
        try {
            deliveryPartnerService.registerDeliveryPartner(request);
            return ResponseEntity.ok("Delivery Partner registered successfully. Pending admin approval.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    //  Refresh Token
    // -----------------------------------------------------------------------

    /**
     * Exchange a valid refresh token (read from the HttpOnly cookie) for a new
     * access token + rotated refresh token cookie.
     *
     * <p>Security guarantees:
     * <ul>
     *   <li>The old refresh token is immediately invalidated (revoked) after this call.</li>
     *   <li>A brand-new refresh token is issued (rotation) and set as a new cookie.</li>
     *   <li>If a previously-used (revoked) refresh token is presented, all active sessions for
     *       that user are terminated immediately (reuse-attack detection).</li>
     * </ul>
     */
    @Operation(summary = "Refresh access token",
            description = "Reads the refresh token from the secure HttpOnly cookie. "
                    + "Returns a new access token and rotates the refresh token cookie. "
                    + "Presenting an already-used token triggers reuse-attack detection and revokes ALL active sessions.")
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request,
                                          HttpServletResponse response) {

        String rawToken = readRefreshTokenCookie(request);

        if (rawToken == null || rawToken.isBlank()) {
            return ResponseEntity.status(401).body("No refresh token cookie present.");
        }

        // 1. Validate – throws TokenRefreshException on failure / reuse detection
        RefreshToken validated = refreshTokenService.validateRefreshToken(rawToken);

        // 2. Rotate – consume old token, issue new one
        RefreshToken rotated = refreshTokenService.rotateRefreshToken(validated);

        // 3. Issue a fresh access token for the same user
        AppUser user = validated.getUser();
        String newAccessToken = jwtUtil.generateToken(user.getUserName(), user.getRole().name());

        // 4. Replace the refresh-token cookie
        addRefreshTokenCookie(response, rotated.getToken());

        return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken));
    }

    // -----------------------------------------------------------------------
    //  Logout
    // -----------------------------------------------------------------------

    /**
     * Revoke all active refresh tokens for the currently authenticated user
     * and clear the refresh-token cookie.
     * The short-lived access token will continue to work until it naturally expires,
     * but no new access tokens can be obtained after this call.
     */
    @Operation(summary = "Logout",
            description = "Revoke all refresh tokens for the authenticated user and clear the HttpOnly cookie. "
                    + "The current access token remains valid until it expires (by design for stateless JWTs).")
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request,
                                         HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            // Still clear any stale cookie even if not authenticated
            clearRefreshTokenCookie(response);
            return ResponseEntity.status(401).body("Not authenticated");
        }

        String username = authentication.getName();
        AppUser user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        refreshTokenService.revokeAllTokensForUser(user.getId());

        // Clear the HttpOnly refresh token cookie from the browser
        clearRefreshTokenCookie(response);

        return ResponseEntity.ok("Logged out successfully. All sessions have been terminated.");
    }

    // -----------------------------------------------------------------------
    //  Current user info
    // -----------------------------------------------------------------------

    @Operation(summary = "Get authenticated user info")
    @GetMapping("/me")
    public ResponseEntity<AuthUserInfoDTO> getLoggedInUserInfo() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        AppUser user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found in DB"));

        AuthUserInfoDTO response = new AuthUserInfoDTO(
                user.getUserName(),
                user.getEmailId(),
                user.getRole().name()
        );

        return ResponseEntity.ok(response);
    }
}
