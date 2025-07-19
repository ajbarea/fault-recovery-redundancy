package com.swen755.fault_recovery_redundancy.controller;

import com.swen755.fault_recovery_redundancy.dto.LoginRequest;
import com.swen755.fault_recovery_redundancy.dto.RegisterRequest;
import com.swen755.fault_recovery_redundancy.entity.User;
import com.swen755.fault_recovery_redundancy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, BindingResult bindingResult) {
        if (!request.password.equals(request.confirmPassword)) {
            return ResponseEntity.badRequest().body("Passwords do not match");
        }
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(errors);
        }
        try {
            User user = userService.register(request);
            return ResponseEntity.ok(Map.of(
                    "message", "Registration successful",
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "streamKey", user.getStreamKey(),
                    "serverUrl", "rtmp://localhost:1935/live"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "suggestion", "User already exists. Try logging in instead."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Registration failed",
                    "details", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Internal server error",
                    "details", "Please try again later"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errors = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest().body(errors);
        }
        
        Optional<User> user = userService.login(request.username, request.password);
        if (user.isPresent()) {
            User authenticatedUser = user.get();
            return ResponseEntity.ok(Map.of(
                    "message", "Login successful",
                    "username", authenticatedUser.getUsername(),
                    "email", authenticatedUser.getEmail(),
                    "streamKey", authenticatedUser.getStreamKey(),
                    "serverUrl", "rtmp://localhost:1935/live",
                    "streamUrl", "http://localhost:9090/live/stream_" + authenticatedUser.getUsername() + "/index.m3u8"));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid username or password"));
        }
    }

    @GetMapping("/profile/{username}")
    public ResponseEntity<?> getProfile(@PathVariable String username) {
        Optional<User> user = userService.findByUsername(username);
        if (user.isPresent()) {
            User foundUser = user.get();
            return ResponseEntity.ok(Map.of(
                    "username", foundUser.getUsername(),
                    "email", foundUser.getEmail(),
                    "streamKey", foundUser.getStreamKey(),
                    "serverUrl", "rtmp://localhost:1935/live",
                    "streamUrl", "http://localhost:9090/live/stream_" + foundUser.getUsername() + "/index.m3u8"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
