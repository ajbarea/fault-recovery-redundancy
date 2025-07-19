package com.swen755.fault_recovery_redundancy.controller;

import com.swen755.fault_recovery_redundancy.service.FailureSimulator;
import com.swen755.fault_recovery_redundancy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/simulation")
public class FailureSimulationController {
    private final FailureSimulator failureSimulator;
    private final UserService userService;

    public FailureSimulationController(FailureSimulator failureSimulator, UserService userService) {
        this.failureSimulator = failureSimulator;
        this.userService = userService;
    }

    @DeleteMapping("/users")
    public ResponseEntity<Map<String, Object>> deleteAllUsers() {
        userService.deleteAllUsers();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "All users have been deleted.");
        return ResponseEntity.ok(response);
    }

    /**
     * Enables failure simulation globally.
     */
    @PostMapping("/enable")
    public ResponseEntity<Map<String, Object>> enableSimulation() {
        failureSimulator.setSimulationEnabled(true);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Failure simulation enabled");
        response.put("enabled", true);
        return ResponseEntity.ok(response);
    }

    /**
     * Disables failure simulation and clears all failures.
     */
    @PostMapping("/disable")
    public ResponseEntity<Map<String, Object>> disableSimulation() {
        failureSimulator.setSimulationEnabled(false);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Failure simulation disabled");
        response.put("enabled", false);
        return ResponseEntity.ok(response);
    }

    /**
     * Simulates a failure for a specific replica.
     */
    @PostMapping("/fail/{replicaUrl}")
    public ResponseEntity<Map<String, Object>> simulateFailure(@PathVariable String replicaUrl) {
        String decodedUrl = replicaUrl.replace("_", "/").replace("-", ":");
        failureSimulator.simulateFailure(decodedUrl);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Simulated failure for replica: " + decodedUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * Recovers a specific replica from simulated failure.
     */
    @PostMapping("/recover/{replicaUrl}")
    public ResponseEntity<Map<String, Object>> recoverReplica(@PathVariable String replicaUrl) {
        String decodedUrl = replicaUrl.replace("_", "/").replace("-", ":");
        failureSimulator.recoverReplica(decodedUrl);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Recovered replica: " + decodedUrl);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the current simulation status and failed replicas.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSimulationStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("simulation_enabled", failureSimulator.isSimulationEnabled());
        status.put("simulated_failures", failureSimulator.getSimulationStatus());
        return ResponseEntity.ok(status);
    }

    /**
     * Clears all simulated failures.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetSimulation() {
        failureSimulator.resetAllFailures();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "All simulated failures cleared");
        return ResponseEntity.ok(response);
    }
}