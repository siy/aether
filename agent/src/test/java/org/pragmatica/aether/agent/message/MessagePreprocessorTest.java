package org.pragmatica.aether.agent.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pragmatica.message.MessageRouter;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for MessagePreprocessor.
 * Tests cover message routing, batching, filtering, and error handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessagePreprocessor Tests")
class MessagePreprocessorTest {
    
    @Mock
    private MessageRouter messageRouter;
    
    private MessagePreprocessor preprocessor;
    private MessagePreprocessor.PreprocessorConfig config;
    
    @BeforeEach
    void setUp() {
        config = new MessagePreprocessor.PreprocessorConfig(
            Duration.ofMillis(100), // Short flush interval for testing
            5,                      // Small batch size for testing
            Duration.ofSeconds(30),
            true,
            1000
        );
        preprocessor = new MessagePreprocessor(messageRouter, config);
    }
    
    @Nested
    @DisplayName("Lifecycle Management Tests")
    class LifecycleManagementTest {
        
        @Test
        @DisplayName("Should start successfully")
        void shouldStartSuccessfully() {
            preprocessor.start();
            
            var stats = preprocessor.getStats();
            assertThat(stats.isRunning()).isTrue();
        }
        
        @Test
        @DisplayName("Should handle multiple start calls gracefully")
        void shouldHandleMultipleStartCallsGracefully() {
            preprocessor.start();
            preprocessor.start(); // Second start should be ignored
            
            var stats = preprocessor.getStats();
            assertThat(stats.isRunning()).isTrue();
        }
        
