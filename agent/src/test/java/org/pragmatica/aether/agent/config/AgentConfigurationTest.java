package org.pragmatica.aether.agent.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for AgentConfiguration and its components.
 * Tests cover configuration creation, validation, and all nested configuration types.
 */
@DisplayName("AgentConfiguration Tests")
class AgentConfigurationTest {
    
    @Nested
    @DisplayName("Configuration Creation Tests")
    class ConfigurationCreationTest {
        
        @Test
        @DisplayName("Should create default configuration with safe defaults")
        void shouldCreateDefaultConfigurationWithSafeDefaults() {
            var config = AgentConfiguration.defaultConfiguration();
            
            assertThat(config).isNotNull();
            assertThat(config.llmProviders()).isNotNull();
            assertThat(config.privacy()).isNotNull();
            assertThat(config.autonomy()).isNotNull();
            assertThat(config.performance()).isNotNull();
            assertThat(config.features()).isNotNull();
            
            // Verify safe defaults
            assertThat(config.privacy().auditLevel()).isEqualTo(AgentConfiguration.PrivacyConfig.AuditLevel.DISABLED);
            assertThat(config.autonomy().defaultLevel()).isEqualTo(AgentConfiguration.AutonomyConfig.AutonomyLevel.ADVISORY_ONLY);
            assertThat(config.features().agentEnabled()).isFalse();
            assertThat(config.features().shadowMode()).isTrue();
        }
        
        @Test
        @DisplayName("Should create configuration using builder pattern")
        void shouldCreateConfigurationUsingBuilderPattern() {
            var customPrivacy = new AgentConfiguration.PrivacyConfig(
                AgentConfiguration.PrivacyConfig.AuditLevel.BASIC,
                Duration.ofDays(7),
                true,
                AgentConfiguration.PrivacyConfig.ConsentMode.OPT_IN,
                List.of(AgentConfiguration.PrivacyConfig.ComplianceStandard.GDPR),
                Map.of("jurisdiction", "EU")
            );
            
            var config = AgentConfiguration.builder()
                .withPrivacy(customPrivacy)
                .build();
            
            assertThat(config.privacy()).isEqualTo(customPrivacy);
            assertThat(config.privacy().auditLevel()).isEqualTo(AgentConfiguration.PrivacyConfig.AuditLevel.BASIC);
            assertThat(config.privacy().enabledStandards()).contains(AgentConfiguration.PrivacyConfig.ComplianceStandard.GDPR);
        }
        
        @Test
        @DisplayName("Should build complete custom configuration")
        void shouldBuildCompleteCustomConfiguration() {
            var customLLM = createCustomLLMConfig();
            var customPrivacy = createCustomPrivacyConfig();
            var customAutonomy = createCustomAutonomyConfig();
            var customPerformance = createCustomPerformanceConfig();
            var customFeatures = createCustomFeaturesConfig();
            
            var config = AgentConfiguration.builder()
                .withLLMProviders(customLLM)
                .withPrivacy(customPrivacy)
                .withAutonomy(customAutonomy)
                .withPerformance(customPerformance)
                .withFeatures(customFeatures)
                .build();
            
            assertThat(config.llmProviders()).isEqualTo(customLLM);
            assertThat(config.privacy()).isEqualTo(customPrivacy);
            assertThat(config.autonomy()).isEqualTo(customAutonomy);
            assertThat(config.performance()).isEqualTo(customPerformance);
            assertThat(config.features()).isEqualTo(customFeatures);
        }
    }
    
    @Nested
    @DisplayName("Configuration Validation Tests")
    class ConfigurationValidationTest {
        
