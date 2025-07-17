//package com.swen755.authentication_and_authorization.controller;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * REST Controller demonstrating authentication and authorization endpoints
// */
//@RestController
//@RequestMapping("/api")
//public class SecurityController {
//
//    /**
//     * Public endpoint - accessible to everyone without authentication
//     */
//    @GetMapping("/public")
//    public ResponseEntity<Map<String, String>> publicEndpoint() {
//        Map<String, String> response = new HashMap<>();
//        response.put("message", "This is a public endpoint accessible to everyone");
//        response.put("access", "No authentication required");
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * User endpoint - requires USER or ADMIN role
//     */
//    @GetMapping("/user")
//    public ResponseEntity<Map<String, String>> userEndpoint(Authentication authentication) {
//        Map<String, String> response = new HashMap<>();
//        response.put("message", "This is a user endpoint");
//        response.put("access", "Requires USER or ADMIN role");
//        response.put("authenticatedUser", authentication.getName());
//        response.put("authorities", authentication.getAuthorities().toString());
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Admin endpoint - requires ADMIN role only
//     */
//    @GetMapping("/admin")
//    public ResponseEntity<Map<String, String>> adminEndpoint(Authentication authentication) {
//        Map<String, String> response = new HashMap<>();
//        response.put("message", "This is an admin endpoint");
//        response.put("access", "Requires ADMIN role only");
//        response.put("authenticatedUser", authentication.getName());
//        response.put("authorities", authentication.getAuthorities().toString());
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * Endpoint to check current authentication status
//     */
//    @GetMapping("/status")
//    public ResponseEntity<Map<String, String>> authStatus(Authentication authentication) {
//        Map<String, String> response = new HashMap<>();
//        response.put("authenticated", "true");
//        response.put("username", authentication.getName());
//        response.put("roles", authentication.getAuthorities().toString());
//        return ResponseEntity.ok(response);
//    }
//}
