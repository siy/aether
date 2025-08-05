package org.pragmatica.aether.agent.llm;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.pragmatica.lang.Promise;

/**
 * Simplified LLM provider interface that avoids complex dependencies.
 * This is a temporary implementation to get the system compiling and working.
 */
public interface SimpleLLMProvider {
    
    /**
     * Simple completion response.
     */
    record CompletionResponse(
        String content,
        int tokensUsed,
        Duration responseTime,
        double cost,
        String modelUsed,
        Map<String, Object> metadata
    ) {}
    
    /**
     * Simple completion request.
     */
    record CompletionRequest(
        List<String> messages,
        int maxTokens,
        double temperature
    ) {}
    
    /**
     * Provider status.
     */
    enum Status {
        HEALTHY, DEGRADED, UNAVAILABLE
    }
    
    /**
     * Provider health.
     */
    record Health(
        Status status,
        String message,
        Map<String, Object> metrics
    ) {}
    
    /**
     * Generates a completion.
     */
    Promise<CompletionResponse> complete(CompletionRequest request);
    
    /**
     * Gets provider health.
     */
    Promise<Health> healthCheck();
    
    /**
     * Gets provider ID.
     */
    String providerId();
}