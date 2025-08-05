package org.pragmatica.aether.agent.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Simple mock LLM provider implementation for Track B demonstration.
 * Provides deterministic responses based on input patterns without complex dependencies.
 */
public class SimpleMockLLMProvider implements SimpleLLMProvider {
    private static final Logger logger = LoggerFactory.getLogger(SimpleMockLLMProvider.class);
    
    private final String providerId;
    private final AtomicLong requestCounter;
    private final List<TestScenario> scenarios;
    
    public SimpleMockLLMProvider(String providerId) {
        this.providerId = providerId;
        this.requestCounter = new AtomicLong(0);
        this.scenarios = createDefaultScenarios();
        
        logger.info("SimpleMockLLMProvider '{}' initialized with {} scenarios", 
                   providerId, scenarios.size());
    }
    
    record TestScenario(
        String name,
        Pattern pattern,
        String response,
        int tokensUsed,
        Duration latency
    ) {}
    
    @Override
    public CompletableFuture<CompletionResponse> complete(CompletionRequest request) {
        long requestId = requestCounter.incrementAndGet();
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                var startTime = Instant.now();
                
                // Find matching scenario
                var scenario = findMatchingScenario(request);
                
                // Simulate latency
                if (!scenario.latency().isZero()) {
                    Thread.sleep(scenario.latency().toMillis());
                }
                
                var responseTime = Duration.between(startTime, Instant.now());
                var cost = scenario.tokensUsed() * 0.0001; // Simple cost calculation
                
                logger.debug("SimpleMockLLMProvider completed request {} using scenario '{}' in {}ms", 
                           requestId, scenario.name(), responseTime.toMillis());
                
                return new CompletionResponse(
                    scenario.response(),
                    scenario.tokensUsed(),
                    responseTime,
                    cost,
                    "simple-mock-model",
                    Map.of(
                        "scenario", scenario.name(),
                        "requestId", requestId,
                        "providerId", providerId
                    )
                );
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Request interrupted", e);
            } catch (Exception e) {
                logger.warn("SimpleMockLLMProvider request {} failed: {}", requestId, e.getMessage());
                throw new RuntimeException("Request failed", e);
            }
        });
    }
    
    @Override
    public CompletableFuture<Health> healthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            // Simulate occasional health issues for testing
            if (ThreadLocalRandom.current().nextDouble() < 0.01) { // 1% chance
                return new Health(
                    Status.DEGRADED,
                    "Simulated degraded performance",
                    Map.of("requestCount", requestCounter.get())
                );
            }
            
            return new Health(
                Status.HEALTHY,
                "Simple mock provider operating normally",
                Map.of(
                    "requestCount", requestCounter.get(),
                    "scenarioCount", scenarios.size()
                )
            );
        });
    }
    
    @Override
    public String providerId() {
        return providerId;
    }
    
    private TestScenario findMatchingScenario(CompletionRequest request) {
        var combinedInput = String.join(" ", request.messages());
        
        return scenarios.stream()
            .filter(scenario -> scenario.pattern().matcher(combinedInput).find())
            .findFirst()
            .orElse(scenarios.get(scenarios.size() - 1)); // Use last as default
    }
    
    private List<TestScenario> createDefaultScenarios() {
        return List.of(
            // Performance analysis scenarios
            new TestScenario(
                "high_cpu_analysis",
                Pattern.compile(".*high CPU.*|.*CPU usage.*|.*processor.*", Pattern.CASE_INSENSITIVE),
                "The high CPU usage appears to be caused by inefficient algorithms in the payment processing slice. I recommend implementing connection pooling and optimizing the transaction validation logic. Expected improvement: 60-80% CPU reduction.",
                150,
                Duration.ofMillis(200)
            ),
            
            // Memory analysis scenarios        
            new TestScenario(
                "memory_leak_analysis",
                Pattern.compile(".*memory leak.*|.*memory usage.*|.*heap.*", Pattern.CASE_INSENSITIVE),
                "Analysis indicates a potential memory leak in the user session management component. The issue appears to be unclosed database connections. Recommendation: Implement proper connection lifecycle management and increase monitoring of connection pools.",
                180,
                Duration.ofMillis(300)
            ),
            
            // Scaling recommendations
            new TestScenario(
                "scaling_recommendation",
                Pattern.compile(".*scale.*|.*scaling.*|.*replicas.*", Pattern.CASE_INSENSITIVE),
                "Based on current load patterns, I recommend scaling the payment slice from 3 to 5 replicas during peak hours (9 AM - 6 PM). This will improve response times by approximately 40% and provide better fault tolerance.",
                120,
                Duration.ofMillis(250)
            ),
            
            // Error analysis scenarios
            new TestScenario(
                "error_pattern_analysis",
                Pattern.compile(".*error.*|.*exception.*|.*failure.*", Pattern.CASE_INSENSITIVE),
                "The error pattern indicates cascading failures triggered by database connection timeouts. Root cause: Insufficient connection pool size during traffic spikes. Immediate action: Increase connection pool to 50 connections. Long-term: Implement circuit breaker pattern.",
                200,
                Duration.ofMillis(400)
            ),
            
            // General system health
            new TestScenario(
                "system_health_summary",
                Pattern.compile(".*health.*|.*status.*|.*overview.*", Pattern.CASE_INSENSITIVE),
                "System health summary: 94% of slices operating normally. Minor issues detected in logging slice (disk space) and notification slice (message queue backlog). Priority actions: Clear log files and scale message processors.",
                140,
                Duration.ofMillis(150)
            ),
            
            // Test scenarios
            new TestScenario(
                "test_scenario",
                Pattern.compile(".*test.*", Pattern.CASE_INSENSITIVE),
                "This is a test response from the SimpleMockLLMProvider. The system is working correctly and can process various types of requests with deterministic responses.",
                80,
                Duration.ofMillis(100)
            ),
            
            // Default fallback
            new TestScenario(
                "default_response",
                Pattern.compile(".*"),
                "I've analyzed the available system metrics and telemetry data. To provide more specific recommendations, could you clarify what aspect of the system you'd like me to focus on? I can help with performance optimization, scaling decisions, error analysis, or general health assessment.",
                80,
                Duration.ofMillis(100)
            )
        );
    }
}