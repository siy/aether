package org.pragmatica.aether.agent.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Sealed interface hierarchy for agent configuration management.
 * 
 * Provides compile-time safety for configuration options and enables hot-reload capability.
 * The configuration system is designed to support privacy and compliance settings,
 * autonomy level configuration per action category, and provider selection rules.
 * 
 * Configuration follows the builder pattern for complex construction and includes
 * validation rules for consistency. JSON serialization is supported for external
 * configuration files, with migration support for schema evolution.
 */
public sealed interface AgentConfiguration 
    permits AgentConfiguration.DefaultConfiguration {
    
    /**
     * Gets the LLM provider configuration.
     */
    LLMProviderConfig llmProviders();
    
    /**
     * Gets the privacy and compliance settings.
     */
    PrivacyConfig privacy();
    
    /**
     * Gets the autonomy configuration for different action categories.
     */
    AutonomyConfig autonomy();
    
    /**
     * Gets performance tuning parameters.
     */
    PerformanceConfig performance();
    
    /**
     * Gets feature toggle configuration.
     */
    FeatureToggleConfig features();
    
    /**
     * Creates a builder for constructing agent configuration.
     */
    static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a default configuration suitable for most deployments.
     */
    static AgentConfiguration defaultConfiguration() {
        return builder().build();
    }
    
    /**
     * Default implementation of agent configuration.
     */
    record DefaultConfiguration(
        LLMProviderConfig llmProviders,
        PrivacyConfig privacy,
        AutonomyConfig autonomy,
        PerformanceConfig performance,
        FeatureToggleConfig features
    ) implements AgentConfiguration {}
    
    /**
     * Configuration for LLM providers and fallback chains.
     */
    record LLMProviderConfig(
        List<ProviderConfig> providers,
        String primaryProvider,
        boolean enableFallback,
        Duration requestTimeout,
        int maxRetries,
        Map<String, Object> customSettings
    ) {
        
        /**
         * Configuration for a single LLM provider.
         */
        public record ProviderConfig(
            String name,
            ProviderType type,
            String endpoint,
            Map<String, String> credentials,
            double costPerToken,
            int maxTokens,
            boolean enabled
        ) {}
        
        /**
         * Types of LLM providers supported.
         */
        public enum ProviderType {
            LOCAL_QWEN3,
            OPENAI_GPT4,
            ANTHROPIC_CLAUDE,
            MOCK_PROVIDER
        }
    }
    
    /**
     * Privacy and compliance configuration.
     */
    record PrivacyConfig(
        AuditLevel auditLevel,
        Duration dataRetentionPeriod,
        boolean enableDataAnonymization,
        ConsentMode consentMode,
        List<ComplianceStandard> enabledStandards,
        Map<String, Object> jurisdictionSettings
    ) {
        
        /**
         * Levels of audit logging and data collection.
         */
        public enum AuditLevel {
            /** No audit logging, minimal data collection */
            DISABLED,
            /** Basic operational logs only */
            BASIC,
            /** Standard operational and performance logs */
            STANDARD,
            /** Comprehensive logging for compliance */
            COMPREHENSIVE
        }
        
        /**
         * Consent management modes.
         */
        public enum ConsentMode {
            /** No data collection without explicit consent */
            OPT_IN,
            /** Data collection enabled by default, can opt out */
            OPT_OUT,
            /** Implied consent for operational data */
            IMPLIED
        }
        
        /**
         * Supported compliance standards.
         */
        public enum ComplianceStandard {
            GDPR,
            CCPA,
            HIPAA,
            SOX,
            PCI_DSS
        }
    }
    
    /**
     * Autonomy configuration for different action categories.
     */
    record AutonomyConfig(
        AutonomyLevel defaultLevel,
        Map<ActionCategory, AutonomyLevel> categoryLevels,
        Map<ActionCategory, List<SafetyLimit>> safetyLimits,
        Duration confirmationTimeout
    ) {
        
        /**
         * Levels of autonomy for agent actions.
         */
        public enum AutonomyLevel {
            /** Agent only observes and reports */
            ADVISORY_ONLY,
            /** Agent recommends actions, requires approval */
            SUPERVISED,
            /** Agent can execute safe actions automatically */
            SEMI_AUTONOMOUS,
            /** Agent can execute all configured actions */
            FULLY_AUTONOMOUS
        }
        
        /**
         * Categories of actions the agent can perform.
         */
        public enum ActionCategory {
            SCALING,
            CONFIGURATION_CHANGES,
            RESTARTS,
            RESOURCE_ALLOCATION,
            MONITORING_SETUP,
            INVESTIGATION,
            EMERGENCY_RESPONSE
        }
        
        /**
         * Safety limits for specific action categories.
         */
        public record SafetyLimit(
            String parameter,
            double minValue,
            double maxValue,
            String description
        ) {}
    }
    
    /**
     * Performance tuning configuration.
     */
    record PerformanceConfig(
        int batchSize,
        Duration processingTimeout,
        int maxConcurrentAnalyses,
        long maxMemoryUsageMB,
        Duration metricsRetentionPeriod,
        boolean enablePerformanceOptimizations
    ) {}
    
    /**
     * Feature toggle configuration.
     */
    record FeatureToggleConfig(
        boolean agentEnabled,
        boolean shadowMode,
        boolean recommendationsEnabled,
        boolean naturalLanguageCliEnabled,
        boolean learningEnabled,
        boolean autonomyEnabled,
        boolean predictiveAnalyticsEnabled,
        boolean crossClusterInsightsEnabled,
        Map<String, Boolean> customFeatures
    ) {}
    
    /**
     * Builder for constructing agent configuration with validation.
     */
    public static class Builder {
        private LLMProviderConfig llmProviders;
        private PrivacyConfig privacy;
        private AutonomyConfig autonomy;
        private PerformanceConfig performance;
        private FeatureToggleConfig features;
        
        private Builder() {
            // Set default values
            this.llmProviders = createDefaultLLMConfig();
            this.privacy = createDefaultPrivacyConfig();
            this.autonomy = createDefaultAutonomyConfig();
            this.performance = createDefaultPerformanceConfig();
            this.features = createDefaultFeaturesConfig();
        }
        
        public Builder withLLMProviders(LLMProviderConfig llmProviders) {
            this.llmProviders = llmProviders;
            return this;
        }
        
        public Builder withPrivacy(PrivacyConfig privacy) {
            this.privacy = privacy;
            return this;
        }
        
        public Builder withAutonomy(AutonomyConfig autonomy) {
            this.autonomy = autonomy;
            return this;
        }
        
        public Builder withPerformance(PerformanceConfig performance) {
            this.performance = performance;
            return this;
        }
        
        public Builder withFeatures(FeatureToggleConfig features) {
            this.features = features;
            return this;
        }
        
        /**
         * Builds the configuration with validation.
         */
        public AgentConfiguration build() {
            validateConfiguration();
            return new DefaultConfiguration(llmProviders, privacy, autonomy, performance, features);
        }
        
        private void validateConfiguration() {
            if (llmProviders == null || llmProviders.providers().isEmpty()) {
                throw new IllegalArgumentException("At least one LLM provider must be configured");
            }
            
            if (performance.maxMemoryUsageMB() <= 0) {
                throw new IllegalArgumentException("Max memory usage must be positive");
            }
            
            if (autonomy.confirmationTimeout().isNegative()) {
                throw new IllegalArgumentException("Confirmation timeout cannot be negative");
            }
            
            // Additional validation rules would go here
        }
        
        private static LLMProviderConfig createDefaultLLMConfig() {
            var mockProvider = new LLMProviderConfig.ProviderConfig(
                "mock",
                LLMProviderConfig.ProviderType.MOCK_PROVIDER,
                "localhost:8080",
                Map.of(),
                0.0,
                4000,
                true
            );
            
            return new LLMProviderConfig(
                List.of(mockProvider),
                "mock",
                true,
                Duration.ofSeconds(30),
                3,
                Map.of()
            );
        }
        
        private static PrivacyConfig createDefaultPrivacyConfig() {
            return new PrivacyConfig(
                PrivacyConfig.AuditLevel.DISABLED,  // Privacy by design - disabled by default
                Duration.ofDays(30),
                true,
                PrivacyConfig.ConsentMode.OPT_IN,
                List.of(),  // No compliance standards enabled by default
                Map.of()
            );
        }
        
        private static AutonomyConfig createDefaultAutonomyConfig() {
            return new AutonomyConfig(
                AutonomyConfig.AutonomyLevel.ADVISORY_ONLY,  // Safe default
                Map.of(),  // No category-specific overrides
                Map.of(),  // No safety limits by default
                Duration.ofMinutes(5)
            );
        }
        
        private static PerformanceConfig createDefaultPerformanceConfig() {
            return new PerformanceConfig(
                100,  // batch size
                Duration.ofSeconds(30),  // processing timeout
                10,   // max concurrent analyses
                100,  // max memory usage MB
                Duration.ofDays(7),  // metrics retention
                true  // enable optimizations
            );
        }
        
        private static FeatureToggleConfig createDefaultFeaturesConfig() {
            return new FeatureToggleConfig(
                false,  // agent disabled by default
                true,   // shadow mode enabled for safety
                false,  // recommendations disabled by default
                false,  // NLP CLI disabled by default
                false,  // learning disabled by default
                false,  // autonomy disabled by default
                false,  // predictive analytics disabled by default
                false,  // cross-cluster insights disabled by default
                Map.of()
            );
        }
    }
}