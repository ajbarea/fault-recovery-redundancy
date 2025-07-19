package com.swen755.fault_recovery_redundancy.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swen755.fault_recovery_redundancy.service.HeartbeatService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private HeartbeatService heartbeatService;

    /**
     * Returns basic health and status of the service and replicas.
     */
    @GetMapping({ "/", "/health" })
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new HashMap<>();

        status.put("service", "spring-boot-app");
        status.put("timestamp", System.currentTimeMillis());

        boolean systemOperational = heartbeatService.isSystemOperational();
        List<String> healthyReplicas = heartbeatService.getHealthyReplicas();
        int totalReplicas = heartbeatService.getReplicaUrls().size();

        if (systemOperational) {
            status.put("status", "UP");
        } else {
            status.put("status", "DOWN");
        }

        status.put("system_operational", systemOperational);
        status.put("healthy_replicas_count", healthyReplicas.size());
        status.put("total_replicas_count", totalReplicas);
        status.put("primary_replica", heartbeatService.getCurrentPrimaryReplica());

        HttpStatus httpStatus = systemOperational ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
        return ResponseEntity.status(httpStatus).body(status);
    }

    /**
     * Returns detailed fault recovery status and recent events.
     */
    @GetMapping("/health/status")
    public ResponseEntity<Map<String, Object>> faultRecoveryStatus() {
        Map<String, Object> detailedStatus = heartbeatService.getDetailedStatus();

        detailedStatus.put("endpoint", "fault-recovery-status");
        detailedStatus.put("timestamp", System.currentTimeMillis());

        boolean systemOperational = heartbeatService.isSystemOperational();
        HttpStatus httpStatus = systemOperational ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(httpStatus).body(detailedStatus);
    }

    /**
     * Returns health status for each replica.
     */
    @GetMapping("/health/replicas")
    public ResponseEntity<Map<String, Object>> replicasHealth() {
        Map<String, Object> replicaStatus = new HashMap<>();

        List<String> replicaUrls = heartbeatService.getReplicaUrls();
        boolean[] replicaHealth = heartbeatService.getReplicaHealth();
        List<String> healthyReplicas = heartbeatService.getHealthyReplicas();

        replicaStatus.put("endpoint", "replicas-health");
        replicaStatus.put("timestamp", System.currentTimeMillis());
        replicaStatus.put("total_replicas", replicaUrls.size());
        replicaStatus.put("healthy_replicas", healthyReplicas.size());
        replicaStatus.put("primary_replica", heartbeatService.getCurrentPrimaryReplica());

        Map<String, Object> replicas = new HashMap<>();
        for (int i = 0; i < replicaUrls.size(); i++) {
            Map<String, Object> replica = new HashMap<>();
            replica.put("url", replicaUrls.get(i));
            replica.put("healthy", replicaHealth[i]);
            replica.put("is_primary", replicaUrls.get(i).equals(heartbeatService.getCurrentPrimaryReplica()));
            replicas.put("replica_" + i, replica);
        }
        replicaStatus.put("replicas", replicas);

        boolean systemOperational = heartbeatService.isSystemOperational();
        HttpStatus httpStatus = systemOperational ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(httpStatus).body(replicaStatus);
    }
}