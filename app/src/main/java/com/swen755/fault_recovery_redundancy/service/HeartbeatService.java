package com.swen755.fault_recovery_redundancy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class HeartbeatService {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatService.class);
    private final RestTemplate restTemplate;
    private final FailureSimulator failureSimulator;

    // Comma-separated list of replica health check URLs
    @Value("${fault.recovery.replica.urls:http://spring-boot-app:8080/health,http://spring-boot-app:8081/health}")
    private String replicaUrlsConfig;

    // Heartbeat interval in milliseconds
    @Value("${fault.recovery.heartbeat.interval:10000}")
    private long heartbeatInterval;

    // Number of consecutive failures before marking a replica as down
    @Value("${fault.recovery.failure.threshold:3}")
    private int failureThreshold;

    // Number of consecutive successes before marking a replica as up
    @Value("${fault.recovery.recovery.threshold:2}")
    private int recoveryThreshold;

    private List<String> replicaUrls;
    private final Map<String, ReplicaStatus> replicaStatusMap = new ConcurrentHashMap<>();
    private final AtomicInteger primaryReplicaIndex = new AtomicInteger(0);
    private volatile boolean systemOperational = true;

    // Tracks consecutive failures and successes for each replica
    private final Map<String, Integer> consecutiveFailures = new ConcurrentHashMap<>();
    private final Map<String, Integer> consecutiveSuccesses = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastFailoverTime = new ConcurrentHashMap<>();
    private final List<FailoverEvent> failoverHistory = new ArrayList<>();

    public HeartbeatService(FailureSimulator failureSimulator, RestTemplate restTemplate) {
        this.failureSimulator = failureSimulator;
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
    }

    /**
     * Initializes the replica list and status maps after bean construction.
     */
    @jakarta.annotation.PostConstruct
    public void initializeReplicas() {
        this.replicaUrls = Arrays.asList(replicaUrlsConfig.split(","));
        for (int i = 0; i < replicaUrls.size(); i++) {
            String url = replicaUrls.get(i).trim();
            replicaStatusMap.put(url, new ReplicaStatus(i, url, false, LocalDateTime.now()));
            consecutiveFailures.put(url, 0);
            consecutiveSuccesses.put(url, recoveryThreshold);
        }
        logger.info("Initialized {} replicas for fault recovery monitoring", replicaUrls.size());
    }

    /**
     * Periodically checks the health of all replicas and manages failover.
     */
    @Scheduled(fixedDelayString = "${fault.recovery.heartbeat.interval:10000}")
    public void performHeartbeatAndFaultRecovery() {
        logger.debug("Starting heartbeat check for {} replicas", replicaUrls.size());

        boolean anyHealthy = false;
        List<String> healthyReplicas = new ArrayList<>();

        for (int i = 0; i < replicaUrls.size(); i++) {
            String url = replicaUrls.get(i);
            ReplicaStatus status = replicaStatusMap.get(url);

            boolean currentHealth = checkReplicaHealth(url);
            boolean previousHealth = status.isHealthy();

            status.setHealthy(currentHealth);
            status.setLastChecked(LocalDateTime.now());

            if (currentHealth) {
                anyHealthy = true;
                healthyReplicas.add(url);
                handleReplicaRecovery(url, previousHealth);
            } else {
                handleReplicaFailure(url, previousHealth);
            }
        }

        updateSystemStatus(anyHealthy);
        performFailoverIfNecessary(healthyReplicas);

        logger.debug("Heartbeat check completed. System operational: {}, Healthy replicas: {}",
                systemOperational, healthyReplicas.size());
    }

    /**
     * Checks the health of a single replica, using simulation if enabled.
     */
    private boolean checkReplicaHealth(String url) {
        if (failureSimulator.shouldSimulateFailure(url)) {
            logger.debug("Simulating failure for replica: {}", url);
            return false;
        }

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            logger.debug("Health check failed for {}: {}", url, e.getMessage());
            return false;
        }
    }

    /**
     * Handles logic for when a replica fails a health check.
     */
    private void handleReplicaFailure(String url, boolean previousHealth) {
        int failures = consecutiveFailures.merge(url, 1, Integer::sum);
        consecutiveSuccesses.put(url, 0);

        if (previousHealth && failures >= failureThreshold) {
            logger.warn("FAULT DETECTED: Replica {} has failed {} consecutive health checks - marking as DOWN",
                    url, failures);
            recordFailoverEvent(url, "FAILURE", "Replica marked as DOWN after " + failures + " consecutive failures");
            lastFailoverTime.put(url, LocalDateTime.now());
        } else if (failures == 1 && previousHealth) {
            logger.info("Replica {} failed health check (attempt 1/{} threshold)", url, failureThreshold);
        }
    }

    /**
     * Handles logic for when a replica recovers after being down.
     */
    private void handleReplicaRecovery(String url, boolean previousHealth) {
        int successes = consecutiveSuccesses.merge(url, 1, Integer::sum);
        consecutiveFailures.put(url, 0);

        if (!previousHealth && successes >= recoveryThreshold) {
            logger.info(
                    "RECOVERY DETECTED: Replica {} has recovered after {} consecutive successful checks - marking as UP",
                    url, successes);
            recordFailoverEvent(url, "RECOVERY", "Replica marked as UP after " + successes + " consecutive successes");
        } else if (successes == 1 && !previousHealth) {
            logger.info("Replica {} passed health check (attempt 1/{} threshold)", url, recoveryThreshold);
        } else if (previousHealth) {
            if (successes < recoveryThreshold) {
                consecutiveSuccesses.put(url, recoveryThreshold);
            }
        }
    }

    /**
     * Updates the overall system status based on replica health.
     */
    private void updateSystemStatus(boolean anyHealthy) {
        boolean previousSystemStatus = systemOperational;
        systemOperational = anyHealthy;

        if (previousSystemStatus != systemOperational) {
            if (systemOperational) {
                logger.info("SYSTEM RECOVERY: At least one replica is healthy - system is operational");
                recordFailoverEvent("SYSTEM", "SYSTEM_RECOVERY", "System restored to operational status");
            } else {
                logger.error("SYSTEM FAILURE: All replicas are down - system is degraded");
                recordFailoverEvent("SYSTEM", "SYSTEM_FAILURE", "All replicas failed - system degraded");
            }
        }
    }

    /**
     * Switches primary replica if the current one is unhealthy.
     */
    private void performFailoverIfNecessary(List<String> healthyReplicas) {
        if (healthyReplicas.isEmpty()) {
            logger.error("CRITICAL: No healthy replicas available for failover");
            return;
        }

        String currentPrimaryUrl = getCurrentPrimaryReplica();
        boolean currentPrimaryHealthy = healthyReplicas.contains(currentPrimaryUrl);

        if (!currentPrimaryHealthy) {
            String newPrimaryUrl = healthyReplicas.get(0);
            int newPrimaryIndex = replicaUrls.indexOf(newPrimaryUrl);

            if (newPrimaryIndex != -1 && newPrimaryIndex != primaryReplicaIndex.get()) {
                int oldPrimaryIndex = primaryReplicaIndex.getAndSet(newPrimaryIndex);
                logger.warn("FAILOVER EXECUTED: Primary replica switched from {} (index {}) to {} (index {})",
                        currentPrimaryUrl, oldPrimaryIndex, newPrimaryUrl, newPrimaryIndex);
                recordFailoverEvent(newPrimaryUrl, "FAILOVER",
                        String.format("Primary replica changed from %s to %s", currentPrimaryUrl, newPrimaryUrl));
            }
        }
    }

    /**
     * Records a failover or recovery event.
     */
    private void recordFailoverEvent(String replicaUrl, String eventType, String description) {
        FailoverEvent event = new FailoverEvent(
                LocalDateTime.now(),
                replicaUrl,
                eventType,
                description);

        synchronized (failoverHistory) {
            failoverHistory.add(event);
            if (failoverHistory.size() > 100) {
                failoverHistory.remove(0);
            }
        }
    }

    /**
     * Returns the URL of the current primary replica.
     */
    public String getCurrentPrimaryReplica() {
        int index = primaryReplicaIndex.get();
        return (index >= 0 && index < replicaUrls.size()) ? replicaUrls.get(index) : replicaUrls.get(0);
    }

    /**
     * Returns an array of booleans indicating health of each replica.
     */
    public boolean[] getReplicaHealth() {
        boolean[] health = new boolean[replicaUrls.size()];
        for (int i = 0; i < replicaUrls.size(); i++) {
            String url = replicaUrls.get(i);
            ReplicaStatus status = replicaStatusMap.get(url);
            health[i] = status != null && status.isHealthy() &&
                    consecutiveSuccesses.getOrDefault(url, 0) >= recoveryThreshold;
        }
        return health;
    }

    /**
     * Returns a list of all replica URLs.
     */
    public List<String> getReplicaUrls() {
        return new ArrayList<>(replicaUrls);
    }

    /**
     * Returns true if the system is operational (at least one healthy replica).
     */
    public boolean isSystemOperational() {
        return systemOperational;
    }

    /**
     * Returns a list of healthy replica URLs.
     */
    public List<String> getHealthyReplicas() {
        return replicaUrls.stream()
                .filter(url -> {
                    ReplicaStatus status = replicaStatusMap.get(url);
                    return status != null && status.isHealthy() &&
                            consecutiveSuccesses.getOrDefault(url, 0) >= recoveryThreshold;
                })
                .toList();
    }

    /**
     * Returns a detailed status map for all replicas and recent events.
     */
    public Map<String, Object> getDetailedStatus() {
        Map<String, Object> status = new HashMap<>();

        List<String> healthyReplicas = getHealthyReplicas();
        status.put("system_status", systemOperational ? "operational" : "degraded");
        status.put("healthy_replicas", healthyReplicas.size());
        status.put("total_replicas", replicaUrls.size());
        status.put("primary_replica", getCurrentPrimaryReplica());
        status.put("primary_replica_index", primaryReplicaIndex.get());

        Map<String, Object> replicas = new HashMap<>();
        for (int i = 0; i < replicaUrls.size(); i++) {
            String url = replicaUrls.get(i);
            ReplicaStatus replicaStatus = replicaStatusMap.get(url);

            Map<String, Object> replicaInfo = new HashMap<>();
            boolean isHealthy = healthyReplicas.contains(url);
            replicaInfo.put("status", isHealthy ? "UP" : "DOWN");
            replicaInfo.put("url", url);
            replicaInfo.put("is_primary", i == primaryReplicaIndex.get());
            replicaInfo.put("consecutive_failures", consecutiveFailures.getOrDefault(url, 0));
            replicaInfo.put("consecutive_successes", consecutiveSuccesses.getOrDefault(url, 0));
            replicaInfo.put("last_checked",
                    replicaStatus.getLastChecked().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            LocalDateTime lastFailover = lastFailoverTime.get(url);
            if (lastFailover != null) {
                replicaInfo.put("last_failover", lastFailover.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }

            replicas.put("replica_" + i, replicaInfo);
        }
        status.put("replicas", replicas);

        synchronized (failoverHistory) {
            List<Map<String, Object>> recentEvents = failoverHistory.stream()
                    .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                    .limit(10)
                    .map(event -> {
                        Map<String, Object> eventMap = new HashMap<>();
                        eventMap.put("timestamp", event.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        eventMap.put("replica_url", event.getReplicaUrl());
                        eventMap.put("event_type", event.getEventType());
                        eventMap.put("description", event.getDescription());
                        return eventMap;
                    })
                    .toList();
            status.put("recent_failover_events", recentEvents);
        }

        return status;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public int getRecoveryThreshold() {
        return recoveryThreshold;
    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    /**
     * Triggers a manual heartbeat check (for testing).
     */
    public void checkReplicas() {
        performHeartbeatAndFaultRecovery();
    }

    /**
     * Holds health and last checked time for a replica.
     */
    private static class ReplicaStatus {
        private boolean healthy;
        private LocalDateTime lastChecked;

        public ReplicaStatus(int index, String url, boolean healthy, LocalDateTime lastChecked) {
            this.healthy = healthy;
            this.lastChecked = lastChecked;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }

        public LocalDateTime getLastChecked() {
            return lastChecked;
        }

        public void setLastChecked(LocalDateTime lastChecked) {
            this.lastChecked = lastChecked;
        }
    }

    /**
     * Represents a failover or recovery event.
     */
    private static class FailoverEvent {
        private final LocalDateTime timestamp;
        private final String replicaUrl;
        private final String eventType;
        private final String description;

        public FailoverEvent(LocalDateTime timestamp, String replicaUrl, String eventType, String description) {
            this.timestamp = timestamp;
            this.replicaUrl = replicaUrl;
            this.eventType = eventType;
            this.description = description;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public String getReplicaUrl() {
            return replicaUrl;
        }

        public String getEventType() {
            return eventType;
        }

        public String getDescription() {
            return description;
        }
    }
}