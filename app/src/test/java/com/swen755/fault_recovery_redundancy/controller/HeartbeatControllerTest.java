package com.swen755.fault_recovery_redundancy.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.swen755.fault_recovery_redundancy.service.HeartbeatService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for HeartbeatController.
 * Tests system health status reporting in various replica states.
 */
@ExtendWith(MockitoExtension.class)
public class HeartbeatControllerTest {

    @Mock
    private HeartbeatService heartbeatService; // Service for monitoring replica health

    @InjectMocks
    private HeartbeatController heartbeatController; // Controller under test

    @BeforeEach
    public void setup() {
        MockMvcBuilders.standaloneSetup(heartbeatController).build();
    }

    /**
     * Tests status endpoint when all replicas are healthy.
     * Verifies response when system is fully operational.
     */
    @Test
    public void testGetStatus_AllHealthy() {
        Map<String, Object> mockDetailedStatus = new HashMap<>();
        mockDetailedStatus.put("system_status", "operational");
        mockDetailedStatus.put("healthy_replicas", 2);
        mockDetailedStatus.put("total_replicas", 2);
        mockDetailedStatus.put("primary_replica", "http://spring-boot-app:8080/health");
        mockDetailedStatus.put("primary_replica_index", 0);

        Map<String, Object> mockReplicas = new HashMap<>();
        Map<String, Object> mockReplica0 = new HashMap<>();
        mockReplica0.put("status", "UP");
        mockReplica0.put("url", "http://spring-boot-app:8080/health");
        mockReplica0.put("is_primary", true);
        mockReplicas.put("replica_0", mockReplica0);

        Map<String, Object> mockReplica1 = new HashMap<>();
        mockReplica1.put("status", "UP");
        mockReplica1.put("url", "http://spring-boot-app:8081/health");
        mockReplica1.put("is_primary", false);
        mockReplicas.put("replica_1", mockReplica1);

        mockDetailedStatus.put("replicas", mockReplicas);

        when(heartbeatService.getDetailedStatus()).thenReturn(mockDetailedStatus);

        ResponseEntity<?> response = heartbeatController.getStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Object responseBodyObj = response.getBody();
        assertNotNull(responseBodyObj);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) responseBodyObj;

        assertEquals("operational", responseBody.get("system_status"));
        assertEquals(2, responseBody.get("healthy_replicas"));
        assertEquals(2, responseBody.get("total_replicas"));

        @SuppressWarnings("unchecked")
        Map<String, Object> replicas = (Map<String, Object>) responseBody.get("replicas");

        assertNotNull(replicas);
        assertEquals(2, replicas.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> replica0 = (Map<String, Object>) replicas.get("replica_0");
        @SuppressWarnings("unchecked")
        Map<String, Object> replica1 = (Map<String, Object>) replicas.get("replica_1");

        assertEquals("UP", replica0.get("status"));
        assertEquals("UP", replica1.get("status"));
    }

    /**
     * Tests status endpoint with mixed replica health states.
     * Verifies response when some replicas are down but system remains operational.
     */
    @Test
    public void testGetStatus_MixedHealth() {
        Map<String, Object> mockDetailedStatus = new HashMap<>();
        mockDetailedStatus.put("system_status", "operational");
        mockDetailedStatus.put("healthy_replicas", 1);
        mockDetailedStatus.put("total_replicas", 2);
        mockDetailedStatus.put("primary_replica", "http://spring-boot-app:8080/health");
        mockDetailedStatus.put("primary_replica_index", 0);

        Map<String, Object> mockReplicas2 = new HashMap<>();
        Map<String, Object> mockReplica0_2 = new HashMap<>();
        mockReplica0_2.put("status", "UP");
        mockReplica0_2.put("url", "http://spring-boot-app:8080/health");
        mockReplica0_2.put("is_primary", true);
        mockReplicas2.put("replica_0", mockReplica0_2);

        Map<String, Object> mockReplica1_2 = new HashMap<>();
        mockReplica1_2.put("status", "DOWN");
        mockReplica1_2.put("url", "http://spring-boot-app:8081/health");
        mockReplica1_2.put("is_primary", false);
        mockReplicas2.put("replica_1", mockReplica1_2);

        mockDetailedStatus.put("replicas", mockReplicas2);

        when(heartbeatService.getDetailedStatus()).thenReturn(mockDetailedStatus);

        ResponseEntity<?> response = heartbeatController.getStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Object responseBodyObj = response.getBody();
        assertNotNull(responseBodyObj);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) responseBodyObj;

        assertEquals("operational", responseBody.get("system_status"));
        assertEquals(1, responseBody.get("healthy_replicas"));
        assertEquals(2, responseBody.get("total_replicas"));

        @SuppressWarnings("unchecked")
        Map<String, Object> replicas = (Map<String, Object>) responseBody.get("replicas");

        assertNotNull(replicas);
        assertEquals(2, replicas.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> replica0 = (Map<String, Object>) replicas.get("replica_0");
        @SuppressWarnings("unchecked")
        Map<String, Object> replica1 = (Map<String, Object>) replicas.get("replica_1");

        assertEquals("UP", replica0.get("status"));
        assertEquals("DOWN", replica1.get("status"));
    }

    /**
     * Tests status endpoint when all replicas are unhealthy.
     * Verifies response when system is in degraded state.
     */
    @Test
    public void testGetStatus_AllUnhealthy() {
        Map<String, Object> mockDetailedStatus = new HashMap<>();
        mockDetailedStatus.put("system_status", "degraded");
        mockDetailedStatus.put("healthy_replicas", 0);
        mockDetailedStatus.put("total_replicas", 2);
        mockDetailedStatus.put("primary_replica", "http://spring-boot-app:8080/health");
        mockDetailedStatus.put("primary_replica_index", 0);

        Map<String, Object> mockReplicas3 = new HashMap<>();
        Map<String, Object> mockReplica0_3 = new HashMap<>();
        mockReplica0_3.put("status", "DOWN");
        mockReplica0_3.put("url", "http://spring-boot-app:8080/health");
        mockReplica0_3.put("is_primary", true);
        mockReplicas3.put("replica_0", mockReplica0_3);

        Map<String, Object> mockReplica1_3 = new HashMap<>();
        mockReplica1_3.put("status", "DOWN");
        mockReplica1_3.put("url", "http://spring-boot-app:8081/health");
        mockReplica1_3.put("is_primary", false);
        mockReplicas3.put("replica_1", mockReplica1_3);

        mockDetailedStatus.put("replicas", mockReplicas3);

        when(heartbeatService.getDetailedStatus()).thenReturn(mockDetailedStatus);

        ResponseEntity<?> response = heartbeatController.getStatus();

        assertEquals(HttpStatus.OK, response.getStatusCode());

        Object responseBodyObj = response.getBody();
        assertNotNull(responseBodyObj);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) responseBodyObj;

        assertEquals("degraded", responseBody.get("system_status"));
        assertEquals(0, responseBody.get("healthy_replicas"));
        assertEquals(2, responseBody.get("total_replicas"));

        @SuppressWarnings("unchecked")
        Map<String, Object> replicas = (Map<String, Object>) responseBody.get("replicas");

        assertNotNull(replicas);
        assertEquals(2, replicas.size());

        @SuppressWarnings("unchecked")
        Map<String, Object> replica0 = (Map<String, Object>) replicas.get("replica_0");
        @SuppressWarnings("unchecked")
        Map<String, Object> replica1 = (Map<String, Object>) replicas.get("replica_1");

        assertEquals("DOWN", replica0.get("status"));
        assertEquals("DOWN", replica1.get("status"));
    }
}