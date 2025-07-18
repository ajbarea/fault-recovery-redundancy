package com.swen755.fault_recovery_redundancy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.swen755.fault_recovery_redundancy.config.TestSecurityConfig;
import com.swen755.fault_recovery_redundancy.service.HeartbeatService;
import com.swen755.fault_recovery_redundancy.service.FailureSimulator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the HealthController endpoints.
 * Tests health status, fault recovery status, and replica health monitoring.
 */
@WebMvcTest(HealthController.class)
@Import(TestSecurityConfig.class)
class HealthControllerIntegrationTest {

        @Autowired
        private MockMvc mockMvc; // Spring MVC test support

        @MockBean
        private HeartbeatService heartbeatService; // Service for monitoring system health

        @MockBean
        private FailureSimulator failureSimulator; // Service for simulating system failures

        /**
         * Tests the health endpoint when the system is operational.
         * Verifies proper response structure and status indicators.
         */
        @Test
        void testHealthEndpoint_SystemOperational() throws Exception {
                when(heartbeatService.isSystemOperational()).thenReturn(true);
                when(heartbeatService.getHealthyReplicas()).thenReturn(Arrays.asList("http://replica1:8080"));
                when(heartbeatService.getReplicaUrls())
                                .thenReturn(Arrays.asList("http://replica1:8080", "http://replica2:8081"));
                when(heartbeatService.getCurrentPrimaryReplica()).thenReturn("http://replica1:8080");

                mockMvc.perform(get("/health"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.status").value("UP"))
                                .andExpect(jsonPath("$.system_operational").value(true))
                                .andExpect(jsonPath("$.healthy_replicas_count").value(1))
                                .andExpect(jsonPath("$.total_replicas_count").value(2))
                                .andExpect(jsonPath("$.primary_replica").value("http://replica1:8080"))
                                .andExpect(jsonPath("$.service").value("spring-boot-app"));
        }

        /**
         * Tests the health endpoint when the system is down.
         * Verifies service unavailable status and proper error response.
         */
        @Test
        void testHealthEndpoint_SystemDown() throws Exception {
                when(heartbeatService.isSystemOperational()).thenReturn(false);
                when(heartbeatService.getHealthyReplicas()).thenReturn(Arrays.asList());
                when(heartbeatService.getReplicaUrls())
                                .thenReturn(Arrays.asList("http://replica1:8080", "http://replica2:8081"));
                when(heartbeatService.getCurrentPrimaryReplica()).thenReturn("http://replica1:8080");

                mockMvc.perform(get("/health"))
                                .andExpect(status().isServiceUnavailable())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.status").value("DOWN"))
                                .andExpect(jsonPath("$.system_operational").value(false))
                                .andExpect(jsonPath("$.healthy_replicas_count").value(0))
                                .andExpect(jsonPath("$.total_replicas_count").value(2));
        }

        /**
         * Tests the fault recovery status endpoint.
         * Verifies detailed system status information and replica health metrics.
         */
        @Test
        void testFaultRecoveryStatusEndpoint() throws Exception {
                Map<String, Object> detailedStatus = new HashMap<>();
                detailedStatus.put("system_status", "operational");
                detailedStatus.put("healthy_replicas", 2);
                detailedStatus.put("total_replicas", 2);
                detailedStatus.put("primary_replica", "http://replica1:8080");

                when(heartbeatService.getDetailedStatus()).thenReturn(detailedStatus);
                when(heartbeatService.isSystemOperational()).thenReturn(true);

                mockMvc.perform(get("/health/status"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.endpoint").value("fault-recovery-status"))
                                .andExpect(jsonPath("$.system_status").value("operational"))
                                .andExpect(jsonPath("$.healthy_replicas").value(2))
                                .andExpect(jsonPath("$.total_replicas").value(2))
                                .andExpect(jsonPath("$.primary_replica").value("http://replica1:8080"));
        }

        /**
         * Tests the replicas health endpoint.
         * Verifies health status of individual replicas and primary replica
         * identification.
         */
        @Test
        void testReplicasHealthEndpoint() throws Exception {
                when(heartbeatService.getReplicaUrls())
                                .thenReturn(Arrays.asList("http://replica1:8080", "http://replica2:8081"));
                when(heartbeatService.getReplicaHealth()).thenReturn(new boolean[] { true, false });
                when(heartbeatService.getHealthyReplicas()).thenReturn(Arrays.asList("http://replica1:8080"));
                when(heartbeatService.getCurrentPrimaryReplica()).thenReturn("http://replica1:8080");
                when(heartbeatService.isSystemOperational()).thenReturn(true);

                mockMvc.perform(get("/health/replicas"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.endpoint").value("replicas-health"))
                                .andExpect(jsonPath("$.total_replicas").value(2))
                                .andExpect(jsonPath("$.healthy_replicas").value(1))
                                .andExpect(jsonPath("$.primary_replica").value("http://replica1:8080"))
                                .andExpect(jsonPath("$.replicas.replica_0.url").value("http://replica1:8080"))
                                .andExpect(jsonPath("$.replicas.replica_0.healthy").value(true))
                                .andExpect(jsonPath("$.replicas.replica_0.is_primary").value(true))
                                .andExpect(jsonPath("$.replicas.replica_1.url").value("http://replica2:8081"))
                                .andExpect(jsonPath("$.replicas.replica_1.healthy").value(false))
                                .andExpect(jsonPath("$.replicas.replica_1.is_primary").value(false));
        }

        /**
         * Tests that the root endpoint redirects to health endpoint.
         * Verifies proper redirection and health status response.
         */
        @Test
        void testRootEndpoint_RedirectsToHealth() throws Exception {
                when(heartbeatService.isSystemOperational()).thenReturn(true);
                when(heartbeatService.getHealthyReplicas()).thenReturn(Arrays.asList("http://replica1:8080"));
                when(heartbeatService.getReplicaUrls()).thenReturn(Arrays.asList("http://replica1:8080"));
                when(heartbeatService.getCurrentPrimaryReplica()).thenReturn("http://replica1:8080");

                mockMvc.perform(get("/"))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.status").value("UP"))
                                .andExpect(jsonPath("$.service").value("spring-boot-app"));
        }
}