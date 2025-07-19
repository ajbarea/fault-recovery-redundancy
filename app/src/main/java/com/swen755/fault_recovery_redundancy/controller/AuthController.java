package com.swen755.fault_recovery_redundancy.controller;

import com.swen755.fault_recovery_redundancy.dto.LoginRequest;
import com.swen755.fault_recovery_redundancy.dto.RegisterRequest;
import com.swen755.fault_recovery_redundancy.entity.User;
import com.swen755.fault_recovery_redundancy.service.UserService;
import com.swen755.fault_recovery_redundancy.service.UserService.RegisterOrLoginResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for user authentication operations.
 * Handles user registration, login, and profile retrieval.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Registers a new user or logs in an existing one with the same credentials.
     * Returns streaming configuration details upon success.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, BindingResult bindingResult) {
        // Validate password confirmation
        if (!request.password.equals(request.confirmPassword)) {
            return ResponseEntity.badRequest().body("Passwords do not match");
        }

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(errors);
        }
        try {
            // Attempt registration or login with provided credentials
            RegisterOrLoginResult result = userService.registerOrLogin(request);
            User user = result.getUser();

            // Return success response with streaming configuration
            return ResponseEntity.ok(Map.of(
                    "message", result.getMessage(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "streamKey", user.getStreamKey(),
                    "serverUrl", "rtmp://localhost:1935/live",
                    "streamUrl", "http://localhost:9090/live/stream_" + user.getUsername() + "/index.m3u8",
                    "wasRegistered", result.wasRegistered(),
                    "wasLoggedIn", result.wasLoggedIn()));
        } catch (IllegalArgumentException e) {
            // Handle validation errors with helpful suggestions
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "suggestion", "Please check your credentials and try again."));
        } catch (RuntimeException e) {
            // Handle registration-specific errors
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Registration failed",
                    "details", e.getMessage()));
        } catch (Exception e) {
            // Handle unexpected errors
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Internal server error",
                    "details", "Please try again later"));
        }
    }

    /**
     * Authenticates an existing user with username and password.
     * Returns streaming configuration details upon successful login.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, BindingResult bindingResult) {
        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(errors);
        }

        // Attempt user authentication
        Optional<User> user = userService.login(request.username, request.password);
        if (user.isPresent()) {
            User authenticatedUser = user.get();
            // Return success response with streaming configuration
            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "username", authenticatedUser.getUsername(),
                    "email", authenticatedUser.getEmail(),
                    "streamKey", authenticatedUser.getStreamKey(),
                    "serverUrl", "rtmp://localhost:1935/live",
                    "streamUrl",
                    "http://localhost:9090/live/stream_" + authenticatedUser.getUsername() + "/index.m3u8"));
        } else {
            // Return error for invalid credentials
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid username or password"));
        }
    }

    /**
     * Retrieves public profile information for a user by username.
     * Includes streaming configuration details.
     */
    @GetMapping("/profile/{username}")
    public ResponseEntity<?> getProfile(@PathVariable String username) {
        Optional<User> user = userService.findByUsername(username);
        if (user.isPresent()) {
            User foundUser = user.get();
            // Return user profile with streaming configuration
            return ResponseEntity.ok(Map.of(
                    "username", foundUser.getUsername(),
                    "email", foundUser.getEmail(),
                    "streamKey", foundUser.getStreamKey(),
                    "serverUrl", "rtmp://localhost:1935/live",
                    "streamUrl", "http://localhost:9090/live/stream_" + foundUser.getUsername() + "/index.m3u8"));
        } else {
            // Return 404 if user not found
            return ResponseEntity.notFound().build();
        }
    }
}
