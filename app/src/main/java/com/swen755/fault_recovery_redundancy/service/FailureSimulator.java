package com.swen755.fault_recovery_redundancy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FailureSimulator {
    private static final Logger logger = LoggerFactory.getLogger(FailureSimulator.class);
    private final Random random = new Random();

    // Whether simulation is enabled (from properties)
    @Value("${fault.recovery.simulation.enabled:false}")
    private boolean simulationEnabled;

    // Probability that a failure will be simulated on a heartbeat
    @Value("${fault.recovery.simulation.failure.probability:0.1}")
    private double failureProbability;

    // Probability that a failed replica will recover on a heartbeat
    @Value("${fault.recovery.simulation.recovery.probability:0.3}")
    private double recoveryProbability;

    // Tracks simulated failure state for each replica
    private final Map<String, Boolean> simulatedFailures = new ConcurrentHashMap<>();

    /**
     * Determines if a failure should be simulated for the given replica.
     * Handles both random failure and recovery based on probabilities.
     */
    public boolean shouldSimulateFailure(String replicaUrl) {
        if (!simulationEnabled) {
            return false;
        }

        boolean currentlyFailed = simulatedFailures.getOrDefault(replicaUrl, false);

        if (currentlyFailed) {
            // Attempt recovery with configured probability
            if (random.nextDouble() < recoveryProbability) {
                simulatedFailures.put(replicaUrl, false);
                logger.info("SIMULATION: Replica {} recovered from simulated failure", replicaUrl);
                return false;
            }
            return true;
        } else {
            // Attempt to simulate a new failure
            if (random.nextDouble() < failureProbability) {
                simulatedFailures.put(replicaUrl, true);
                logger.warn("SIMULATION: Replica {} entering simulated failure state", replicaUrl);
                return true;
            }
            return false;
        }
    }

    /**
     * Manually set a replica to failed state.
     */
    public void simulateFailure(String replicaUrl) {
        simulatedFailures.put(replicaUrl, true);
        logger.warn("MANUAL SIMULATION: Replica {} manually set to failed state", replicaUrl);
    }

    /**
     * Manually recover a replica from failure.
     */
    public void recoverReplica(String replicaUrl) {
        simulatedFailures.put(replicaUrl, false);
        logger.info("MANUAL SIMULATION: Replica {} manually recovered from failure", replicaUrl);
    }

    /**
     * Returns a copy of the current simulation status for all replicas.
     */
    public Map<String, Boolean> getSimulationStatus() {
        return new ConcurrentHashMap<>(simulatedFailures);
    }

    /**
     * Enable or disable simulation. Clears failures if disabling.
     */
    public void setSimulationEnabled(boolean enabled) {
        this.simulationEnabled = enabled;
        if (enabled) {
            logger.info("SIMULATION: Failure simulation enabled");
        } else {
            logger.info("SIMULATION: Failure simulation disabled");
            simulatedFailures.clear();
        }
    }

    /**
     * Returns whether simulation is enabled.
     */
    public boolean isSimulationEnabled() {
        return simulationEnabled;
    }

    /**
     * Clears all simulated failures.
     */
    public void resetAllFailures() {
        simulatedFailures.clear();
        logger.info("SIMULATION: All simulated failures cleared");
    }
}