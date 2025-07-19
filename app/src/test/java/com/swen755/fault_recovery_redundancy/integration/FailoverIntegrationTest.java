package com.swen755.fault_recovery_redundancy.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import com.swen755.fault_recovery_redundancy.config.TestSecurityConfig;
import com.swen755.fault_recovery_redundancy.service.HeartbeatService;
import com.swen755.fault_recovery_redundancy.service.FailureSimulator;

import org.springframework.core.ParameterizedTypeReference;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Integration tests for failover functionality.
 * Tests system behavior during replica failures and recovery,
 * focusing on maintaining service availability.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class FailoverIntegrationTest {

        @Autowired
        private TestRestTemplate restTemplate; // Client for testing HTTP endpoints

        @MockitoBean
        private RestTemplate mockRestTemplate; // Mock for simulating replica responses

        @MockitoBean
        private FailureSimulator failureSimulator;

        @Autowired
        private HeartbeatService heartbeatService;

        /**
         * Tests complete failover scenario from healthy state through failure and
         * recovery.
         * Verifies system maintains operational status during replica transitions.
         */
        @Test
        public void testFailoverScenario() throws Exception {
                when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();

                ResponseEntity<Map<String, Object>> response1 = restTemplate.exchange("/heartbeat/status",
                                org.springframework.http.HttpMethod.GET, null,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });
                assertEquals(HttpStatus.OK, response1.getStatusCode());

                Map<String, Object> body1 = response1.getBody();
                assertNotNull(body1);
                assertEquals("operational", body1.get("system_status"));
                assertEquals(2, body1.get("healthy_replicas"));

                when(mockRestTemplate.getForEntity(eq("http://spring-boot-app:8080/health"), eq(String.class)))
                                .thenThrow(new RuntimeException("Connection refused"));
                when(mockRestTemplate.getForEntity(eq("http://spring-boot-app:8081/health"), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();

                ResponseEntity<Map<String, Object>> response2 = restTemplate.exchange("/heartbeat/status",
                                org.springframework.http.HttpMethod.GET, null,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });
                assertEquals(HttpStatus.OK, response2.getStatusCode());

                Map<String, Object> body2 = response2.getBody();
                assertNotNull(body2);
                assertEquals("operational", body2.get("system_status"));
                assertEquals(1, body2.get("healthy_replicas"));

                when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();

                ResponseEntity<Map<String, Object>> response3 = restTemplate.exchange("/heartbeat/status",
                                org.springframework.http.HttpMethod.GET, null,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });
                assertEquals(HttpStatus.OK, response3.getStatusCode());

                Map<String, Object> body3 = response3.getBody();
                assertNotNull(body3);
                assertEquals("operational", body3.get("system_status"));
                assertEquals(2, body3.get("healthy_replicas"));
        }

        /**
         * Tests system behavior during complete failure of all replicas.
         * Verifies proper degraded state reporting and health status updates.
         */
        @Test
        public void testCompleteFailureScenario() throws Exception {
                when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();

                boolean[] initialHealth = heartbeatService.getReplicaHealth();
                assertTrue(initialHealth[0]);
                assertTrue(initialHealth[1]);

                when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                                .thenThrow(new RuntimeException("Connection refused"));

                heartbeatService.checkReplicas();

                boolean[] failedHealth = heartbeatService.getReplicaHealth();
                assertFalse(failedHealth[0]);
                assertFalse(failedHealth[1]);
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange("/heartbeat/status",
                                org.springframework.http.HttpMethod.GET, null,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });
                assertEquals(HttpStatus.OK, response.getStatusCode());

                Map<String, Object> body = response.getBody();
                assertNotNull(body);
                assertEquals("degraded", body.get("system_status"));
                assertEquals(0, body.get("healthy_replicas"));
        }
}