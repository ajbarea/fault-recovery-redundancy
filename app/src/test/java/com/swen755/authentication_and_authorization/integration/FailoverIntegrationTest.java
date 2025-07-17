package com.swen755.authentication_and_authorization.integration;

import com.swen755.authentication_and_authorization.service.HeartbeatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class FailoverIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private RestTemplate mockRestTemplate;

    @Autowired
    private HeartbeatService heartbeatService;

    @Test
    public void testFailoverScenario() throws Exception {
        // Initial state - both replicas are healthy
        when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
        
        // Trigger health check
        heartbeatService.checkReplicas();
        
        // Verify initial state through heartbeat endpoint
        ResponseEntity<Map> response1 = restTemplate.getForEntity("/heartbeat/status", Map.class);
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        
        Map<String, Object> body1 = response1.getBody();
        assertNotNull(body1);
        assertEquals("operational", body1.get("system_status"));
        assertEquals(2, body1.get("healthy_replicas"));
        
        // Simulate failure of first replica
        when(mockRestTemplate.getForEntity(eq("http://spring-boot-app:8080/health"), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));
        when(mockRestTemplate.getForEntity(eq("http://spring-boot-app:8081/health"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
        
        // Trigger health check again
        heartbeatService.checkReplicas();
        
        // Verify degraded state through heartbeat endpoint
        ResponseEntity<Map> response2 = restTemplate.getForEntity("/heartbeat/status", Map.class);
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        
        Map<String, Object> body2 = response2.getBody();
        assertNotNull(body2);
        assertEquals("operational", body2.get("system_status"));
        assertEquals(1, body2.get("healthy_replicas"));
        
        // Simulate recovery of first replica
        when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
        
        // Trigger health check again
        heartbeatService.checkReplicas();
        
        // Verify recovered state through heartbeat endpoint
        ResponseEntity<Map> response3 = restTemplate.getForEntity("/heartbeat/status", Map.class);
        assertEquals(HttpStatus.OK, response3.getStatusCode());
        
        Map<String, Object> body3 = response3.getBody();
        assertNotNull(body3);
        assertEquals("operational", body3.get("system_status"));
        assertEquals(2, body3.get("healthy_replicas"));
    }
    
    @Test
    public void testCompleteFailureScenario() throws Exception {
        // Initial state - both replicas are healthy
        when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
        
        // Trigger health check
        heartbeatService.checkReplicas();
        
        // Verify initial state
        boolean[] initialHealth = heartbeatService.getReplicaHealth();
        assertTrue(initialHealth[0]);
        assertTrue(initialHealth[1]);
        
        // Simulate failure of all replicas
        when(mockRestTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));
        
        // Trigger health check again
        heartbeatService.checkReplicas();
        
        // Verify all replicas are down
        boolean[] failedHealth = heartbeatService.getReplicaHealth();
        assertFalse(failedHealth[0]);
        assertFalse(failedHealth[1]);
        
        // Verify through heartbeat endpoint
        ResponseEntity<Map> response = restTemplate.getForEntity("/heartbeat/status", Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("degraded", body.get("system_status"));
        assertEquals(0, body.get("healthy_replicas"));
    }
}