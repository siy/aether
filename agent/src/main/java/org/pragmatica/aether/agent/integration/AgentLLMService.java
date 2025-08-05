package org.pragmatica.aether.agent.integration;

import org.pragmatica.aether.agent.features.FeatureToggle;
import org.pragmatica.aether.agent.llm.SimpleLLMProvider;
import org.pragmatica.aether.agent.message.AgentRecommendation;
import org.pragmatica.aether.agent.message.ClusterEvent;
import org.pragmatica.aether.agent.message.SliceTelemetryBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Integration service that connects the Aether Agent with LLM providers.
 * 
 * This service handles the core integration between Track A (Message Infrastructure),
 * Track B (Mock LLM), and provides data for Track C (CLI) to display.
 * 
 * Key responsibilities:
 * - Process telemetry data and cluster events
 * - Generate analysis requests for LLM providers
 * - Create structured recommendations from LLM responses
 * - Manage feature toggles for LLM functionality
 * - Track metrics and costs for monitoring
 * 
 * The service follows the Aether pattern of functional composition and
 * immutable data structures while providing comprehensive integration
 * capabilities for the Phase 1 demonstration.
 */
public class AgentLLMService {
    private static final Logger logger = LoggerFactory.getLogger(AgentLLMService.class);
    
    private final SimpleLLMProvider llmProvider;
    private final FeatureToggle featureToggle;
    private final AtomicLong requestCounter;
    private final ConcurrentLinkedQueue<AgentRecommendation> recentRecommendations;
    private final ServiceMetrics metrics;
    
    public AgentLLMService(SimpleLLMProvider llmProvider, FeatureToggle featureToggle) {
        this.llmProvider = llmProvider;
        this.featureToggle = featureToggle;
        this.requestCounter = new AtomicLong(0);
        this.recentRecommendations = new ConcurrentLinkedQueue<>();
        this.metrics = new ServiceMetrics();
        
        logger.info("AgentLLMService initialized with provider: {} and feature toggles", 
                   llmProvider.providerId());
    }
    
    /**
     * Processes telemetry data and generates recommendations.
     * This represents the core Track A -> Track B integration.
     */
    public CompletableFuture<AgentRecommendation> processTelemetry(SliceTelemetryBatch telemetry) {
        if (!featureToggle.isEnabled(FeatureToggle.KnownFeature.AGENT_RECOMMENDATIONS_ENABLED)) {
            logger.debug("Recommendations disabled - skipping telemetry processing");
            return CompletableFuture.completedFuture(createDisabledRecommendation(telemetry));
        }
        
        long requestId = requestCounter.incrementAndGet();
        var startTime = Instant.now();
        
        logger.debug("Processing telemetry batch {} with {} entries", requestId, telemetry.sliceMetrics().size());
        
        return analyzeTelemetryWithLLM(telemetry, requestId)
            .thenApply(llmResponse -> {
                var recommendation = createRecommendation(telemetry, llmResponse, requestId, startTime);
                
                // Store for CLI access
                recentRecommendations.offer(recommendation);
                if (recentRecommendations.size() > 100) { // Keep last 100 recommendations
                    recentRecommendations.poll();
                }
                
                // Update metrics
                metrics.recordRecommendation(llmResponse.cost(), llmResponse.responseTime());
                
                logger.info("Generated recommendation {} for telemetry from {} slices", 
                          requestId, telemetry.sliceMetrics().size());
                
                return recommendation;
            })
            .exceptionally(error -> {
                logger.warn("Failed to process telemetry {}: {}", requestId, error.getMessage());
                metrics.recordError();
                return createErrorRecommendation(telemetry, error, requestId);
            });
    }
    
