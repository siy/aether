package org.pragmatica.aether.agent.message;

/**
 * Sealed hierarchy of messages used by the Aether AI Agent system.
 * Provides compile-time safety and pattern matching completeness for all agent-related messages.
 * 
 * This hierarchy ensures that all agent messages can be processed exhaustively without 
 * missing cases, enabling reliable message routing and handling throughout the system.
 * 
 * Note: This interface does not extend the Pragmatica Message interface directly due to 
 * sealed class restrictions, but provides the same functionality for agent-specific messages.
 */
public sealed interface AgentMessage
    permits SliceTelemetryBatch, ClusterEvent, StateTransition, AgentRecommendation {
    
    /**
     * Returns the unique message identifier for routing and correlation purposes.
     * This identifier is used to track message flow through the system and correlate
     * related messages across different components.
     */
    String messageId();
    
    /**
     * Returns the timestamp when this message was created.
     * Used for message ordering, age-based filtering, and temporal analysis.
     */
    long timestamp();
    
    /**
     * Returns the priority level of this message for processing order.
     * Higher priority messages are processed before lower priority ones,
     * enabling critical events to be handled immediately.
     */
    MessagePriority priority();
    
    /**
     * Priority levels for agent messages, from highest to lowest urgency.
     */
    enum MessagePriority {
        /** Critical events requiring immediate attention (e.g., system failures) */
        CRITICAL,
        /** Important events that should be processed quickly (e.g., performance degradation) */
        HIGH,
        /** Normal operational events (e.g., routine scaling recommendations) */
        NORMAL,
        /** Background information (e.g., statistical telemetry) */
        LOW
    }
}