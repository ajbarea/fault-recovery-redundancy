package com.swen755.fault_recovery_redundancy.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import org.springframework.context.annotation.Profile;

/**
 * Security configuration for the fault recovery application.
 * Excludes test profile to allow unrestricted access during testing.
 */
@Configuration
@EnableWebSecurity
@Profile("!test")
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    /**
     * Configures HTTP security filter chain with CORS support and endpoint
     * authorization.
     * Permits public access to health checks, authentication, and streaming
     * endpoints.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Enable CORS with custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints - no authentication required
                        .requestMatchers("/", "/health", "/health/**", "/heartbeat/**").permitAll()
                        .requestMatchers("/register", "/success", "/guide").permitAll()
                        .requestMatchers("/simulation/**").permitAll()
                        .requestMatchers("/api/stream/**").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/profile/**").permitAll()
                        // All other endpoints require authentication
                        .anyRequest().authenticated())
                // Enable HTTP Basic authentication
                .httpBasic(basic -> {
                })
                // Disable CSRF for API endpoints
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    /**
     * Provides BCrypt password encoder for secure password hashing.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
