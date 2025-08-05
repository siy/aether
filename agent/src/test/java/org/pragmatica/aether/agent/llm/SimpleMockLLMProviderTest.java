package org.pragmatica.aether.agent.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Result.Success;
import org.pragmatica.lang.Result.Failure;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for SimpleMockLLMProvider functionality.
 */
@DisplayName("SimpleMockLLMProvider Tests")
class SimpleMockLLMProviderTest {
    
    private SimpleMockLLMProvider provider;
    
    @BeforeEach
    void setUp() {
        provider = new SimpleMockLLMProvider("test-provider");
    }
    
    @Test
    @DisplayName("Test basic completion request")
    void testBasicCompletionRequest() throws Exception {
        var request = new SimpleLLMProvider.CompletionRequest(
            List.of("Test message"),
            100,
            0.0
        );
        
        var response = switch(provider.complete(request).await()) {
            case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
            case Failure f -> {
                throw new RuntimeException(f.cause().message());
            }
        };
        
        assertThat(response.content()).isNotEmpty();
        assertThat(response.tokensUsed()).isGreaterThan(0);
        assertThat(response.responseTime()).isPositive();
        assertThat(response.cost()).isGreaterThanOrEqualTo(0.0);
        assertThat(response.modelUsed()).isEqualTo("simple-mock-model");
        assertThat(response.metadata()).containsKeys("scenario", "requestId", "providerId");
    }
    
    @Test
    @DisplayName("Test deterministic responses")
    void testDeterministicResponses() throws Exception {
        var request = new SimpleLLMProvider.CompletionRequest(
            List.of("CPU usage analysis"),
            100,
            0.0
        );
        
        var response1 = switch(provider.complete(request).await()) {
            case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
            case Failure f -> {
                throw new RuntimeException(f.cause().message());
            }
        };
        var response2 = switch(provider.complete(request).await()) {
            case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
            case Failure f -> {
                throw new RuntimeException(f.cause().message());
            }
        };
        
        // Content should be identical for same input
        assertThat(response1.content()).isEqualTo(response2.content());
        assertThat(response1.tokensUsed()).isEqualTo(response2.tokensUsed());
    }
    
    @Test
    @DisplayName("Test scenario pattern matching")
    void testScenarioPatternMatching() throws Exception {
        // Test high CPU scenario
        var cpuRequest = new SimpleLLMProvider.CompletionRequest(
            List.of("High CPU usage detected"),
            100,
            0.0
        );
        
        var cpuResponse = switch(provider.complete(cpuRequest).await()) {
            case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
            case Failure f -> {
                throw new RuntimeException(f.cause().message());
            }
        };
        assertThat(cpuResponse.content().toLowerCase()).contains("cpu");
        
        // Test memory scenario
        var memoryRequest = new SimpleLLMProvider.CompletionRequest(
            List.of("Memory leak investigation"),
            100,
            0.0
        );
        
        var memoryResponse = switch(provider.complete(memoryRequest).await()) {
            case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
            case Failure f -> {
                throw new RuntimeException(f.cause().message());
            }
        };
        assertThat(memoryResponse.content().toLowerCase()).contains("memory");
        
        // Test error scenario
        var errorRequest = new SimpleLLMProvider.CompletionRequest(
            List.of("Exception stack trace analysis"),
            100,
            0.0
        );
        
        var errorResponse = switch(provider.complete(errorRequest).await()) {
            case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
            case Failure f -> {
                throw new RuntimeException(f.cause().message());
            }
        };
        assertThat(errorResponse.content().toLowerCase()).containsAnyOf("error", "exception");
    }
    
    @Test
    @DisplayName("Test cost calculation")
    void testCostCalculation() throws Exception {
        var request = new SimpleLLMProvider.CompletionRequest(
            List.of("Cost test"),
            200,
            0.0
        );
        
        var response = switch(provider.complete(request).await()) {
            case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
            case Failure f -> {
                throw new RuntimeException(f.cause().message());
            }
        };
        
        var cost = response.cost();
        var tokensUsed = response.tokensUsed();
        
        assertThat(cost).isGreaterThan(0);
        assertThat(tokensUsed).isGreaterThan(0);
        
        // Cost should be proportional to tokens used
        var expectedCost = tokensUsed * 0.0001;
        assertThat(cost).isEqualTo(expectedCost);
    }
    
    @Test
    @DisplayName("Test health check")
    void testHealthCheck() throws Exception {
        var health = switch(provider.healthCheck().await()) {
            case Success s -> (SimpleLLMProvider.Health) s.value();
            case Failure f -> {
                throw new RuntimeException(f.cause().message());
            }
        };
        
        assertThat(health.status()).isIn(
            SimpleLLMProvider.Status.HEALTHY, 
            SimpleLLMProvider.Status.DEGRADED
        );
        assertThat(health.message()).isNotEmpty();
        assertThat(health.metrics()).containsKeys("requestCount", "scenarioCount");
    }
    
    @Test
    @DisplayName("Test provider ID")
    void testProviderId() {
        assertThat(provider.providerId()).isEqualTo("test-provider");
    }
    
    @Test
    @DisplayName("Test test scenario")
    void testTestScenario() throws Exception {
        var request = new SimpleLLMProvider.CompletionRequest(
            List.of("This is a test request"),
            100,
            0.0
        );
        
        var response = switch(provider.complete(request).await()) {
            case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
            case Failure f -> {
                throw new RuntimeException(f.cause().message());
            }
        };
        assertThat(response.content().toLowerCase()).contains("test");
        assertThat(response.metadata().get("scenario")).isEqualTo("test_scenario");
    }
    
    @Test
    @DisplayName("Test default fallback scenario")
    void testDefaultFallbackScenario() throws Exception {
        var request = new SimpleLLMProvider.CompletionRequest(
            List.of("This is a completely unmatched request with unique words xyz123"),
            100,
            0.0
        );
        
        var response = switch(provider.complete(request).await()) {
            case Success s -> (SimpleLLMProvider.CompletionResponse) s.value();
            case Failure f -> {
                throw new RuntimeException(f.cause().message());
            }
        };
        assertThat(response.content()).isNotEmpty();
        assertThat(response.metadata().get("scenario")).isEqualTo("default_response");
    }
}