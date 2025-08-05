package org.pragmatica.aether.agent.message;

import java.util.Map;
import java.util.UUID;

/**
 * Represents a state transition within the Aether system that may trigger agent analysis.
 * State transitions capture changes in system state, component status, or operational
 * conditions that the agent should be aware of for pattern recognition and learning.
 * 
 * These messages are essential for the agent's understanding of system behavior and
 * help build temporal patterns for predictive analysis and recommendation generation.
 */
public record StateTransition(
    String messageId,
    long timestamp,
    MessagePriority priority,
    String sourceComponent,
    TransitionType transitionType,
    String fromState,
    String toState,
    Map<String, Object> transitionContext,
    String reason
) implements AgentMessage {
    
    /**
     * Creates a new state transition with generated message ID and current timestamp.
     */
    public static StateTransition create(
        String sourceComponent,
        TransitionType transitionType,
        String fromState,
        String toState,
        Map<String, Object> transitionContext,
        String reason
    ) {
        // Determine priority based on transition type
        MessagePriority priority = switch (transitionType) {
            case SLICE_FAILURE, NODE_FAILURE, CLUSTER_FAILURE -> MessagePriority.CRITICAL;
            case SLICE_RECOVERY, NODE_RECOVERY, PERFORMANCE_CHANGE -> MessagePriority.HIGH;
            case SLICE_SCALING, CONFIGURATION_CHANGE, DEPLOYMENT_CHANGE -> MessagePriority.NORMAL;
            case SLICE_STARTUP, SLICE_SHUTDOWN, ROUTINE_MAINTENANCE -> MessagePriority.LOW;
            case NODE_STARTUP, NODE_SHUTDOWN, CLUSTER_FORMATION, CLUSTER_SPLIT, 
                 CLUSTER_MERGE, RESOURCE_STATE_CHANGE, EMERGENCY_MAINTENANCE -> MessagePriority.NORMAL;
        };
        
        return new StateTransition(
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            priority,
            sourceComponent,
            transitionType,
            fromState,
            toState,
            transitionContext,
            reason
        );
    }
    
    /**
     * Types of state transitions that can occur in the system.
     */
    public enum TransitionType {
        /** Slice-level state changes */
        SLICE_STARTUP,
        SLICE_SHUTDOWN,
        SLICE_SCALING,
        SLICE_FAILURE,
        SLICE_RECOVERY,
        
        /** Node-level state changes */
        NODE_STARTUP,
        NODE_SHUTDOWN,
        NODE_FAILURE,
        NODE_RECOVERY,
        
        /** Cluster-level state changes */
        CLUSTER_FORMATION,
        CLUSTER_SPLIT,
        CLUSTER_MERGE,
        CLUSTER_FAILURE,
        
        /** Performance-related transitions */
        PERFORMANCE_CHANGE,
        RESOURCE_STATE_CHANGE,
        
        /** Configuration and deployment transitions */
        CONFIGURATION_CHANGE,
        DEPLOYMENT_CHANGE,
        
        /** Maintenance operations */
        ROUTINE_MAINTENANCE,
        EMERGENCY_MAINTENANCE
    }
    
    /**
     * Checks if this transition represents a failure scenario.
     */
    public boolean isFailureTransition() {
        return transitionType == TransitionType.SLICE_FAILURE ||
               transitionType == TransitionType.NODE_FAILURE ||
               transitionType == TransitionType.CLUSTER_FAILURE;
    }
    
    /**
     * Checks if this transition represents a recovery scenario.
     */
    public boolean isRecoveryTransition() {
        return transitionType == TransitionType.SLICE_RECOVERY ||
               transitionType == TransitionType.NODE_RECOVERY ||
               transitionType == TransitionType.CLUSTER_MERGE;
    }
    
    /**
     * Checks if this transition is related to scaling operations.
     */
    public boolean isScalingTransition() {
        return transitionType == TransitionType.SLICE_SCALING ||
               (transitionType == TransitionType.RESOURCE_STATE_CHANGE &&
                transitionContext.containsKey("scaling"));
    }
    
    /**
     * Gets the elapsed time since this transition occurred.
     */
    public long getAgeMillis() {
        return System.currentTimeMillis() - timestamp;
    }
    
    /**
     * Creates a string representation suitable for logging and analysis.
     */
    public String getTransitionSummary() {
        return String.format("%s: %s -> %s (%s)", 
            sourceComponent, fromState, toState, reason);
    }
}