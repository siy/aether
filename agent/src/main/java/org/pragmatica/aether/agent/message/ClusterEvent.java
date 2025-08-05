package org.pragmatica.aether.agent.message;

import java.util.Map;
import java.util.UUID;

/**
 * Represents significant cluster-wide events that require immediate agent attention.
 * These events indicate changes in cluster state, topology, or health that may
 * require automated response or human intervention.
 * 
 * Cluster events are processed with higher priority than routine telemetry and
 * can trigger immediate analysis and recommendation generation.
 */
public record ClusterEvent(
    String messageId,
    long timestamp,
    MessagePriority priority,
    String sourceNodeId,
    ClusterEventType eventType,
    String description,
    Map<String, Object> eventData,
    Severity severity
) implements AgentMessage {
    
    /**
     * Creates a new cluster event with generated message ID and current timestamp.
     */
    public static ClusterEvent create(
        String sourceNodeId,
        ClusterEventType eventType,
        String description,
        Map<String, Object> eventData,
        Severity severity
    ) {
        // Map severity to message priority for processing order
        MessagePriority priority = switch (severity) {
            case CRITICAL -> MessagePriority.CRITICAL;
            case HIGH -> MessagePriority.HIGH;
            case MEDIUM -> MessagePriority.NORMAL;
            case LOW, INFO -> MessagePriority.LOW;
        };
        
        return new ClusterEvent(
            UUID.randomUUID().toString(),
            System.currentTimeMillis(),
            priority,
            sourceNodeId,
            eventType,
            description,
            eventData,
            severity
        );
    }
    
    /**
     * Types of cluster events that can occur.
     */
    public enum ClusterEventType {
        /** Node joining or leaving the cluster */
        TOPOLOGY_CHANGE,
        /** Consensus leader election or change */
        LEADERSHIP_CHANGE,
        /** Quorum establishment or loss */
        QUORUM_CHANGE,
        /** Slice deployment, scaling, or removal */
        DEPLOYMENT_CHANGE,
        /** Network partition or connectivity issues */
        NETWORK_PARTITION,
        /** Resource exhaustion (CPU, memory, disk) */
        RESOURCE_EXHAUSTION,
        /** Performance degradation detected */
        PERFORMANCE_DEGRADATION,
        /** Security-related events */
        SECURITY_EVENT,
        /** Configuration changes */
        CONFIGURATION_CHANGE,
        /** System errors or failures */
        SYSTEM_ERROR,
        /** Health check failures */
        HEALTH_CHECK_FAILURE,
        /** Custom application events */
        APPLICATION_EVENT
    }
    
    /**
     * Severity levels for cluster events.
     */
    public enum Severity {
        /** Immediate attention required, system at risk */
        CRITICAL,
        /** Significant issue requiring prompt attention */
        HIGH,
        /** Notable event that should be monitored */
        MEDIUM,
        /** Minor event of limited impact */
        LOW,
        /** Informational event for audit/tracking */
        INFO
    }
    
    /**
     * Checks if this event requires immediate processing.
     */
    public boolean requiresImmediateAttention() {
        return severity == Severity.CRITICAL || severity == Severity.HIGH;
    }
    
    /**
     * Checks if this event is related to system health.
     */
    public boolean isHealthRelated() {
        return eventType == ClusterEventType.RESOURCE_EXHAUSTION ||
               eventType == ClusterEventType.PERFORMANCE_DEGRADATION ||
               eventType == ClusterEventType.HEALTH_CHECK_FAILURE ||
               eventType == ClusterEventType.SYSTEM_ERROR;
    }
    
    /**
     * Checks if this event is related to cluster topology.
     */
    public boolean isTopologyRelated() {
        return eventType == ClusterEventType.TOPOLOGY_CHANGE ||
               eventType == ClusterEventType.LEADERSHIP_CHANGE ||
               eventType == ClusterEventType.QUORUM_CHANGE ||
               eventType == ClusterEventType.NETWORK_PARTITION;
    }
}