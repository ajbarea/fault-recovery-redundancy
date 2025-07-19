package com.swen755.fault_recovery_redundancy.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HeartbeatService.
 * Tests replica health monitoring and status tracking functionality.
 */
@ExtendWith(MockitoExtension.class)
public class HeartbeatServiceTest {

        @Mock
        private RestTemplate restTemplate; // HTTP client for health checks

        @Mock
        private FailureSimulator failureSimulator; // Simulates replica failures

        @InjectMocks
        private HeartbeatService heartbeatService; // Service under test

        private List<String> testReplicaUrls; // Test replica endpoints

        @BeforeEach
        public void setup() {
                testReplicaUrls = Arrays.asList(
                                "http://spring-boot-app:8080/health",
                                "http://spring-boot-app:8081/health");

                ReflectionTestUtils.setField(heartbeatService, "replicaUrls", testReplicaUrls);
                ReflectionTestUtils.setField(heartbeatService, "restTemplate", restTemplate);
                ReflectionTestUtils.setField(heartbeatService, "failureSimulator", failureSimulator);
                ReflectionTestUtils.setField(heartbeatService, "replicaUrlsConfig", String.join(",", testReplicaUrls));

                lenient().when(failureSimulator.shouldSimulateFailure(anyString())).thenReturn(false);

                heartbeatService.initializeReplicas();
        }

        /**
         * Tests health check when all replicas are responding normally.
         * Verifies correct health status reporting for fully operational system.
         */
        @Test
        public void testGetReplicaHealth_AllHealthy() {
                when(restTemplate.getForEntity(eq(testReplicaUrls.get(0)), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
                when(restTemplate.getForEntity(eq(testReplicaUrls.get(1)), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();

                boolean[] health = heartbeatService.getReplicaHealth();
                assertEquals(2, health.length);
                assertTrue(health[0]);
                assertTrue(health[1]);
        }

        /**
         * Tests health check with mixed replica states.
         * Verifies correct health status when some replicas are unreachable.
         */
        @Test
        public void testGetReplicaHealth_MixedHealth() {
                when(restTemplate.getForEntity(eq(testReplicaUrls.get(0)), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
                when(restTemplate.getForEntity(eq(testReplicaUrls.get(1)), eq(String.class)))
                                .thenThrow(new RestClientException("Connection refused"));

                heartbeatService.checkReplicas();

                boolean[] health = heartbeatService.getReplicaHealth();
                assertEquals(2, health.length);
                assertTrue(health[0]);
                assertFalse(health[1]);
        }

        /**
         * Tests health check when all replicas are unreachable.
         * Verifies proper handling of complete system failure.
         */
        @Test
        public void testGetReplicaHealth_AllUnhealthy() {
                when(restTemplate.getForEntity(anyString(), eq(String.class)))
                                .thenThrow(new RestClientException("Connection refused"));

                heartbeatService.checkReplicas();

                boolean[] health = heartbeatService.getReplicaHealth();
                assertEquals(2, health.length);
                assertFalse(health[0]);
                assertFalse(health[1]);
        }

        /**
         * Tests health status transitions between checks.
         * Verifies health status updates when replica availability changes.
         */
        @Test
        public void testGetReplicaHealth_StatusChange() {
                when(restTemplate.getForEntity(anyString(), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
                heartbeatService.checkReplicas();

                boolean[] health1 = heartbeatService.getReplicaHealth();
                assertTrue(health1[0]);
                assertTrue(health1[1]);

                when(restTemplate.getForEntity(eq(testReplicaUrls.get(0)), eq(String.class)))
                                .thenThrow(new RestClientException("Connection refused"));
                when(restTemplate.getForEntity(eq(testReplicaUrls.get(1)), eq(String.class)))
                                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

                heartbeatService.checkReplicas();

                boolean[] health2 = heartbeatService.getReplicaHealth();
                assertFalse(health2[0]);
                assertTrue(health2[1]);
        }

        /**
         * Tests retrieval of configured replica URLs.
         * Verifies correct initialization of replica endpoints.
         */
        @Test
        public void testGetReplicaUrls() {
                List<String> urls = heartbeatService.getReplicaUrls();
                assertEquals(testReplicaUrls, urls);
                assertEquals(2, urls.size());
        }
}