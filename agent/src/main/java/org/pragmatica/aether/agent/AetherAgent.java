package org.pragmatica.aether.agent;

import org.pragmatica.aether.agent.config.AgentConfiguration;
import org.pragmatica.aether.agent.message.*;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.topology.QuorumStateNotification;
import org.pragmatica.message.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.lang.Unit;
import org.pragmatica.lang.Option;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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
    private final ConcurrentHashMap<Class<? extends AgentMessage>, Consumer<AgentMessage>> messageHandlers;
    private final AtomicLong processedMessageCount;
    private final AtomicLong droppedMessageCount;
    private volatile boolean isLeader;
    private volatile boolean messageProcessingActive;
    private volatile Instant startTime;
    
    public AetherAgent(NodeId nodeId, MessageRouter messageRouter, AgentConfiguration initialConfig) {
        this.nodeId = nodeId;
        this.messageRouter = messageRouter;
        this.state = new AtomicReference<>(AgentState.DORMANT);
        this.configuration = new AtomicReference<>(initialConfig);
        this.messageHandlers = new ConcurrentHashMap<>();
        this.processedMessageCount = new AtomicLong(0);
        this.droppedMessageCount = new AtomicLong(0);
        this.isLeader = false;
        this.messageProcessingActive = false;
        
        registerDefaultMessageHandlers();
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
    public Promise<Unit> start() {
        return Promise.lift(Causes::fromThrowable, () -> {
            if (!state.compareAndSet(AgentState.DORMANT, AgentState.STARTING)) {
                logger.warn("Agent is already running or in invalid state: {}", state.get());
                return Unit.unit();
            }
            
            startTime = Instant.now();
            
            // Register for leadership change notifications
            messageRouter.addRoute(QuorumStateNotification.class, this::onQuorumStateChange);
            
            // Initialize other components would go here
            
            state.set(AgentState.INACTIVE);
            logger.info("AetherAgent started successfully on node {} at {}", nodeId, startTime);
            return Unit.unit();
        }).recover(error -> {
            state.set(AgentState.FAILED);
            logger.error("Failed to start AetherAgent: {}", error.message());
            return Unit.unit();
        });
    }
    
    /**
     * Stops the agent and cleans up resources.
     */
    public Promise<Unit> stop() {
        return Promise.lift(Causes::fromThrowable, () -> {
            AgentState currentState = state.get();
            if (currentState == AgentState.DORMANT || currentState == AgentState.STOPPING) {
                logger.debug("Agent is already stopped or stopping");
                return Unit.unit();
            }
            
            state.set(AgentState.STOPPING);
            
            // Stop message processing if active
            if (messageProcessingActive) {
                stopMessageProcessing();
            }
            
            isLeader = false;
            
            state.set(AgentState.DORMANT);
            
            Duration uptime = Duration.between(startTime, Instant.now());
            logger.info("AetherAgent stopped successfully. Uptime: {}", uptime);
            return Unit.unit();
        }).recover(error -> {
            state.set(AgentState.FAILED);
            logger.error("Error during agent shutdown: {}", error.message());
            return Unit.unit();
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
        if (state.get() == AgentState.ACTIVE) {
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
        var processingStats = Option.some(new ProcessingStats(
            processedMessageCount.get(),
            droppedMessageCount.get(),
            messageProcessingActive
        ));
        
        return new AgentHealth(
            nodeId,
            state.get(),
            isLeader,
            Option.option(startTime),
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
                startMessageProcessing();
                
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
                stopMessageProcessing();
                
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
     * Processes an agent message directly without complex batching.
     * Only processes messages when the agent is active and is the leader.
     */
    public void processMessage(AgentMessage message) {
        if (!messageProcessingActive) {
            logger.debug("Dropping message - agent not processing: {}", message.messageId());
            droppedMessageCount.incrementAndGet();
            return;
        }
        
        try {
            @SuppressWarnings("unchecked")
            var handler = (Consumer<AgentMessage>) messageHandlers.get(message.getClass());
            
            if (handler != null) {
                handler.accept(message);
                processedMessageCount.incrementAndGet();
            } else {
                logger.warn("No handler found for message type: {}", message.getClass().getSimpleName());
                droppedMessageCount.incrementAndGet();
            }
            
        } catch (Exception e) {
            logger.error("Error processing message {}: {}", message.messageId(), e.getMessage(), e);
            droppedMessageCount.incrementAndGet();
        }
    }
    
    /**
     * Registers a custom message handler for a specific message type.
     */
    public <T extends AgentMessage> void registerMessageHandler(Class<T> messageType, Consumer<T> handler) {
        @SuppressWarnings("unchecked")
        Consumer<AgentMessage> wrappedHandler = (Consumer<AgentMessage>) handler;
        messageHandlers.put(messageType, wrappedHandler);
        logger.debug("Registered message handler for type: {}", messageType.getSimpleName());
    }
    
    private void registerDefaultMessageHandlers() {
        registerMessageHandler(SliceTelemetryBatch.class, this::handleTelemetryBatch);
        registerMessageHandler(ClusterEvent.class, this::handleClusterEvent);
        registerMessageHandler(StateTransition.class, this::handleStateTransition);
        registerMessageHandler(AgentRecommendation.class, this::handleAgentRecommendation);
    }
    
    private void startMessageProcessing() {
        messageProcessingActive = true;
        logger.debug("Message processing started on node {}", nodeId);
    }
    
    private void stopMessageProcessing() {
        messageProcessingActive = false;
        logger.debug("Message processing stopped on node {}", nodeId);
    }
    
    // Default message handlers - simplified without complex batching
    private void handleTelemetryBatch(SliceTelemetryBatch batch) {
        logger.debug("Processing telemetry batch from node {} with {} slice metrics", 
                    batch.nodeId(), batch.sliceMetrics().size());
        // Additional telemetry-specific processing can be added here
    }
    
    private void handleClusterEvent(ClusterEvent event) {
        logger.info("Processing cluster event: {} from node {} (severity: {})", 
                   event.eventType(), event.sourceNodeId(), event.severity());
        // Additional cluster event processing can be added here
    }
    
    private void handleStateTransition(StateTransition transition) {
        logger.debug("Processing state transition: {} - {} -> {}", 
                    transition.sourceComponent(), transition.fromState(), transition.toState());
        // Additional state transition processing can be added here
    }
    
    private void handleAgentRecommendation(AgentRecommendation recommendation) {
        logger.info("Processing agent recommendation: {} (confidence: {:.1f}%)", 
                   recommendation.summary(), recommendation.confidence() * 100);
        // Additional recommendation processing can be added here
    }
    
    /**
     * Processing statistics for monitoring.
     */
    public record ProcessingStats(
        long processedMessages,
        long droppedMessages,
        boolean isRunning
    ) {}
    
    /**
     * Health information for the agent.
     */
    public record AgentHealth(
        NodeId nodeId,
        AgentState state,
        boolean isLeader,
        Option<Instant> startTime,
        Option<ProcessingStats> processingStats
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
        public Duration uptime() {
            return startTime.map(start -> Duration.between(start, Instant.now()))
                           .or(Duration.ZERO);
        }
    }
}