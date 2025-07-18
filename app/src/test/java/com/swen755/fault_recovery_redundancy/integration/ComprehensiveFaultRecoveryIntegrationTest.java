package com.swen755.fault_recovery_redundancy.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;

/**
 * Integration tests for fault recovery system.
 * Tests complete fault detection, failover, and recovery workflows
 * including system degradation, automatic recovery, and status reporting.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class ComprehensiveFaultRecoveryIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate; // Client for testing HTTP endpoints

    @MockBean
    private RestTemplate mockRestTemplate; // Mock for simulating replica responses

    @MockBean
    private FailureSimulator failureSimulator; // Service for controlled failure simulation

    @Autowired
    private HeartbeatService heartbeatService;

    private static final String REPLICA_1_URL = "http://spring-boot-app:8080/health";
    private static final String REPLICA_2_URL = "http://spring-boot-app:8081/health";

    @BeforeEach
    void setUp() {
        reset(mockRestTemplate);

        mockAllReplicasHealthy();
        heartbeatService.checkReplicas();
    }

    /**
     * Verifies the system detects replica failures and continues operating with
     * healthy replicas.
     */
    @Test
    public void testBasicFaultDetectionWorkflow() throws Exception {

        mockAllReplicasHealthy();
        heartbeatService.checkReplicas();

        assertTrue(heartbeatService.isSystemOperational());
        assertEquals(2, heartbeatService.getHealthyReplicas().size());

        mockReplica1Failed();
        heartbeatService.checkReplicas();

        assertTrue(heartbeatService.isSystemOperational());
        assertEquals(1, heartbeatService.getHealthyReplicas().size());

        heartbeatService.checkReplicas();

        assertTrue(heartbeatService.isSystemOperational());
        assertEquals(1, heartbeatService.getHealthyReplicas().size());

        boolean[] replicaHealth = heartbeatService.getReplicaHealth();
        assertFalse(replicaHealth[0]);
        assertTrue(replicaHealth[1]);
    }

    /**
     * Ensures automatic failover promotes a healthy secondary replica to primary
     * when the current primary fails.
     */
    @Test
    public void testAutomaticFailoverExecution() throws Exception {

        mockAllReplicasHealthy();
        heartbeatService.checkReplicas();

        String initialPrimary = heartbeatService.getCurrentPrimaryReplica();
        assertNotNull(initialPrimary);

        if (initialPrimary.contains("8080")) {
            mockReplica1Failed();
        } else {
            mockReplica2Failed();
        }

        heartbeatService.checkReplicas();
        heartbeatService.checkReplicas();

        String newPrimary = heartbeatService.getCurrentPrimaryReplica();
        assertNotNull(newPrimary);
        assertNotEquals(initialPrimary, newPrimary);

        assertTrue(heartbeatService.isSystemOperational());
        assertEquals(1, heartbeatService.getHealthyReplicas().size());
    }

    /**
     * Confirms the system detects when failed replicas recover and updates health
     * status accordingly.
     */
    @Test
    public void testAutomaticRecoveryDetection() throws Exception {
        mockReplica1Failed();
        heartbeatService.checkReplicas();
        heartbeatService.checkReplicas();

        assertEquals(1, heartbeatService.getHealthyReplicas().size());

        mockAllReplicasHealthy();
        heartbeatService.checkReplicas();

        assertTrue(heartbeatService.isSystemOperational());
        assertEquals(2, heartbeatService.getHealthyReplicas().size());

        boolean[] replicaHealth = heartbeatService.getReplicaHealth();
        assertTrue(replicaHealth[0]);
        assertTrue(replicaHealth[1]);
    }

    /**
     * Tests system behavior during total failure and subsequent recovery of
     * replicas.
     */
    @Test
    public void testCompleteSystemFailureAndRecovery() throws Exception {
        mockAllReplicasHealthy();
        heartbeatService.checkReplicas();
        assertTrue(heartbeatService.isSystemOperational());

        mockAllReplicasFailed();
        heartbeatService.checkReplicas();
        heartbeatService.checkReplicas();

        assertFalse(heartbeatService.isSystemOperational());
        assertEquals(0, heartbeatService.getHealthyReplicas().size());

        mockReplica1Healthy();
        heartbeatService.checkReplicas();

        assertTrue(heartbeatService.isSystemOperational());
        assertEquals(1, heartbeatService.getHealthyReplicas().size());

        mockAllReplicasHealthy();
        heartbeatService.checkReplicas();

        assertTrue(heartbeatService.isSystemOperational());
        assertEquals(2, heartbeatService.getHealthyReplicas().size());
    }

    /**
     * Validates the /heartbeat/status endpoint returns correct system and replica
     * status during failures.
     */
    @Test
    public void testStatusEndpointDuringFailureScenarios() throws Exception {
        mockAllReplicasHealthy();
        heartbeatService.checkReplicas();

        ResponseEntity<Map<String, Object>> response = getHeartbeatStatus();
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("operational", body.get("system_status"));
        assertEquals(2, body.get("healthy_replicas"));

        mockReplica1Failed();
        heartbeatService.checkReplicas();
        heartbeatService.checkReplicas();

        ResponseEntity<Map<String, Object>> response2 = getHeartbeatStatus();
        assertEquals(HttpStatus.OK, response2.getStatusCode());

        Map<String, Object> body2 = response2.getBody();
        assertNotNull(body2);
        assertEquals("operational", body2.get("system_status"));
        assertEquals(1, body2.get("healthy_replicas"));

        assertTrue(body2.containsKey("recent_failover_events"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) body2.get("recent_failover_events");
        assertNotNull(events);
        assertTrue(events.size() > 0);
    }

    /**
     * Checks that the /health/replicas endpoint accurately reports health and
     * details for each replica.
     */
    @Test
    public void testReplicaHealthEndpointAccuracy() throws Exception {
        mockReplica1Healthy();
        mockReplica2Failed();
        heartbeatService.checkReplicas();
        heartbeatService.checkReplicas();

        ResponseEntity<Map<String, Object>> response = getReplicasHealth();
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        assertEquals(2, body.get("total_replicas"));

        @SuppressWarnings("unchecked")
        Map<String, Object> replicas = (Map<String, Object>) body.get("replicas");
        assertNotNull(replicas);

        assertTrue(replicas.containsKey("replica_0"));
        assertTrue(replicas.containsKey("replica_1"));

        @SuppressWarnings("unchecked")
        Map<String, Object> replica0 = (Map<String, Object>) replicas.get("replica_0");
        @SuppressWarnings("unchecked")
        Map<String, Object> replica1 = (Map<String, Object>) replicas.get("replica_1");

        Boolean replica0Healthy = (Boolean) replica0.get("healthy");
        Boolean replica1Healthy = (Boolean) replica1.get("healthy");

        assertNotNull(replica0Healthy, "Replica 0 should have a healthy status");
        assertNotNull(replica1Healthy, "Replica 1 should have a healthy status");

        assertNotNull(replica0.get("url"), "Replica 0 should have a URL");
        assertNotNull(replica1.get("url"), "Replica 1 should have a URL");

        int healthyCount = (Integer) body.get("healthy_replicas");
        assertTrue(healthyCount >= 1, "Should have at least 1 healthy replica, but had: " + healthyCount);
    }

    /**
     * Verifies that failover events are logged and retrievable after a failover
     * occurs.
     */
    @Test
    public void testFailoverEventLogging() throws Exception {
        mockAllReplicasHealthy();
        heartbeatService.checkReplicas();

        String initialPrimary = heartbeatService.getCurrentPrimaryReplica();

        if (initialPrimary.contains("8080")) {
            mockReplica1Failed();
        } else {
            mockReplica2Failed();
        }

        heartbeatService.checkReplicas();
        heartbeatService.checkReplicas();

        ResponseEntity<Map<String, Object>> response = getHeartbeatStatus();
        Map<String, Object> body = response.getBody();
        assertNotNull(body);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) body.get("recent_failover_events");
        assertNotNull(events);

        boolean hasFailoverEvent = events.stream()
                .anyMatch(event -> "FAILOVER".equals(event.get("event_type")));
        assertTrue(hasFailoverEvent, "Should have recorded a failover event");
    }

    private void mockAllReplicasHealthy() {
        when(mockRestTemplate.getForEntity(eq(REPLICA_1_URL), eq(String.class)))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
        when(mockRestTemplate.getForEntity(eq(REPLICA_2_URL), eq(String.class)))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
    }

    private void mockAllReplicasFailed() {
        when(mockRestTemplate.getForEntity(eq(REPLICA_1_URL), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));
        when(mockRestTemplate.getForEntity(eq(REPLICA_2_URL), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));
    }

    private void mockReplica1Failed() {
        when(mockRestTemplate.getForEntity(eq(REPLICA_1_URL), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));
        when(mockRestTemplate.getForEntity(eq(REPLICA_2_URL), eq(String.class)))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
    }

    private void mockReplica2Failed() {
        when(mockRestTemplate.getForEntity(eq(REPLICA_1_URL), eq(String.class)))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
        when(mockRestTemplate.getForEntity(eq(REPLICA_2_URL), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));
    }

    private void mockReplica1Healthy() {
        when(mockRestTemplate.getForEntity(eq(REPLICA_1_URL), eq(String.class)))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
    }

    private ResponseEntity<Map<String, Object>> getHeartbeatStatus() {
        return restTemplate.exchange("/heartbeat/status",
                org.springframework.http.HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
    }

    private ResponseEntity<Map<String, Object>> getReplicasHealth() {
        return restTemplate.exchange("/health/replicas",
                org.springframework.http.HttpMethod.GET, null,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
    }
}