    /**
     * Processes cluster events and generates relevant recommendations.
     * This handles cluster-wide issues and scaling decisions.
     */
    public CompletableFuture<AgentRecommendation> processClusterEvent(ClusterEvent event) {
        if (!featureToggle.isEnabled(FeatureToggle.KnownFeature.AGENT_RECOMMENDATIONS_ENABLED)) {
            return CompletableFuture.completedFuture(createDisabledEventRecommendation(event));
        }
        
        long requestId = requestCounter.incrementAndGet();
        var startTime = Instant.now();
        
        logger.debug("Processing cluster event {} of type {}", requestId, event.eventType());
        
        return analyzeEventWithLLM(event, requestId)
            .thenApply(llmResponse -> {
                var recommendation = createEventRecommendation(event, llmResponse, requestId, startTime);
                
                recentRecommendations.offer(recommendation);
                if (recentRecommendations.size() > 100) {
                    recentRecommendations.poll();
                }
                
                metrics.recordRecommendation(llmResponse.cost(), llmResponse.responseTime());
                
                logger.info("Generated event recommendation {} for {}", requestId, event.eventType());
                
                return recommendation;
            })
            .exceptionally(error -> {
                logger.warn("Failed to process event {}: {}", requestId, error.getMessage());
                metrics.recordError();
                return createEventErrorRecommendation(event, error, requestId);
            });
    }
    
    /**
     * Gets recent recommendations for CLI display.
     * This provides Track C with data to show users.
     */
    public List<AgentRecommendation> recentRecommendations(int limit) {
        return recentRecommendations.stream()
            .skip(Math.max(0, recentRecommendations.size() - limit))
            .toList();
    }
    
    /**
     * Gets service metrics for monitoring and CLI display.
     */
    public ServiceMetrics metrics() {
        return metrics;
    }
    
    /**
     * Gets the current LLM provider health for status monitoring.
     */
    public CompletableFuture<SimpleLLMProvider.Health> llmHealth() {
        return llmProvider.healthCheck();
    }
    
    /**
     * Analyzes telemetry data using the LLM provider.
     */
    private CompletableFuture<SimpleLLMProvider.CompletionResponse> analyzeTelemetryWithLLM(
            SliceTelemetryBatch telemetry, long requestId) {
        
        var analysisPrompt = buildTelemetryAnalysisPrompt(telemetry);
        var request = new SimpleLLMProvider.CompletionRequest(
            List.of(analysisPrompt),
            300, // Allow for detailed analysis
            0.1  // Low temperature for consistent recommendations
        );
        
        logger.debug("Sending telemetry analysis request {} to LLM provider", requestId);
        return llmProvider.complete(request);
    }
    
    /**
     * Analyzes cluster events using the LLM provider.
     */
    private CompletableFuture<SimpleLLMProvider.CompletionResponse> analyzeEventWithLLM(
            ClusterEvent event, long requestId) {
        
        var analysisPrompt = buildEventAnalysisPrompt(event);
        var request = new SimpleLLMProvider.CompletionRequest(
            List.of(analysisPrompt),
            250, // Moderate length for event analysis
            0.1  // Low temperature for consistent recommendations
        );
        
        logger.debug("Sending event analysis request {} to LLM provider", requestId);
        return llmProvider.complete(request);
    }
    
    /**
     * Builds analysis prompt from telemetry data.
     */
    private String buildTelemetryAnalysisPrompt(SliceTelemetryBatch telemetry) {
        var sb = new StringBuilder();
        sb.append("Analyze the following system telemetry and provide recommendations:\n\n");
        
        // Add telemetry summary
        sb.append("Batch Interval: ").append(telemetry.batchInterval()).append("\n");
        sb.append("Total Events: ").append(telemetry.totalEvents()).append("\n");
        sb.append("Number of Slices: ").append(telemetry.sliceMetrics().size()).append("\n\n");
        
        // Add key metrics from slice metrics
        telemetry.sliceMetrics().forEach(slice -> {
            sb.append("Slice: ").append(slice.sliceName()).append(" (").append(slice.sliceId()).append(")\n");
            sb.append("  CPU: ").append(String.format("%.1f%%", slice.resources().cpuUtilizationPercent())).append("\n");
            sb.append("  Memory: ").append(String.format("%.1f%%", slice.resources().memoryUtilizationPercent())).append("\n");
            sb.append("  Requests: ").append(slice.performance().requestCount()).append("\n");
            sb.append("  Errors: ").append(slice.performance().errorCount()).append("\n");
            sb.append("  Status: ").append(slice.operations().status()).append("\n");
            sb.append("\n");
        });
        
        sb.append("Please provide specific recommendations for performance optimization, scaling, or issue resolution.");
        
        return sb.toString();
    }
    
