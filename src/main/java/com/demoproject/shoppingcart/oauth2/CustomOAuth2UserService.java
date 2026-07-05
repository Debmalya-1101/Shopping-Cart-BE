package com.demoproject.shoppingcart.oauth2;

import com.demoproject.shoppingcart.model.AppUser;
import com.demoproject.shoppingcart.model.AuthProvider;
import com.demoproject.shoppingcart.model.Role;
import com.demoproject.shoppingcart.oauth2.userinfo.OAuth2UserInfo;
import com.demoproject.shoppingcart.oauth2.userinfo.OAuth2UserInfoFactory;
import com.demoproject.shoppingcart.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Custom OAuth2 user service used for <b>non-OIDC providers</b> (currently Facebook).
 *
 * <p>After Spring Security exchanges the authorization code for an access token and fetches
 * the user-info from the provider, this service is called to load (or create) the local
 * {@link AppUser} record.
 *
 * <h2>Account linking policy</h2>
 * <ol>
 *   <li>Look up existing user by <b>providerId + authProvider</b> (fastest path – returning user).</li>
 *   <li>If not found, look up by <b>email</b> (account linking – user previously registered
 *       with email/password using the same address).</li>
 *   <li>If still not found, <b>create a new user</b> with the provider's details.</li>
 * </ol>
 *
 * <p>The {@link #processOAuth2User} method is also called by {@link CustomOidcUserService}
 * for Google logins, keeping the find-or-create logic in a single place.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(UserRepository userRepository,
                                   PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId, oAuth2User.getAttributes());

        processOAuth2User(registrationId, userInfo);

        return oAuth2User;
    }

    /**
     * Core find-or-create logic shared with {@link CustomOidcUserService}.
     * Persists the local {@link AppUser} record and returns it.
     *
     * @param registrationId provider name (e.g. "google", "facebook")
     * @param userInfo       normalised user info from the provider
     */
    @Transactional
    public AppUser processOAuth2User(String registrationId, OAuth2UserInfo userInfo) {
        if (userInfo.getEmail() == null || userInfo.getEmail().isBlank()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_found"),
                    "Email address not returned by " + registrationId
                            + ". Please ensure the email permission is granted.");
        }

        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        // 1. Look up by provider-issued ID (fastest path – existing OAuth2 user returning)
        Optional<AppUser> byProviderId = userRepository
                .findByProviderIdAndAuthProvider(userInfo.getId(), provider);

        if (byProviderId.isPresent()) {
            return updateExistingUser(byProviderId.get(), userInfo);
        }

        // 2. Look up by email (account linking – email/password user logging in via social)
        Optional<AppUser> byEmail = userRepository.findByEmailId(userInfo.getEmail());
        if (byEmail.isPresent()) {
            AppUser existingUser = byEmail.get();
            log.info("Linking {} account to existing user '{}'", provider, existingUser.getUserName());
            existingUser.setAuthProvider(provider);
            existingUser.setProviderId(userInfo.getId());
            existingUser.setDisplayName(userInfo.getName());
            existingUser.setAvatarUrl(userInfo.getImageUrl());
            return userRepository.save(existingUser);
        }

        // 3. Brand-new user — create a local record
        return registerNewOAuth2User(provider, userInfo);
    }

    // -----------------------------------------------------------------------
    //  Private helpers
    // -----------------------------------------------------------------------

    private AppUser updateExistingUser(AppUser user, OAuth2UserInfo userInfo) {
        // Refresh display name and avatar in case they changed on the provider side
        user.setDisplayName(userInfo.getName());
        user.setAvatarUrl(userInfo.getImageUrl());
        return userRepository.save(user);
    }

    private AppUser registerNewOAuth2User(AuthProvider provider, OAuth2UserInfo userInfo) {
        AppUser user = new AppUser();
        user.setEmailId(userInfo.getEmail());
        user.setUserName(deriveUniqueUsername(userInfo.getEmail()));
        user.setDisplayName(userInfo.getName());
        user.setAvatarUrl(userInfo.getImageUrl());
        user.setAuthProvider(provider);
        user.setProviderId(userInfo.getId());
        user.setRole(Role.ROLE_USER);

        // OAuth2 users have no password. We store an unguessable random hash so that:
        // (a) the password column stays NOT NULL, and (b) password-based login is
        // impossible without knowing the random value (which nobody does).
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        log.info("Registering new {} user: {}", provider, user.getEmailId());
        return userRepository.save(user);
    }

    /**
     * Derives a URL-friendly, unique username from the user's email address.
     * Uses the local part (before @) and appends a short random suffix if
     * the candidate username is already taken.
     */
    private String deriveUniqueUsername(String email) {
        String base = email.split("@")[0]
                .replaceAll("[^a-zA-Z0-9_]", "_")
                .toLowerCase();

        String candidate = base;
        int attempts = 0;
        while (userRepository.findByUserName(candidate).isPresent() && attempts < 10) {
            candidate = base + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
            attempts++;
        }
        return candidate;
    }
}
