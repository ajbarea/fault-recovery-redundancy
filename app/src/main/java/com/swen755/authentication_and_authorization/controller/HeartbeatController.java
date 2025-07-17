package com.swen755.authentication_and_authorization.controller;

import com.swen755.authentication_and_authorization.service.HeartbeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/heartbeat")
public class HeartbeatController {
    private final HeartbeatService heartbeatService;

    public HeartbeatController(HeartbeatService heartbeatService) {
        this.heartbeatService = heartbeatService;
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        boolean[] health = heartbeatService.getReplicaHealth();
        List<String> urls = heartbeatService.getReplicaUrls();
        Map<String, Object> status = new HashMap<>();
        
        // Add overall system health
        int healthyCount = 0;
        for (boolean isHealthy : health) {
            if (isHealthy) healthyCount++;
        }
        
        status.put("system_status", healthyCount > 0 ? "operational" : "degraded");
        status.put("healthy_replicas", healthyCount);
        status.put("total_replicas", health.length);
        
        // Add individual replica status
        Map<String, Object> replicas = new HashMap<>();
        for (int i = 0; i < health.length; i++) {
            Map<String, Object> replicaInfo = new HashMap<>();
            replicaInfo.put("status", health[i] ? "UP" : "DOWN");
            replicaInfo.put("url", urls.get(i));
            replicas.put("replica_" + i, replicaInfo);
        }
        
        status.put("replicas", replicas);
        
        return ResponseEntity.ok(status);
    }
}