    /**
     * Builds analysis prompt from cluster event.
     */
    private String buildEventAnalysisPrompt(ClusterEvent event) {
        return String.format("""
            Analyze the following cluster event and provide recommendations:
            
            Event Type: %s
            Node ID: %s
            Timestamp: %s
            Severity: %s
            
            Event Data:
            %s
            
            Please provide specific recommendations for addressing this event and preventing similar issues.
            """,
            event.eventType(),
            event.sourceNodeId(),
            event.timestamp(),
            event.severity(),
            event.eventData().toString()
        );
    }
    
    /**
     * Creates a recommendation from telemetry and LLM response.
     */
    private AgentRecommendation createRecommendation(
            SliceTelemetryBatch telemetry, 
            SimpleLLMProvider.CompletionResponse llmResponse,
            long requestId,
            Instant startTime) {
        
        return AgentRecommendation.create(
            "telemetry-" + requestId,
            AgentRecommendation.RecommendationType.OPERATIONAL_GUIDANCE,
            "Telemetry Analysis Recommendation",
            llmResponse.content(),
            0.85, // Confidence level
            List.of(), // No specific actions for Phase 1
            Map.of(
                "analysisType", "telemetry",
                "requestId", requestId,
                "sliceCount", telemetry.sliceMetrics().size(),
                "llmModel", llmResponse.modelUsed(),
                "processingTimeMs", java.time.Duration.between(startTime, Instant.now()).toMillis(),
                "tokensUsed", llmResponse.tokensUsed(),
                "cost", llmResponse.cost()
            ),
            java.time.Duration.ofHours(24), // Recommendations valid for 24 hours
            List.of(), // Affected components
            AgentRecommendation.RiskLevel.LOW
        );
    }
    
    /**
     * Creates a recommendation from cluster event and LLM response.
     */
    private AgentRecommendation createEventRecommendation(
            ClusterEvent event,
            SimpleLLMProvider.CompletionResponse llmResponse,
            long requestId,
            Instant startTime) {
        
        var riskLevel = switch (event.severity()) {
            case CRITICAL -> AgentRecommendation.RiskLevel.CRITICAL;
            case HIGH -> AgentRecommendation.RiskLevel.HIGH;
            case MEDIUM -> AgentRecommendation.RiskLevel.MEDIUM;
            case LOW, INFO -> AgentRecommendation.RiskLevel.LOW;
        };
        
        var type = switch (event.eventType()) {
            case SYSTEM_ERROR, RESOURCE_EXHAUSTION -> AgentRecommendation.RecommendationType.EMERGENCY_ACTION;
            case PERFORMANCE_DEGRADATION -> AgentRecommendation.RecommendationType.INVESTIGATION_REQUIRED;
            case DEPLOYMENT_CHANGE -> AgentRecommendation.RecommendationType.SCALING_RECOMMENDATION;
            default -> AgentRecommendation.RecommendationType.OPERATIONAL_GUIDANCE;
        };
        
        return AgentRecommendation.create(
            "event-" + requestId,
            type,
            "Cluster Event Analysis",
            llmResponse.content(),
            0.80, // Confidence level
            List.of(),
            Map.of(
                "analysisType", "event",
                "requestId", requestId,
                "eventType", event.eventType().toString(),
                "nodeId", event.sourceNodeId(),
                "severity", event.severity().toString(),
                "llmModel", llmResponse.modelUsed(),
                "processingTimeMs", java.time.Duration.between(startTime, Instant.now()).toMillis(),
                "tokensUsed", llmResponse.tokensUsed(),
                "cost", llmResponse.cost()
            ),
            java.time.Duration.ofHours(12), // Event recommendations expire faster
            List.of(), // Affected components
            riskLevel
        );
    }
    