        @Test
        @DisplayName("Should validate LLM provider configuration")
        void shouldValidateLLMProviderConfiguration() {
            var invalidLLMConfig = new AgentConfiguration.LLMProviderConfig(
                List.of(), // Empty providers list
                "nonexistent",
                true,
                Duration.ofSeconds(30),
                3,
                Map.of()
            );
            
            assertThatThrownBy(() -> 
                AgentConfiguration.builder()
                    .withLLMProviders(invalidLLMConfig)
                    .build()
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("At least one LLM provider must be configured");
        }
        
        @Test
        @DisplayName("Should validate performance configuration")
        void shouldValidatePerformanceConfiguration() {
            var invalidPerformanceConfig = new AgentConfiguration.PerformanceConfig(
                100,
                Duration.ofSeconds(30),
                10,
                -100, // Invalid negative memory
                Duration.ofDays(7),
                true
            );
            
            assertThatThrownBy(() -> 
                AgentConfiguration.builder()
                    .withPerformance(invalidPerformanceConfig)
                    .build()
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("Max memory usage must be positive");
        }
        
        @Test
        @DisplayName("Should validate autonomy configuration")
        void shouldValidateAutonomyConfiguration() {
            var invalidAutonomyConfig = new AgentConfiguration.AutonomyConfig(
                AgentConfiguration.AutonomyConfig.AutonomyLevel.ADVISORY_ONLY,
                Map.of(),
                Map.of(),
                Duration.ofSeconds(-10) // Invalid negative timeout
            );
            
            assertThatThrownBy(() -> 
                AgentConfiguration.builder()
                    .withAutonomy(invalidAutonomyConfig)
                    .build()
            ).isInstanceOf(IllegalArgumentException.class)
             .hasMessageContaining("Confirmation timeout cannot be negative");
        }
    }
    
    @Nested
    @DisplayName("LLM Provider Configuration Tests")
    class LLMProviderConfigurationTest {
        
        @Test
        @DisplayName("Should create valid provider configurations")
        void shouldCreateValidProviderConfigurations() {
            var mockProvider = new AgentConfiguration.LLMProviderConfig.ProviderConfig(
                "mock-provider",
                AgentConfiguration.LLMProviderConfig.ProviderType.MOCK_PROVIDER,
                "localhost:8080",
                Map.of("api_key", "test-key"),
                0.0,
                4000,
                true
            );
            
            var openAIProvider = new AgentConfiguration.LLMProviderConfig.ProviderConfig(
                "openai-gpt4",
                AgentConfiguration.LLMProviderConfig.ProviderType.OPENAI_GPT4,
                "https://api.openai.com/v1",
                Map.of("api_key", "sk-test"),
                0.03,
                8000,
                true
            );
            
            assertThat(mockProvider.type()).isEqualTo(AgentConfiguration.LLMProviderConfig.ProviderType.MOCK_PROVIDER);
            assertThat(mockProvider.costPerToken()).isEqualTo(0.0);
            assertThat(mockProvider.enabled()).isTrue();
            
            assertThat(openAIProvider.type()).isEqualTo(AgentConfiguration.LLMProviderConfig.ProviderType.OPENAI_GPT4);
            assertThat(openAIProvider.costPerToken()).isEqualTo(0.03);
        }
        
        @Test
        @DisplayName("Should support all provider types")
        void shouldSupportAllProviderTypes() {
            var providerTypes = AgentConfiguration.LLMProviderConfig.ProviderType.values();
            
            assertThat(providerTypes).containsExactly(
                AgentConfiguration.LLMProviderConfig.ProviderType.LOCAL_QWEN3,
                AgentConfiguration.LLMProviderConfig.ProviderType.OPENAI_GPT4,
                AgentConfiguration.LLMProviderConfig.ProviderType.ANTHROPIC_CLAUDE,
                AgentConfiguration.LLMProviderConfig.ProviderType.MOCK_PROVIDER
            );
        }
    }
    
    @Nested
    @DisplayName("Privacy Configuration Tests")
    class PrivacyConfigurationTest {
        
        @Test
        @DisplayName("Should support all audit levels")
        void shouldSupportAllAuditLevels() {
            var auditLevels = AgentConfiguration.PrivacyConfig.AuditLevel.values();
            
            assertThat(auditLevels).containsExactly(
                AgentConfiguration.PrivacyConfig.AuditLevel.DISABLED,
                AgentConfiguration.PrivacyConfig.AuditLevel.BASIC,
                AgentConfiguration.PrivacyConfig.AuditLevel.STANDARD,
                AgentConfiguration.PrivacyConfig.AuditLevel.COMPREHENSIVE
            );
        }
        
        @Test
        @DisplayName("Should support all consent modes")
        void shouldSupportAllConsentModes() {
            var consentModes = AgentConfiguration.PrivacyConfig.ConsentMode.values();
            
            assertThat(consentModes).containsExactly(
                AgentConfiguration.PrivacyConfig.ConsentMode.OPT_IN,
                AgentConfiguration.PrivacyConfig.ConsentMode.OPT_OUT,
                AgentConfiguration.PrivacyConfig.ConsentMode.IMPLIED
            );
        }
        
        @Test
        @DisplayName("Should support all compliance standards")
        void shouldSupportAllComplianceStandards() {
            var complianceStandards = AgentConfiguration.PrivacyConfig.ComplianceStandard.values();
            
            assertThat(complianceStandards).containsExactly(
                AgentConfiguration.PrivacyConfig.ComplianceStandard.GDPR,
                AgentConfiguration.PrivacyConfig.ComplianceStandard.CCPA,
                AgentConfiguration.PrivacyConfig.ComplianceStandard.HIPAA,
                AgentConfiguration.PrivacyConfig.ComplianceStandard.SOX,
                AgentConfiguration.PrivacyConfig.ComplianceStandard.PCI_DSS
            );
        }
        
        @Test
        @DisplayName("Should create privacy configuration with multiple compliance standards")
        void shouldCreatePrivacyConfigurationWithMultipleComplianceStandards() {
            var privacyConfig = new AgentConfiguration.PrivacyConfig(
                AgentConfiguration.PrivacyConfig.AuditLevel.COMPREHENSIVE,
                Duration.ofDays(365),
                true,
                AgentConfiguration.PrivacyConfig.ConsentMode.OPT_IN,
                List.of(
                    AgentConfiguration.PrivacyConfig.ComplianceStandard.GDPR,
                    AgentConfiguration.PrivacyConfig.ComplianceStandard.CCPA,
                    AgentConfiguration.PrivacyConfig.ComplianceStandard.HIPAA
                ),
                Map.of("gdpr_representative", "eu-rep@company.com")
            );
            
            assertThat(privacyConfig.enabledStandards()).hasSize(3);
            assertThat(privacyConfig.enabledStandards()).contains(
                AgentConfiguration.PrivacyConfig.ComplianceStandard.GDPR,
                AgentConfiguration.PrivacyConfig.ComplianceStandard.CCPA,
                AgentConfiguration.PrivacyConfig.ComplianceStandard.HIPAA
            );
        }
    }
    
    @Nested
    @DisplayName("Autonomy Configuration Tests")
    class AutonomyConfigurationTest {
        
        @Test
        @DisplayName("Should support all autonomy levels")
        void shouldSupportAllAutonomyLevels() {
            var autonomyLevels = AgentConfiguration.AutonomyConfig.AutonomyLevel.values();
            
            assertThat(autonomyLevels).containsExactly(
                AgentConfiguration.AutonomyConfig.AutonomyLevel.ADVISORY_ONLY,
                AgentConfiguration.AutonomyConfig.AutonomyLevel.SUPERVISED,
                AgentConfiguration.AutonomyConfig.AutonomyLevel.SEMI_AUTONOMOUS,
                AgentConfiguration.AutonomyConfig.AutonomyLevel.FULLY_AUTONOMOUS
            );
        }
        
        @Test
        @DisplayName("Should support all action categories")
        void shouldSupportAllActionCategories() {
            var actionCategories = AgentConfiguration.AutonomyConfig.ActionCategory.values();
            
            assertThat(actionCategories).containsExactly(
                AgentConfiguration.AutonomyConfig.ActionCategory.SCALING,
                AgentConfiguration.AutonomyConfig.ActionCategory.CONFIGURATION_CHANGES,
                AgentConfiguration.AutonomyConfig.ActionCategory.RESTARTS,
                AgentConfiguration.AutonomyConfig.ActionCategory.RESOURCE_ALLOCATION,
                AgentConfiguration.AutonomyConfig.ActionCategory.MONITORING_SETUP,
                AgentConfiguration.AutonomyConfig.ActionCategory.INVESTIGATION,
                AgentConfiguration.AutonomyConfig.ActionCategory.EMERGENCY_RESPONSE
            );
        }
        
        @Test
        @DisplayName("Should create autonomy configuration with category-specific levels")
        void shouldCreateAutonomyConfigurationWithCategorySpecificLevels() {
            var categoryLevels = Map.of(
                AgentConfiguration.AutonomyConfig.ActionCategory.SCALING, 
                AgentConfiguration.AutonomyConfig.AutonomyLevel.SEMI_AUTONOMOUS,
                AgentConfiguration.AutonomyConfig.ActionCategory.EMERGENCY_RESPONSE, 
                AgentConfiguration.AutonomyConfig.AutonomyLevel.SUPERVISED
            );
            
            var safetyLimits = Map.of(
                AgentConfiguration.AutonomyConfig.ActionCategory.SCALING,
                List.of(new AgentConfiguration.AutonomyConfig.SafetyLimit(
                    "max_replicas", 2.0, 10.0, "Replica count limits"
                ))
            );
            
            var autonomyConfig = new AgentConfiguration.AutonomyConfig(
                AgentConfiguration.AutonomyConfig.AutonomyLevel.ADVISORY_ONLY,
                categoryLevels,
                safetyLimits,
                Duration.ofMinutes(5)
            );
            
            assertThat(autonomyConfig.categoryLevels()).hasSize(2);
            assertThat(autonomyConfig.categoryLevels().get(AgentConfiguration.AutonomyConfig.ActionCategory.SCALING))
                .isEqualTo(AgentConfiguration.AutonomyConfig.AutonomyLevel.SEMI_AUTONOMOUS);
            assertThat(autonomyConfig.safetyLimits()).hasSize(1);
        }
        
        @Test
        @DisplayName("Should create safety limits correctly")
        void shouldCreateSafetyLimitsCorrectly() {
            var safetyLimit = new AgentConfiguration.AutonomyConfig.SafetyLimit(
                "cpu_threshold",
                10.0,
                90.0,
                "CPU utilization limits for scaling decisions"
            );
            
            assertThat(safetyLimit.parameter()).isEqualTo("cpu_threshold");
            assertThat(safetyLimit.minValue()).isEqualTo(10.0);
            assertThat(safetyLimit.maxValue()).isEqualTo(90.0);
            assertThat(safetyLimit.description()).contains("CPU utilization limits");
        }
    }
    
    @Nested
    @DisplayName("Feature Toggle Configuration Tests")
    class FeatureToggleConfigurationTest {
        
        @Test
        @DisplayName("Should create feature configuration with all toggles")
        void shouldCreateFeatureConfigurationWithAllToggles() {
            var customFeatures = Map.of(
                "experimental_feature_1", true,
                "beta_feature_2", false
            );
            
            var featureConfig = new AgentConfiguration.FeatureToggleConfig(
                true,  // agentEnabled
                false, // shadowMode
                true,  // recommendationsEnabled
                true,  // naturalLanguageCliEnabled
                true,  // learningEnabled
                false, // autonomyEnabled
                true,  // predictiveAnalyticsEnabled
                false, // crossClusterInsightsEnabled
                customFeatures
            );
            
            assertThat(featureConfig.agentEnabled()).isTrue();
            assertThat(featureConfig.shadowMode()).isFalse();
            assertThat(featureConfig.recommendationsEnabled()).isTrue();
            assertThat(featureConfig.naturalLanguageCliEnabled()).isTrue();
            assertThat(featureConfig.learningEnabled()).isTrue();
            assertThat(featureConfig.autonomyEnabled()).isFalse();
            assertThat(featureConfig.predictiveAnalyticsEnabled()).isTrue();
            assertThat(featureConfig.crossClusterInsightsEnabled()).isFalse();
            assertThat(featureConfig.customFeatures()).hasSize(2);
            assertThat(featureConfig.customFeatures().get("experimental_feature_1")).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Performance Configuration Tests")
    class PerformanceConfigurationTest {
        
        @Test
        @DisplayName("Should create performance configuration with all parameters")
        void shouldCreatePerformanceConfigurationWithAllParameters() {
            var performanceConfig = new AgentConfiguration.PerformanceConfig(
                200,                    // batchSize
                Duration.ofSeconds(45), // processingTimeout
                15,                     // maxConcurrentAnalyses
                256,                    // maxMemoryUsageMB
                Duration.ofDays(30),    // metricsRetentionPeriod
                true                    // enablePerformanceOptimizations
            );
            
            assertThat(performanceConfig.batchSize()).isEqualTo(200);
            assertThat(performanceConfig.processingTimeout()).isEqualTo(Duration.ofSeconds(45));
            assertThat(performanceConfig.maxConcurrentAnalyses()).isEqualTo(15);
            assertThat(performanceConfig.maxMemoryUsageMB()).isEqualTo(256);
            assertThat(performanceConfig.metricsRetentionPeriod()).isEqualTo(Duration.ofDays(30));
            assertThat(performanceConfig.enablePerformanceOptimizations()).isTrue();
        }
    }
    
    // Helper methods to create custom configurations
    
    private AgentConfiguration.LLMProviderConfig createCustomLLMConfig() {
        var provider = new AgentConfiguration.LLMProviderConfig.ProviderConfig(
            "test-provider",
            AgentConfiguration.LLMProviderConfig.ProviderType.MOCK_PROVIDER,
            "localhost:9090",
            Map.of("key", "value"),
            0.01,
            8000,
            true
        );
        
        return new AgentConfiguration.LLMProviderConfig(
            List.of(provider),
            "test-provider",
            true,
            Duration.ofSeconds(45),
            5,
            Map.of("custom", "setting")
        );
    }
    
    private AgentConfiguration.PrivacyConfig createCustomPrivacyConfig() {
        return new AgentConfiguration.PrivacyConfig(
            AgentConfiguration.PrivacyConfig.AuditLevel.STANDARD,
            Duration.ofDays(90),
            true,
            AgentConfiguration.PrivacyConfig.ConsentMode.OPT_OUT,
            List.of(AgentConfiguration.PrivacyConfig.ComplianceStandard.GDPR),
            Map.of("region", "EU")
        );
    }
    
    private AgentConfiguration.AutonomyConfig createCustomAutonomyConfig() {
        return new AgentConfiguration.AutonomyConfig(
            AgentConfiguration.AutonomyConfig.AutonomyLevel.SUPERVISED,
            Map.of(),
            Map.of(),
            Duration.ofMinutes(3)
        );
    }
    
    private AgentConfiguration.PerformanceConfig createCustomPerformanceConfig() {
        return new AgentConfiguration.PerformanceConfig(
            150,
            Duration.ofSeconds(20),
            8,
            128,
            Duration.ofDays(14),
            false
        );
    }
    
    private AgentConfiguration.FeatureToggleConfig createCustomFeaturesConfig() {
        return new AgentConfiguration.FeatureToggleConfig(
            true, true, true, false, false, false, false, false, Map.of()
        );
    }
}