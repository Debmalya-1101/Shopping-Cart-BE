package com.demoproject.shoppingcart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Isolated configuration for the {@link PasswordEncoder} bean.
 *
 * <p>Extracted from {@link SecurityConfig} to break the circular dependency:
 * <pre>
 *   SecurityConfig  →  CustomOAuth2UserService  →  PasswordEncoder  →  SecurityConfig (cycle!)
 * </pre>
 * With this class, the dependency graph becomes acyclic:
 * <pre>
 *   PasswordEncoderConfig  (no deps on other app beans)
 *         ↓
 *   CustomOAuth2UserService
 *         ↓
 *   SecurityConfig
 * </pre>
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
