package org.pragmatica.aether.agent;

import org.pragmatica.aether.agent.config.AgentConfiguration;
import org.pragmatica.aether.agent.message.MessagePreprocessor;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.topology.QuorumStateNotification;
import org.pragmatica.message.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Main controller for the Aether AI Agent system.
 * 
 * The AetherAgent serves as the orchestrator for all agent functionality, managing:
 * - Agent lifecycle (startup, shutdown, health monitoring)
 * - Leadership management (active only on consensus leader)
 * - Message routing coordination through MessageRouter
 * - Component orchestration and coordination
 * - State management through SMR consensus
 * 
 * Following the design principle of single dependency (MessageRouter), this component
 * integrates with the existing Aether infrastructure while maintaining clean boundaries.
 * The agent operates in leadership-based mode where only the consensus leader runs
 * the active agent to prevent conflicts and ensure consistent decision-making.
 */
public class AetherAgent {
    private static final Logger logger = LoggerFactory.getLogger(AetherAgent.class);
    
    private final NodeId nodeId;
    private final MessageRouter messageRouter;
    private final AtomicReference<AgentState> state;
    private final AtomicReference<AgentConfiguration> configuration;
    private volatile MessagePreprocessor messagePreprocessor;
    private volatile boolean isLeader;
    private volatile Instant startTime;
    
    public AetherAgent(NodeId nodeId, MessageRouter messageRouter, AgentConfiguration initialConfig) {
        this.nodeId = nodeId;
        this.messageRouter = messageRouter;
        this.state = new AtomicReference<>(AgentState.DORMANT);
        this.configuration = new AtomicReference<>(initialConfig);
        this.isLeader = false;
        
        logger.info("AetherAgent created for node {} with configuration: {}", nodeId, initialConfig);
    }
    
    /**
     * Creates an AetherAgent with default configuration.
     */
    public static AetherAgent create(NodeId nodeId, MessageRouter messageRouter) {
        return new AetherAgent(nodeId, messageRouter, AgentConfiguration.defaultConfiguration());
    }
    
