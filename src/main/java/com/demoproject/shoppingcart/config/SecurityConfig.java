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
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

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

    /**
     * Global CORS policy. Required because we use HttpOnly cookies for the refresh token,
     * and browsers refuse to send credentials (cookies) to origins configured with "*".
     *
     * <p>Only the two known frontend origins are whitelisted:
     * <ul>
     *   <li>Local development: http://localhost:4200</li>
     *   <li>Production (Vercel): https://nexis-store-sigma.vercel.app</li>
     * </ul>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",
                "https://nexis-store-sigma.vercel.app"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        configuration.setAllowedHeaders(List.of("*"));

        // Allow the browser to send the HttpOnly refresh-token cookie
        configuration.setAllowCredentials(true);

        // Cache pre-flight response for 1 hour
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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
                            "/api/products/**",
                            "/api/recommendations/**",
                            "/api/home/**",
                            "/api/faqs/**",
                            // Swagger / OpenAPI documentation
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html"
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