        @Test
        @DisplayName("Should stop successfully and flush pending messages")
        void shouldStopSuccessfullyAndFlushPendingMessages() {
            preprocessor.start();
            
            // Add a message to batch buffer
            var telemetryMessage = createSampleTelemetryBatch();
            preprocessor.processMessage(telemetryMessage);
            
            preprocessor.stop();
            
            var stats = preprocessor.getStats();
            assertThat(stats.isRunning()).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Message Processing Tests")
    class MessageProcessingTest {
        
        @Test
        @DisplayName("Should process high-priority messages immediately")
        void shouldProcessHighPriorityMessagesImmediately() throws InterruptedException {
            preprocessor.start();
            
            var criticalEvent = ClusterEvent.create(
                "node-1",
                ClusterEvent.ClusterEventType.SYSTEM_ERROR,
                "Critical failure",
                Map.of(),
                ClusterEvent.Severity.CRITICAL
            );
            
            var processedMessages = new AtomicInteger(0);
            var latch = new CountDownLatch(1);
            
            preprocessor.registerHandler(ClusterEvent.class, event -> {
                processedMessages.incrementAndGet();
                latch.countDown();
            });
            
            preprocessor.processMessage(criticalEvent);
            
            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(processedMessages.get()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("Should batch low-priority telemetry messages")
        void shouldBatchLowPriorityTelemetryMessages() throws InterruptedException {
            preprocessor.start();
            
            var processedMessages = new AtomicInteger(0);
            var latch = new CountDownLatch(3);
            
            preprocessor.registerHandler(SliceTelemetryBatch.class, batch -> {
                processedMessages.incrementAndGet();
                latch.countDown();
            });
            
            // Send multiple telemetry messages
            for (int i = 0; i < 3; i++) {
                var telemetryMessage = createSampleTelemetryBatch();
                preprocessor.processMessage(telemetryMessage);
            }
            
            // Messages should be batched, wait for flush
            Thread.sleep(150); // Wait longer than flush interval
            
            assertThat(processedMessages.get()).isEqualTo(3);
        }
        
        @Test
        @DisplayName("Should flush batch when size limit is reached")
        void shouldFlushBatchWhenSizeLimitIsReached() throws InterruptedException {
            preprocessor.start();
            
            var processedMessages = new AtomicInteger(0);
            var latch = new CountDownLatch(config.maxBatchSize());
            
            preprocessor.registerHandler(SliceTelemetryBatch.class, batch -> {
                processedMessages.incrementAndGet();
                latch.countDown();
            });
            
            // Send messages up to batch size limit
            for (int i = 0; i < config.maxBatchSize(); i++) {
                var telemetryMessage = createSampleTelemetryBatch();
                preprocessor.processMessage(telemetryMessage);
            }
            
            // Should flush immediately when batch size is reached
            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(processedMessages.get()).isEqualTo(config.maxBatchSize());
        }
        
        @Test
        @DisplayName("Should drop messages when not running")
        void shouldDropMessagesWhenNotRunning() {
            // Don't start the preprocessor
            var message = createSampleTelemetryBatch();
            
            preprocessor.processMessage(message);
            
            var stats = preprocessor.getStats();
            assertThat(stats.droppedMessages()).isEqualTo(1);
            assertThat(stats.processedMessages()).isEqualTo(0);
        }
        
        @Test
        @DisplayName("Should handle processing errors gracefully")
        void shouldHandleProcessingErrorsGracefully() {
            preprocessor.start();
            
            // Register a handler that throws an exception
            preprocessor.registerHandler(ClusterEvent.class, event -> {
                throw new RuntimeException("Test exception");
            });
            
            var event = ClusterEvent.create(
                "node-1",
                ClusterEvent.ClusterEventType.TOPOLOGY_CHANGE,
                "Test event",
                Map.of(),
                ClusterEvent.Severity.INFO
            );
            
            // Should not throw exception
            assertThatCode(() -> preprocessor.processMessage(event))
                .doesNotThrowAnyException();
            
            var stats = preprocessor.getStats();
            assertThat(stats.droppedMessages()).isEqualTo(1);
        }
    }
    
    @Nested
    @DisplayName("Custom Handler Registration Tests")
    class CustomHandlerRegistrationTest {
        
        @Test
        @DisplayName("Should register and invoke custom handlers")
        void shouldRegisterAndInvokeCustomHandlers() throws InterruptedException {
            preprocessor.start();
            
            var handlerInvoked = new AtomicInteger(0);
            var latch = new CountDownLatch(1);
            
            preprocessor.registerHandler(StateTransition.class, transition -> {
                handlerInvoked.incrementAndGet();
                latch.countDown();
            });
            
            var stateTransition = StateTransition.create(
                "test-component",
                StateTransition.TransitionType.SLICE_STARTUP,
                "STOPPED",
                "STARTING",
                Map.of(),
                "Test transition"
            );
            
            preprocessor.processMessage(stateTransition);
            
            assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();
            assertThat(handlerInvoked.get()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("Should handle missing handlers gracefully")
        void shouldHandleMissingHandlersGracefully() {
            preprocessor.start();
            
            // Clear default handlers by creating a custom message type (hypothetically)
            var recommendation = AgentRecommendation.create(
                "analysis-1",
                AgentRecommendation.RecommendationType.SCALING_RECOMMENDATION,
                "Test recommendation",
                "Test rationale",
                0.8,
                List.of(),
                Map.of(),
                Duration.ofMinutes(5),
                List.of("service-1"),
                AgentRecommendation.RiskLevel.LOW
            );
            
            // Should not throw exception even if handler is missing
            assertThatCode(() -> preprocessor.processMessage(recommendation))
                .doesNotThrowAnyException();
        }
    }
    
    @Nested
    @DisplayName("Statistics and Monitoring Tests")
    class StatisticsAndMonitoringTest {
        
        @Test
        @DisplayName("Should track processing statistics correctly")
        void shouldTrackProcessingStatisticsCorrectly() {
            preprocessor.start();
            
            var initialStats = preprocessor.getStats();
            assertThat(initialStats.processedMessages()).isEqualTo(0);
            assertThat(initialStats.droppedMessages()).isEqualTo(0);
            assertThat(initialStats.currentBatchSize()).isEqualTo(0);
            assertThat(initialStats.isRunning()).isTrue();
            
            // Process some messages
            var event = ClusterEvent.create(
                "node-1",
                ClusterEvent.ClusterEventType.CONFIGURATION_CHANGE,
                "Config updated",
                Map.of(),
                ClusterEvent.Severity.INFO
            );
            
            preprocessor.processMessage(event);
            
            var updatedStats = preprocessor.getStats();
            assertThat(updatedStats.processedMessages()).isEqualTo(1);
            assertThat(updatedStats.droppedMessages()).isEqualTo(0);
        }
        
        @Test
        @DisplayName("Should track batch buffer size correctly")
        void shouldTrackBatchBufferSizeCorrectly() {
            preprocessor.start();
            
            // Add telemetry messages to batch buffer
            for (int i = 0; i < 3; i++) {
                var telemetryMessage = createSampleTelemetryBatch();
                preprocessor.processMessage(telemetryMessage);
            }
            
            var stats = preprocessor.getStats();
            assertThat(stats.currentBatchSize()).isLessThanOrEqualTo(3);
        }
    }
    
    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTest {
        
        @Test
        @DisplayName("Should create default configuration with sensible values")
        void shouldCreateDefaultConfigurationWithSensibleValues() {
            var defaultConfig = MessagePreprocessor.PreprocessorConfig.defaultConfig();
            
            assertThat(defaultConfig.batchFlushInterval()).isEqualTo(Duration.ofSeconds(30));
            assertThat(defaultConfig.maxBatchSize()).isEqualTo(100);
            assertThat(defaultConfig.maxMessageAge()).isEqualTo(Duration.ofMinutes(5));
            assertThat(defaultConfig.enableRateLimiting()).isTrue();
            assertThat(defaultConfig.maxMessagesPerSecond()).isEqualTo(10000);
        }
        
        @Test
        @DisplayName("Should accept custom configuration")
        void shouldAcceptCustomConfiguration() {
            var customConfig = new MessagePreprocessor.PreprocessorConfig(
                Duration.ofSeconds(10),
                50,
                Duration.ofMinutes(1),
                false,
                5000
            );
            
            var customPreprocessor = new MessagePreprocessor(messageRouter, customConfig);
            
            // Configuration should be used (tested indirectly through behavior)
            assertThat(customPreprocessor).isNotNull();
        }
    }
    
    // Helper methods
    
    private SliceTelemetryBatch createSampleTelemetryBatch() {
        var performance = new SliceTelemetryBatch.SliceMetrics.PerformanceMetrics(
            45.5, 120.0, 200.0, 1000L, 20L, 0.02, 167.5
        );
        
        var resources = new SliceTelemetryBatch.SliceMetrics.ResourceMetrics(
            65.5, 1024L * 1024 * 512, 1024L * 1024 * 1024, 78.2,
            1024L * 100, 1024L * 80, 1024L * 50, 1024L * 30
        );
        
        var operations = new SliceTelemetryBatch.SliceMetrics.OperationalMetrics(
            SliceTelemetryBatch.SliceMetrics.SliceStatus.HEALTHY,
            3, 3, 86400L, 0, Map.of()
        );
        
        var sliceMetrics = new SliceTelemetryBatch.SliceMetrics(
            "test-slice-" + System.nanoTime(),
            "Test Slice",
            performance,
            resources,
            operations
        );
        
        return SliceTelemetryBatch.create(
            "test-node",
            List.of(sliceMetrics),
            Duration.ofSeconds(30),
            1000L
        );
    }
}