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

/**
 * Security configuration class that defines the application's web security
 * settings.
 * 
 * This configuration sets up Spring Security to:
 * - Allow public access to health check endpoints, simulation endpoints,
 * streaming API, and user registration
 * - Require authentication for all other requests
 * - Use HTTP Basic authentication
 * - Disable CSRF protection
 * - Configure BCrypt password encoding for secure password storage
 * 
 * The security filter chain permits unrestricted access to:
 * - Root path ("/")
 * - Health check endpoints ("/health", "/health/**")
 * - Heartbeat endpoints ("/heartbeat/**")
 * - Simulation endpoints ("/simulation/**")
 * - Stream API endpoints ("/api/stream/**")
 * - User registration endpoint ("/api/auth/register")
 * 
 * All other endpoints require user authentication via HTTP Basic Auth.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/", "/health", "/health/**", "/heartbeat/**").permitAll()
                        .requestMatchers("/register", "/success", "/guide").permitAll()
                        .requestMatchers("/simulation/**").permitAll()
                        .requestMatchers("/api/stream/**").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/profile/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(basic -> {
                })
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