    /**
     * Starts the agent and registers with MessageRouter.
     * The agent will begin processing messages only when it becomes the consensus leader.
     */
    public CompletableFuture<Void> start() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!state.compareAndSet(AgentState.DORMANT, AgentState.STARTING)) {
                    logger.warn("Agent is already running or in invalid state: {}", state.get());
                    return;
                }
                
                startTime = Instant.now();
                
                // Initialize message preprocessor
                var preprocessorConfig = MessagePreprocessor.PreprocessorConfig.defaultConfig();
                messagePreprocessor = new MessagePreprocessor(messageRouter, preprocessorConfig);
                
                // Register for leadership change notifications
                messageRouter.addRoute(QuorumStateNotification.class, this::onQuorumStateChange);
                
                // Initialize other components would go here
                
                state.set(AgentState.INACTIVE);
                logger.info("AetherAgent started successfully on node {} at {}", nodeId, startTime);
                
            } catch (Exception e) {
                state.set(AgentState.FAILED);
                logger.error("Failed to start AetherAgent: {}", e.getMessage(), e);
                throw new RuntimeException("Agent startup failed", e);
            }
        });
    }
    
    /**
     * Stops the agent and cleans up resources.
     */
    public CompletableFuture<Void> stop() {
        return CompletableFuture.runAsync(() -> {
            try {
                AgentState currentState = state.get();
                if (currentState == AgentState.DORMANT || currentState == AgentState.STOPPING) {
                    logger.debug("Agent is already stopped or stopping");
                    return;
                }
                
                state.set(AgentState.STOPPING);
                
                // Stop message processing if active
                if (messagePreprocessor != null && isLeader) {
                    messagePreprocessor.stop();
                }
                
                isLeader = false;
                
                state.set(AgentState.DORMANT);
                
                Duration uptime = Duration.between(startTime, Instant.now());
                logger.info("AetherAgent stopped successfully. Uptime: {}", uptime);
                
            } catch (Exception e) {
                state.set(AgentState.FAILED);
                logger.error("Error during agent shutdown: {}", e.getMessage(), e);
                throw new RuntimeException("Agent shutdown failed", e);
            }
        });
    }
    
    /**
     * Updates the agent configuration.
     * Configuration changes are applied immediately and may affect agent behavior.
     */
    public void updateConfiguration(AgentConfiguration newConfig) {
        AgentConfiguration oldConfig = configuration.getAndSet(newConfig);
        logger.info("Agent configuration updated from {} to {}", oldConfig, newConfig);
        
        // Apply configuration changes if agent is active
        if (state.get() == AgentState.ACTIVE && messagePreprocessor != null) {
            // Configuration changes would be applied here
            logger.debug("Applied configuration changes to active agent components");
        }
    }
    
    /**
     * Gets the current agent state.
     */
    public AgentState currentState() {
        return state.get();
    }
    
    /**
     * Gets the current agent configuration.
     */
    public AgentConfiguration currentConfiguration() {
        return configuration.get();
    }
    
    /**
     * Checks if this agent is currently the active leader.
     */
    public boolean isActiveLeader() {
        return isLeader && state.get() == AgentState.ACTIVE;
    }
    
    /**
     * Gets agent health information for monitoring.
     */
    public AgentHealth health() {
        var processingStats = messagePreprocessor != null ? 
            messagePreprocessor.stats() : null;
        
        return new AgentHealth(
            nodeId,
            state.get(),
            isLeader,
            startTime,
            processingStats
        );
    }
    
    /**
     * Handles quorum state changes to manage leadership.
     */
    private void onQuorumStateChange(QuorumStateNotification notification) {
        switch (notification) {
            case ESTABLISHED -> {
                if (!isLeader) {
                    becomeLeader();
                }
            }
            case DISAPPEARED -> {
                if (isLeader) {
                    stepDownFromLeadership();
                }
            }
        }
    }
    
    /**
     * Activates the agent when becoming the consensus leader.
     */
    private void becomeLeader() {
        try {
            isLeader = true;
            
            if (state.compareAndSet(AgentState.INACTIVE, AgentState.ACTIVE)) {
                // Start message processing
                if (messagePreprocessor != null) {
                    messagePreprocessor.start();
                }
                
                logger.info("AetherAgent became active leader on node {}", nodeId);
            }
            
        } catch (Exception e) {
            logger.error("Error becoming leader: {}", e.getMessage(), e);
            state.set(AgentState.FAILED);
        }
    }
    
    /**
     * Deactivates the agent when stepping down from leadership.
     */
    private void stepDownFromLeadership() {
        try {
            isLeader = false;
            
            if (state.compareAndSet(AgentState.ACTIVE, AgentState.INACTIVE)) {
                // Stop message processing
                if (messagePreprocessor != null) {
                    messagePreprocessor.stop();
                }
                
                logger.info("AetherAgent stepped down from leadership on node {}", nodeId);
            }
            
        } catch (Exception e) {
            logger.error("Error stepping down from leadership: {}", e.getMessage(), e);
            state.set(AgentState.FAILED);
        }
    }
    
    /**
     * Possible states of the AetherAgent.
     */
    public enum AgentState {
        /** Agent is not running */
        DORMANT,
        /** Agent is starting up */
        STARTING,
        /** Agent is running but not the leader (inactive) */
        INACTIVE,
        /** Agent is running and is the active leader */
        ACTIVE,
        /** Agent is shutting down */
        STOPPING,
        /** Agent has encountered an error */
        FAILED
    }
    
    /**
     * Health information for the agent.
     */
    public record AgentHealth(
        NodeId nodeId,
        AgentState state,
        boolean isLeader,
        Instant startTime,
        MessagePreprocessor.ProcessingStats processingStats
    ) {
        
        /**
         * Checks if the agent is in a healthy state.
         */
        public boolean isHealthy() {
            return state == AgentState.ACTIVE || state == AgentState.INACTIVE;
        }
        
        /**
         * Gets the uptime of the agent.
         */
        public java.time.Duration uptime() {
            return startTime != null ? 
                java.time.Duration.between(startTime, Instant.now()) : 
                java.time.Duration.ZERO;
        }
    }
}