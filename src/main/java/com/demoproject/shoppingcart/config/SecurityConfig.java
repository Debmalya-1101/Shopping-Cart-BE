package com.demoproject.shoppingcart.config;

import com.demoproject.shoppingcart.oauth2.CustomOAuth2UserService;
import com.demoproject.shoppingcart.oauth2.CustomOidcUserService;
import com.demoproject.shoppingcart.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.demoproject.shoppingcart.oauth2.OAuth2AuthenticationFailureHandler;
import com.demoproject.shoppingcart.oauth2.OAuth2AuthenticationSuccessHandler;
import com.demoproject.shoppingcart.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthRequestRepository;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomOAuth2UserService customOAuth2UserService,
            CustomOidcUserService customOidcUserService,
            OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler,
            OAuth2AuthenticationFailureHandler oAuth2FailureHandler,
            HttpCookieOAuth2AuthorizationRequestRepository cookieAuthRequestRepository) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.customOAuth2UserService = customOAuth2UserService;
        this.customOidcUserService = customOidcUserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.oAuth2FailureHandler = oAuth2FailureHandler;
        this.cookieAuthRequestRepository = cookieAuthRequestRepository;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                    // Stateless JWT API. The OAuth2 state is kept in a cookie
                    // via HttpCookieOAuth2AuthorizationRequestRepository, not in a session.
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                    // ── Public endpoints ──────────────────────────────────────────
                    .requestMatchers(
                            "/auth/**",
                            // OAuth2 initiation  : GET  /oauth2/authorization/{provider}
                            "/oauth2/**",
                            // OAuth2 callback    : GET  /login/oauth2/code/{provider}
                            "/login/oauth2/**",
                            "/api/products/**"
                    ).permitAll()

                    // ── Admin endpoints ───────────────────────────────────────────
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")

                    // ── Everything else requires authentication ───────────────────
                    .anyRequest().authenticated()
            )

            // ── OAuth2 Social Login ───────────────────────────────────────────────
            .oauth2Login(oauth2 -> oauth2
                    .authorizationEndpoint(endpoint -> endpoint
                            // Store the OAuth2 state in a cookie instead of the session
                            .authorizationRequestRepository(cookieAuthRequestRepository)
                    )
                    .userInfoEndpoint(userInfo -> userInfo
                            // Non-OIDC providers (Facebook)
                            .userService(customOAuth2UserService)
                            // OIDC providers (Google)
                            .oidcUserService(customOidcUserService)
                    )
                    .successHandler(oAuth2SuccessHandler)
                    .failureHandler(oAuth2FailureHandler)
            );

        // JWT filter runs before the standard username/password filter
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
