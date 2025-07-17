package com.swen755.authentication_and_authorization.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HeartbeatServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private HeartbeatService heartbeatService;

    private List<String> testReplicaUrls;

    @BeforeEach
    public void setup() {
        testReplicaUrls = Arrays.asList(
                "http://spring-boot-app:8080/health",
                "http://spring-boot-app:8081/health"
        );
        
        // Use reflection to set the private fields
        ReflectionTestUtils.setField(heartbeatService, "replicaUrls", testReplicaUrls);
        ReflectionTestUtils.setField(heartbeatService, "restTemplate", restTemplate);
    }

    @Test
    public void testGetReplicaHealth_AllHealthy() {
        // Mock responses for both replicas
        when(restTemplate.getForEntity(eq(testReplicaUrls.get(0)), eq(String.class)))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
        when(restTemplate.getForEntity(eq(testReplicaUrls.get(1)), eq(String.class)))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));

        // Call the method to check replicas
        heartbeatService.checkReplicas();

        // Verify all replicas are marked as healthy
        boolean[] health = heartbeatService.getReplicaHealth();
        assertEquals(2, health.length);
        assertTrue(health[0]);
        assertTrue(health[1]);
    }

    @Test
    public void testGetReplicaHealth_MixedHealth() {
        // Mock responses - first replica healthy, second unhealthy
        when(restTemplate.getForEntity(eq(testReplicaUrls.get(0)), eq(String.class)))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
        when(restTemplate.getForEntity(eq(testReplicaUrls.get(1)), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // Call the method to check replicas
        heartbeatService.checkReplicas();

        // Verify correct health status
        boolean[] health = heartbeatService.getReplicaHealth();
        assertEquals(2, health.length);
        assertTrue(health[0]);
        assertFalse(health[1]);
    }

    @Test
    public void testGetReplicaHealth_AllUnhealthy() {
        // Mock responses - both replicas unhealthy
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // Call the method to check replicas
        heartbeatService.checkReplicas();

        // Verify all replicas are marked as unhealthy
        boolean[] health = heartbeatService.getReplicaHealth();
        assertEquals(2, health.length);
        assertFalse(health[0]);
        assertFalse(health[1]);
    }

    @Test
    public void testGetReplicaHealth_StatusChange() {
        // First check - all healthy
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
        heartbeatService.checkReplicas();
        
        boolean[] health1 = heartbeatService.getReplicaHealth();
        assertTrue(health1[0]);
        assertTrue(health1[1]);

        // Second check - first replica becomes unhealthy
        when(restTemplate.getForEntity(eq(testReplicaUrls.get(0)), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));
        when(restTemplate.getForEntity(eq(testReplicaUrls.get(1)), eq(String.class)))
                .thenReturn(new ResponseEntity<>("UP", HttpStatus.OK));
        
        heartbeatService.checkReplicas();
        
        boolean[] health2 = heartbeatService.getReplicaHealth();
        assertFalse(health2[0]);
        assertTrue(health2[1]);
    }

    @Test
    public void testGetReplicaUrls() {
        List<String> urls = heartbeatService.getReplicaUrls();
        assertEquals(testReplicaUrls, urls);
        assertEquals(2, urls.size());
    }
}