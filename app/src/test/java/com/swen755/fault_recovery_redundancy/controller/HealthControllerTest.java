package com.swen755.fault_recovery_redundancy.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.swen755.fault_recovery_redundancy.service.HeartbeatService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthController class.
 * Tests the controller's response handling and interaction with
 * HeartbeatService.
 */
class HealthControllerTest {

    @Mock
    private HeartbeatService heartbeatService; // Service for system health monitoring

    @InjectMocks
    private HealthController healthController; // Controller under test

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Tests health check endpoint when system is operational.
     * Verifies OK status and correct health metrics in response.
     */
    @Test
    void testHealthCheck_SystemOperational() {
        when(heartbeatService.isSystemOperational()).thenReturn(true);
        when(heartbeatService.getHealthyReplicas())
                .thenReturn(Arrays.asList("http://replica1:8080", "http://replica2:8081"));
        when(heartbeatService.getReplicaUrls())
                .thenReturn(Arrays.asList("http://replica1:8080", "http://replica2:8081"));
        when(heartbeatService.getCurrentPrimaryReplica()).thenReturn("http://replica1:8080");

        ResponseEntity<Map<String, Object>> response = healthController.healthCheck();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("UP", body.get("status"));
        assertEquals(true, body.get("system_operational"));
        assertEquals(2, body.get("healthy_replicas_count"));
        assertEquals(2, body.get("total_replicas_count"));
        assertEquals("http://replica1:8080", body.get("primary_replica"));
    }

    /**
     * Tests health check endpoint when system is down.
     * Verifies SERVICE_UNAVAILABLE status and proper error response.
     */
    @Test
    void testHealthCheck_SystemDown() {
        when(heartbeatService.isSystemOperational()).thenReturn(false);
        when(heartbeatService.getHealthyReplicas()).thenReturn(Arrays.asList());
        when(heartbeatService.getReplicaUrls())
                .thenReturn(Arrays.asList("http://replica1:8080", "http://replica2:8081"));
        when(heartbeatService.getCurrentPrimaryReplica()).thenReturn("http://replica1:8080");

        ResponseEntity<Map<String, Object>> response = healthController.healthCheck();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("DOWN", body.get("status"));
        assertEquals(false, body.get("system_operational"));
        assertEquals(0, body.get("healthy_replicas_count"));
        assertEquals(2, body.get("total_replicas_count"));
    }

    /**
     * Tests fault recovery status when system is operational.
     * Verifies OK status and detailed system metrics.
     */
    @Test
    void testFaultRecoveryStatus_SystemOperational() {
        Map<String, Object> detailedStatus = new HashMap<>();
        detailedStatus.put("system_status", "operational");
        detailedStatus.put("healthy_replicas", 2);
        detailedStatus.put("total_replicas", 2);

        when(heartbeatService.getDetailedStatus()).thenReturn(detailedStatus);
        when(heartbeatService.isSystemOperational()).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = healthController.faultRecoveryStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("fault-recovery-status", body.get("endpoint"));
        assertEquals("operational", body.get("system_status"));
        assertTrue(body.containsKey("timestamp"));
    }

    /**
     * Tests fault recovery status when system is degraded.
     * Verifies SERVICE_UNAVAILABLE status and degraded state indicators.
     */
    @Test
    void testFaultRecoveryStatus_SystemDegraded() {
        Map<String, Object> detailedStatus = new HashMap<>();
        detailedStatus.put("system_status", "degraded");
        detailedStatus.put("healthy_replicas", 0);
        detailedStatus.put("total_replicas", 2);

        when(heartbeatService.getDetailedStatus()).thenReturn(detailedStatus);
        when(heartbeatService.isSystemOperational()).thenReturn(false);

        ResponseEntity<Map<String, Object>> response = healthController.faultRecoveryStatus();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("fault-recovery-status", body.get("endpoint"));
        assertEquals("degraded", body.get("system_status"));
    }

    /**
     * Tests replicas health endpoint.
     * Verifies detailed health status of individual replicas and primary replica
     * identification.
     */
    @Test
    void testReplicasHealth() {
        List<String> replicaUrls = Arrays.asList("http://replica1:8080", "http://replica2:8081");
        boolean[] replicaHealth = { true, false };
        List<String> healthyReplicas = Arrays.asList("http://replica1:8080");

        when(heartbeatService.getReplicaUrls()).thenReturn(replicaUrls);
        when(heartbeatService.getReplicaHealth()).thenReturn(replicaHealth);
        when(heartbeatService.getHealthyReplicas()).thenReturn(healthyReplicas);
        when(heartbeatService.getCurrentPrimaryReplica()).thenReturn("http://replica1:8080");
        when(heartbeatService.isSystemOperational()).thenReturn(true);

        ResponseEntity<Map<String, Object>> response = healthController.replicasHealth();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("replicas-health", body.get("endpoint"));
        assertEquals(2, body.get("total_replicas"));
        assertEquals(1, body.get("healthy_replicas"));
        assertEquals("http://replica1:8080", body.get("primary_replica"));

        @SuppressWarnings("unchecked")
        Map<String, Object> replicas = (Map<String, Object>) body.get("replicas");
        assertNotNull(replicas);

        @SuppressWarnings("unchecked")
        Map<String, Object> replica0 = (Map<String, Object>) replicas.get("replica_0");
        assertEquals("http://replica1:8080", replica0.get("url"));
        assertEquals(true, replica0.get("healthy"));
        assertEquals(true, replica0.get("is_primary"));

        @SuppressWarnings("unchecked")
        Map<String, Object> replica1 = (Map<String, Object>) replicas.get("replica_1");
        assertEquals("http://replica2:8081", replica1.get("url"));
        assertEquals(false, replica1.get("healthy"));
        assertEquals(false, replica1.get("is_primary"));
    }
}