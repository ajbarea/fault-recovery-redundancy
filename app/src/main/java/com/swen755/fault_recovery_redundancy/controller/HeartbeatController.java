package com.swen755.fault_recovery_redundancy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.swen755.fault_recovery_redundancy.service.HeartbeatService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/heartbeat")
public class HeartbeatController {
    private final HeartbeatService heartbeatService;

    public HeartbeatController(HeartbeatService heartbeatService) {
        this.heartbeatService = heartbeatService;
    }

    /**
     * Returns detailed system and replica status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(heartbeatService.getDetailedStatus());
    }

    /**
     * Returns the current primary replica and system status.
     */
    @GetMapping("/primary")
    public ResponseEntity<Map<String, Object>> getPrimaryReplica() {
        Map<String, Object> response = new HashMap<>();
        response.put("primary_replica", heartbeatService.getCurrentPrimaryReplica());
        response.put("system_operational", heartbeatService.isSystemOperational());
        return ResponseEntity.ok(response);
    }
}