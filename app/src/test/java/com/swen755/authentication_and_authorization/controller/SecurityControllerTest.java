package com.swen755.authentication_and_authorization.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for security endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void publicEndpoint_ShouldBeAccessibleWithoutAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/public", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("public endpoint");
    }

    @Test
    void userEndpoint_ShouldRequireAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/user", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void adminEndpoint_ShouldRequireAuthentication() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/admin", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void userEndpoint_ShouldBeAccessibleWithValidUserCredentials() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("user", "user123")
                .getForEntity("http://localhost:" + port + "/api/user", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("user endpoint");
    }

    @Test
    void adminEndpoint_ShouldBeAccessibleWithAdminCredentials() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("admin", "admin123")
                .getForEntity("http://localhost:" + port + "/api/admin", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("admin endpoint");
    }

    @Test
    void adminEndpoint_ShouldBeForbiddenForUserRole() {
        ResponseEntity<String> response = restTemplate.withBasicAuth("user", "user123")
                .getForEntity("http://localhost:" + port + "/api/admin", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
