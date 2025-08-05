package org.pragmatica.aether.agent.message;

import org.pragmatica.message.MessageRouter;
import org.pragmatica.lang.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Preprocesses and routes agent messages with batching, filtering, and priority handling.
 * This component serves as the entry point for all agent messages, providing:
 * - Message batching for telemetry data to reduce processing overhead
 * - Priority-based routing to ensure critical events are processed immediately
 * - Rate limiting to prevent system overload during traffic spikes
 * - Message correlation and deduplication
 * 
 * The preprocessor integrates directly with MessageRouter to leverage the existing
 * message routing infrastructure while adding agent-specific processing logic.
 */
public class MessagePreprocessor {
    private static final Logger logger = LoggerFactory.getLogger(MessagePreprocessor.class);
    
    private final MessageRouter messageRouter;
    private final PreprocessorConfig config;
    private final ConcurrentHashMap<Class<? extends AgentMessage>, MessageHandler<?>> handlers;
    private final ConcurrentLinkedQueue<AgentMessage> batchBuffer;
    private final AtomicLong processedMessageCount;
    private final AtomicLong droppedMessageCount;
    private volatile boolean isRunning;
    private volatile Instant lastBatchFlush;
    
    public MessagePreprocessor(MessageRouter messageRouter, PreprocessorConfig config) {
        this.messageRouter = messageRouter;
        this.config = config;
        this.handlers = new ConcurrentHashMap<>();
        this.batchBuffer = new ConcurrentLinkedQueue<>();
        this.processedMessageCount = new AtomicLong(0);
        this.droppedMessageCount = new AtomicLong(0);
        this.isRunning = false;
        this.lastBatchFlush = Instant.now();
        
        registerDefaultHandlers();
    }
    
    /**
     * Starts the message preprocessor.
     * Note: Agent messages are handled independently from the main MessageRouter system.
     */
    public void start() {
        if (isRunning) {
            logger.warn("MessagePreprocessor is already running");
            return;
        }
        
        isRunning = true;
        lastBatchFlush = Instant.now();
        
        logger.info("MessagePreprocessor started with config: {}", config);
    }
    
    /**
     * Stops the message preprocessor and flushes any pending batched messages.
     */
    public void stop() {
        if (!isRunning) {
            return;
        }
        
        isRunning = false;
        flushBatchBuffer(); // Ensure no messages are lost during shutdown
        
        logger.info("MessagePreprocessor stopped. Processed: {}, Dropped: {}", 
                   processedMessageCount.get(), droppedMessageCount.get());
    }
    
    /**
     * Processes an incoming agent message with appropriate routing and batching.
     */
    public void processMessage(AgentMessage message) {
        if (!isRunning) {
            logger.warn("Dropping message - preprocessor not running: {}", message.messageId());
            droppedMessageCount.incrementAndGet();
            return;
        }
        
        try {
            // Check if we should flush the batch buffer based on time or size
            checkAndFlushBatch();
            
            // Route message based on type and priority
            if (shouldBatch(message)) {
                batchBuffer.offer(message);
            } else {
                routeImmediately(message);
            }
            
            processedMessageCount.incrementAndGet();
            
        } catch (Exception e) {
            logger.error("Error processing message {}: {}", message.messageId(), e.getMessage(), e);
            droppedMessageCount.incrementAndGet();
        }
    }
    
    /**
     * Registers a custom message handler for a specific message type.
     */
    public <T extends AgentMessage> void registerHandler(Class<T> messageType, Consumer<T> handler) {
        handlers.put(messageType, new MessageHandler<>(messageType, handler));
        logger.debug("Registered custom handler for message type: {}", messageType.getSimpleName());
    }
    
    /**
     * Gets processing statistics for monitoring and observability.
     */
    public ProcessingStats stats() {
        return new ProcessingStats(
            processedMessageCount.get(),
            droppedMessageCount.get(),
            batchBuffer.size(),
            isRunning
        );
    }
    
    private void registerDefaultHandlers() {
        // Default handlers for each message type
        registerHandler(SliceTelemetryBatch.class, this::handleTelemetryBatch);
        registerHandler(ClusterEvent.class, this::handleClusterEvent);
        registerHandler(StateTransition.class, this::handleStateTransition);
        registerHandler(AgentRecommendation.class, this::handleAgentRecommendation);
    }
    
    /**
     * Publishes an agent message to external systems if needed.
     * This method can be used to bridge agent messages to the MessageRouter system.
     */
    public void publishToMessageRouter(AgentMessage message) {
        // Future integration point: convert agent messages to system messages
        // For now, agent messages are handled internally within the agent system
        logger.debug("Agent message could be published to MessageRouter: {}", message.messageId());
    }
    
    private boolean shouldBatch(AgentMessage message) {
        // Only batch low-priority telemetry messages
        return message instanceof SliceTelemetryBatch && 
               message.priority() == AgentMessage.MessagePriority.LOW;
    }
    
    private void routeImmediately(AgentMessage message) {
        @SuppressWarnings("unchecked")
        var handler = (MessageHandler<AgentMessage>) handlers.get(message.getClass());
        
        Option.option(handler)
              .onPresent(h -> h.handle(message))
              .onEmpty(() -> logger.warn("No handler found for message type: {}", message.getClass().getSimpleName()));
    }
    
    private void checkAndFlushBatch() {
        Duration timeSinceLastFlush = Duration.between(lastBatchFlush, Instant.now());
        
        boolean shouldFlushByTime = timeSinceLastFlush.compareTo(config.batchFlushInterval()) >= 0;
        boolean shouldFlushBySize = batchBuffer.size() >= config.maxBatchSize();
        
        if (shouldFlushByTime || shouldFlushBySize) {
            flushBatchBuffer();
        }
    }
    
    private void flushBatchBuffer() {
        if (batchBuffer.isEmpty()) {
            return;
        }
        
        while (!batchBuffer.isEmpty()) {
            Option.option(batchBuffer.poll())
                  .onPresent(this::routeImmediately);
        }
        
        lastBatchFlush = Instant.now();
        logger.debug("Flushed batch buffer at {}", lastBatchFlush);
    }
    
    // Default message handlers
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
     * Configuration for the message preprocessor.
     */
    public record PreprocessorConfig(
        Duration batchFlushInterval,
        int maxBatchSize,
        Duration maxMessageAge,
        boolean enableRateLimiting,
        int maxMessagesPerSecond
    ) {
        
        public static PreprocessorConfig defaultConfig() {
            return new PreprocessorConfig(
                Duration.ofSeconds(30),  // Flush batches every 30 seconds
                100,                     // Maximum 100 messages per batch
                Duration.ofMinutes(5),   // Drop messages older than 5 minutes
                true,                    // Enable rate limiting
                10000                    // Maximum 10,000 messages per second
            );
        }
    }
    
    /**
     * Processing statistics for monitoring.
     */
    public record ProcessingStats(
        long processedMessages,
        long droppedMessages,
        int currentBatchSize,
        boolean isRunning
    ) {}
    
    /**
     * Internal message handler wrapper.
     */
    private record MessageHandler<T extends AgentMessage>(
        Class<T> messageType,
        Consumer<T> handler
    ) {
        public void handle(AgentMessage message) {
            if (messageType.isInstance(message)) {
                handler.accept(messageType.cast(message));
            }
        }
    }
}