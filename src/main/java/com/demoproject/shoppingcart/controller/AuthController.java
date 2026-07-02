package com.demoproject.shoppingcart.controller;

import com.demoproject.shoppingcart.dto.*;
import com.demoproject.shoppingcart.exception.TokenRefreshException;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "Login, signup, token refresh and logout")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DeliveryPartnerService deliveryPartnerService;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          DeliveryPartnerService deliveryPartnerService,
                          DeliveryPartnerRepository deliveryPartnerRepository,
                          RefreshTokenService refreshTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.deliveryPartnerService = deliveryPartnerService;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.refreshTokenService = refreshTokenService;
    }

    // -----------------------------------------------------------------------
    //  Login
    // -----------------------------------------------------------------------

    @Operation(summary = "Login",
            description = "Authenticate with username/email and password. Returns a short-lived access token and a long-lived refresh token.")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {

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

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken()));
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
     * Exchange a valid refresh token for a new access token + rotated refresh token.
     *
     * <p>Security guarantees:
     * <ul>
     *   <li>The old refresh token is immediately invalidated (revoked) after this call.</li>
     *   <li>A brand-new refresh token is issued (rotation).</li>
     *   <li>If a previously-used (revoked) refresh token is presented, all active sessions for
     *       that user are terminated immediately (reuse-attack detection).</li>
     * </ul>
     */
    @Operation(summary = "Refresh access token",
            description = "Submit a valid refresh token to obtain a new access token and a rotated refresh token. "
                    + "Presenting an already-used token triggers reuse-attack detection and revokes ALL active sessions.")
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@RequestBody TokenRefreshRequest request) {

        // 1. Validate – throws TokenRefreshException on failure / reuse detection
        RefreshToken validated = refreshTokenService.validateRefreshToken(request.getRefreshToken());

        // 2. Rotate – consume old token, issue new one
        RefreshToken rotated = refreshTokenService.rotateRefreshToken(validated);

        // 3. Issue a fresh access token for the same user
        AppUser user = validated.getUser();
        String newAccessToken = jwtUtil.generateToken(user.getUserName(), user.getRole().name());

        return ResponseEntity.ok(
                new TokenRefreshResponse(newAccessToken, rotated.getToken())
        );
    }

    // -----------------------------------------------------------------------
    //  Logout
    // -----------------------------------------------------------------------

    /**
     * Revoke all active refresh tokens for the currently authenticated user.
     * The short-lived access token will continue to work until it naturally expires,
     * but no new access tokens can be obtained after this call.
     */
    @Operation(summary = "Logout",
            description = "Revoke all refresh tokens for the authenticated user. "
                    + "The current access token remains valid until it expires (by design for stateless JWTs).")
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        String username = authentication.getName();
        AppUser user = userRepository.findByUserName(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        refreshTokenService.revokeAllTokensForUser(user.getId());
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
