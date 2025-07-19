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
import org.springframework.web.client.RestClientException;

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
 * Integration tests for fault detection functionality.
 * Tests system's ability to detect and respond to various failure conditions
 * while maintaining accurate health status reporting.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class FaultDetectionIntegrationTest {

        @Autowired
        private TestRestTemplate restTemplate; // Client for testing HTTP endpoints

        @MockitoBean
        private RestTemplate mockRestTemplate; // Mock for simulating replica responses

        @MockitoBean
        private FailureSimulator failureSimulator;

        @Autowired
        private HeartbeatService heartbeatService;

        /**
         * Tests fault detection sensitivity thresholds.
         * Verifies system correctly identifies failures while avoiding false positives.
         */
        @Test
        public void testFaultDetectionThreshold() throws Exception {
                when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();

                assertTrue(heartbeatService.isSystemOperational());
                assertEquals(2, heartbeatService.getHealthyReplicas().size());

                when(mockRestTemplate.getForEntity(eq("http://spring-boot-app:8080/health"), eq(String.class)))
                                .thenThrow(new RestClientException("Connection refused"));
                when(mockRestTemplate.getForEntity(eq("http://spring-boot-app:8081/health"), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();

                assertTrue(heartbeatService.isSystemOperational());
                assertEquals(1, heartbeatService.getHealthyReplicas().size());

                heartbeatService.checkReplicas();

                assertTrue(heartbeatService.isSystemOperational());
                assertEquals(1, heartbeatService.getHealthyReplicas().size());
        }

        @Test
        public void testAutomaticFailover() throws Exception {
                when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();

                String initialPrimary = heartbeatService.getCurrentPrimaryReplica();
                assertNotNull(initialPrimary);

                String primaryUrl = "http://spring-boot-app:8080/health";
                String secondaryUrl = "http://spring-boot-app:8081/health";

                if (initialPrimary.equals(primaryUrl)) {
                        when(mockRestTemplate.getForEntity(eq(primaryUrl), eq(String.class)))
                                        .thenThrow(new RestClientException("Connection refused"));
                        when(mockRestTemplate.getForEntity(eq(secondaryUrl), eq(String.class)))
                                        .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
                } else {
                        when(mockRestTemplate.getForEntity(eq(secondaryUrl), eq(String.class)))
                                        .thenThrow(new RestClientException("Connection refused"));
                        when(mockRestTemplate.getForEntity(eq(primaryUrl), eq(String.class)))
                                        .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
                }

                heartbeatService.checkReplicas();
                heartbeatService.checkReplicas();
                heartbeatService.checkReplicas();

                String newPrimary = heartbeatService.getCurrentPrimaryReplica();
                assertNotNull(newPrimary);
                assertNotEquals(initialPrimary, newPrimary);

                assertTrue(heartbeatService.isSystemOperational());
                assertEquals(1, heartbeatService.getHealthyReplicas().size());
        }

        /**
         * Tests automatic recovery detection mechanism.
         * Verifies system detects when failed replicas become healthy again.
         */
        @Test
        public void testAutomaticRecovery() throws Exception {
                when(mockRestTemplate.getForEntity(eq("http://spring-boot-app:8080/health"), eq(String.class)))
                                .thenThrow(new RestClientException("Connection refused"));
                when(mockRestTemplate.getForEntity(eq("http://spring-boot-app:8081/health"), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();
                heartbeatService.checkReplicas();

                assertEquals(1, heartbeatService.getHealthyReplicas().size());

                when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();

                assertTrue(heartbeatService.isSystemOperational());
                assertEquals(2, heartbeatService.getHealthyReplicas().size());
        }

        /**
         * Tests system behavior during complete degradation and recovery.
         * Verifies proper state transitions from operational to degraded and back.
         */
        @Test
        public void testSystemDegradationAndRecovery() throws Exception {
                when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();
                assertTrue(heartbeatService.isSystemOperational());

                when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                                .thenThrow(new RestClientException("All replicas down"));

                heartbeatService.checkReplicas();
                heartbeatService.checkReplicas();

                assertFalse(heartbeatService.isSystemOperational());
                assertEquals(0, heartbeatService.getHealthyReplicas().size());

                when(mockRestTemplate.getForEntity(eq("http://spring-boot-app:8080/health"), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
                when(mockRestTemplate.getForEntity(eq("http://spring-boot-app:8081/health"), eq(String.class)))
                                .thenThrow(new RestClientException("Still down"));

                heartbeatService.checkReplicas();

                assertTrue(heartbeatService.isSystemOperational());
                assertEquals(1, heartbeatService.getHealthyReplicas().size());
        }

        /**
         * Tests status endpoint responses during failure conditions.
         * Verifies accurate health status reporting and failover event logging.
         */
        @Test
        public void testStatusEndpointDuringFailure() throws Exception {
                when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange("/heartbeat/status",
                                org.springframework.http.HttpMethod.GET, null,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });

                assertEquals(HttpStatus.OK, response.getStatusCode());
                Map<String, Object> body = response.getBody();
                assertNotNull(body);
                assertEquals("operational", body.get("system_status"));
                assertEquals(2, body.get("healthy_replicas"));

                when(mockRestTemplate.getForEntity(eq("http://spring-boot-app:8080/health"), eq(String.class)))
                                .thenThrow(new RestClientException("Connection refused"));
                when(mockRestTemplate.getForEntity(eq("http://spring-boot-app:8081/health"), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();
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

                assertTrue(body2.containsKey("recent_failover_events"));
        }

        /**
         * Tests health endpoint's replica status reporting accuracy.
         * Verifies detailed health information for individual replicas.
         */
        @Test
        public void testHealthEndpointReplicaStatus() throws Exception {
                when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();
                heartbeatService.checkReplicas();

                when(mockRestTemplate.getForEntity(eq("http://spring-boot-app:8080/health"), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
                when(mockRestTemplate.getForEntity(eq("http://spring-boot-app:8081/health"), eq(String.class)))
                                .thenThrow(new RestClientException("Connection refused"));

                heartbeatService.checkReplicas();
                heartbeatService.checkReplicas();
                heartbeatService.checkReplicas();

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange("/health/replicas",
                                org.springframework.http.HttpMethod.GET, null,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });

                assertEquals(HttpStatus.OK, response.getStatusCode());
                Map<String, Object> body = response.getBody();
                assertNotNull(body);

                assertEquals(2, body.get("total_replicas"));
                assertEquals(1, body.get("healthy_replicas"));

                @SuppressWarnings("unchecked")
                Map<String, Object> replicas = (Map<String, Object>) body.get("replicas");
                assertNotNull(replicas);

                @SuppressWarnings("unchecked")
                Map<String, Object> replica0 = (Map<String, Object>) replicas.get("replica_0");
                @SuppressWarnings("unchecked")
                Map<String, Object> replica1 = (Map<String, Object>) replicas.get("replica_1");

                boolean replica0Healthy = (Boolean) replica0.get("healthy");
                boolean replica1Healthy = (Boolean) replica1.get("healthy");

                assertTrue(replica0Healthy != replica1Healthy);
        }
}