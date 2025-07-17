package com.swen755.authentication_and_authorization.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class HeartbeatService {
    private static final Logger logger = LoggerFactory.getLogger(HeartbeatService.class);
    private final RestTemplate restTemplate = new RestTemplate();

    // TODO: Replace with dynamic discovery in Swarm, or config
    private final List<String> replicaUrls = Arrays.asList(
            "http://spring-boot-app:8080/health",
            "http://spring-boot-app:8081/health" // Example, adjust as needed
    );

    // Store health status
    private final boolean[] healthy = new boolean[replicaUrls.size()];

    // Periodically check health of all replicas
    @Scheduled(fixedDelay = 10000) // every 10 seconds
    public void checkReplicas() {
        for (int i = 0; i < replicaUrls.size(); i++) {
            String url = replicaUrls.get(i);
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                boolean currentHealth = response.getStatusCode().is2xxSuccessful();
                
                // Log status changes
                if (healthy[i] != currentHealth) {
                    if (currentHealth) {
                        logger.info("Replica {} at {} is now UP", i, url);
                    } else {
                        logger.warn("Replica {} at {} is now DOWN", i, url);
                    }
                }
                
                healthy[i] = currentHealth;
                logger.debug("Replica {} at {} is {}", i, url, healthy[i] ? "UP" : "DOWN");
            } catch (Exception e) {
                // Log status changes
                if (healthy[i]) {
                    logger.warn("Replica {} at {} is now DOWN: {}", i, url, e.getMessage());
                }
                
                healthy[i] = false;
                logger.debug("Replica {} at {} is DOWN: {}", i, url, e.getMessage());
            }
        }
    }

    // Expose health status for controller
    public boolean[] getReplicaHealth() {
        return healthy;
    }
    
    // Get replica URLs for reference
    public List<String> getReplicaUrls() {
        return replicaUrls;
    }
}