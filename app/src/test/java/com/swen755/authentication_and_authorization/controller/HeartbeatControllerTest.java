package com.swen755.authentication_and_authorization.controller;

import com.swen755.authentication_and_authorization.service.HeartbeatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HeartbeatControllerTest {

    @Mock
    private HeartbeatService heartbeatService;

    @InjectMocks
    private HeartbeatController heartbeatController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(heartbeatController).build();
    }

    @Test
    public void testGetStatus_AllHealthy() {
        // Mock service responses
        boolean[] mockHealth = {true, true};
        List<String> mockUrls = Arrays.asList(
                "http://spring-boot-app:8080/health",
                "http://spring-boot-app:8081/health"
        );
        
        when(heartbeatService.getReplicaHealth()).thenReturn(mockHealth);
        when(heartbeatService.getReplicaUrls()).thenReturn(mockUrls);

        // Call the controller method
        ResponseEntity<?> response = heartbeatController.getStatus();

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
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

    @Test
    public void testGetStatus_MixedHealth() {
        // Mock service responses - one healthy, one unhealthy
        boolean[] mockHealth = {true, false};
        List<String> mockUrls = Arrays.asList(
                "http://spring-boot-app:8080/health",
                "http://spring-boot-app:8081/health"
        );
        
        when(heartbeatService.getReplicaHealth()).thenReturn(mockHealth);
        when(heartbeatService.getReplicaUrls()).thenReturn(mockUrls);

        // Call the controller method
        ResponseEntity<?> response = heartbeatController.getStatus();

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
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

    @Test
    public void testGetStatus_AllUnhealthy() {
        // Mock service responses - all unhealthy
        boolean[] mockHealth = {false, false};
        List<String> mockUrls = Arrays.asList(
                "http://spring-boot-app:8080/health",
                "http://spring-boot-app:8081/health"
        );
        
        when(heartbeatService.getReplicaHealth()).thenReturn(mockHealth);
        when(heartbeatService.getReplicaUrls()).thenReturn(mockUrls);

        // Call the controller method
        ResponseEntity<?> response = heartbeatController.getStatus();

        // Verify response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        
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