    /**
     * Creates a disabled recommendation when features are toggled off.
     */
    private AgentRecommendation createDisabledRecommendation(SliceTelemetryBatch telemetry) {
        return AgentRecommendation.create(
            "disabled-" + System.nanoTime(),
            AgentRecommendation.RecommendationType.OPERATIONAL_GUIDANCE,
            "Recommendations Disabled",
            "Agent recommendations are currently disabled via feature toggle. Enable 'agent.recommendations.enabled' to receive AI-powered insights.",
            1.0, // Full confidence in this message
            List.of(),
            Map.of("reason", "feature_disabled", "featureToggle", "agent.recommendations.enabled"),
            java.time.Duration.ofMinutes(5),
            List.of(),
            AgentRecommendation.RiskLevel.LOW
        );
    }
    
    /**
     * Creates a disabled recommendation for events when features are toggled off.
     */
    private AgentRecommendation createDisabledEventRecommendation(ClusterEvent event) {
        return AgentRecommendation.create(
            "disabled-event-" + System.nanoTime(),
            AgentRecommendation.RecommendationType.OPERATIONAL_GUIDANCE,
            "Event Analysis Disabled",
            "Event analysis is currently disabled via feature toggle. Enable 'agent.recommendations.enabled' to receive insights for cluster events.",
            1.0,
            List.of(),
            Map.of("reason", "feature_disabled", "eventType", event.eventType().toString()),
            java.time.Duration.ofMinutes(5),
            List.of(),
            AgentRecommendation.RiskLevel.LOW
        );
    }
    
    /**
     * Creates an error recommendation when LLM processing fails.
     */
    private AgentRecommendation createErrorRecommendation(
            SliceTelemetryBatch telemetry, 
            Throwable error, 
            long requestId) {
        
        return AgentRecommendation.create(
            "error-" + requestId,
            AgentRecommendation.RecommendationType.OPERATIONAL_GUIDANCE,
            "Analysis Error",
            "Failed to generate recommendation due to: " + error.getMessage() + 
            ". The system will retry on the next telemetry batch.",
            0.0, // No confidence in error cases
            List.of(),
            Map.of("error", error.getMessage(), "requestId", requestId, "type", "telemetry_error"),
            java.time.Duration.ofMinutes(15),
            List.of(),
            AgentRecommendation.RiskLevel.LOW
        );
    }
    
    /**
     * Creates an error recommendation when event processing fails.
     */
    private AgentRecommendation createEventErrorRecommendation(
            ClusterEvent event, 
            Throwable error, 
            long requestId) {
        
        return AgentRecommendation.create(
            "error-event-" + requestId,
            AgentRecommendation.RecommendationType.OPERATIONAL_GUIDANCE,
            "Event Analysis Error",
            "Failed to analyze cluster event due to: " + error.getMessage(),
            0.0,
            List.of(),
            Map.of(
                "error", error.getMessage(), 
                "requestId", requestId, 
                "eventType", event.eventType().toString(),
                "type", "event_error"
            ),
            java.time.Duration.ofMinutes(15),
            List.of(),
            AgentRecommendation.RiskLevel.LOW
        );
    }
    
    /**
     * Service metrics for monitoring.
     */
    public static class ServiceMetrics {
        private final AtomicLong totalRecommendations = new AtomicLong(0);
        private final AtomicLong totalErrors = new AtomicLong(0);
        private volatile double totalCost = 0.0;
        private volatile java.time.Duration totalResponseTime = java.time.Duration.ZERO;
        private volatile Instant lastRecommendationTime;
        
        void recordRecommendation(double cost, java.time.Duration responseTime) {
            totalRecommendations.incrementAndGet();
            totalCost += cost;
            totalResponseTime = totalResponseTime.plus(responseTime);
            lastRecommendationTime = Instant.now();
        }
        
        void recordError() {
            totalErrors.incrementAndGet();
        }
        
        public long totalRecommendations() { return totalRecommendations.get(); }
        public long totalErrors() { return totalErrors.get(); }
        public double totalCost() { return totalCost; }
        public java.time.Duration averageResponseTime() { 
            long total = totalRecommendations.get();
            return total > 0 ? totalResponseTime.dividedBy(total) : java.time.Duration.ZERO;
        }
        public Instant lastRecommendationTime() { return lastRecommendationTime; }
        
        public double successRate() {
            long total = totalRecommendations.get() + totalErrors.get();
            return total > 0 ? (double) totalRecommendations.get() / total : 1.0;
        }
    }
}