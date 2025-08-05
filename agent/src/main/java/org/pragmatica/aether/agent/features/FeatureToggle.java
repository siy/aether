package org.pragmatica.aether.agent.features;

import org.pragmatica.lang.Option;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Feature toggle system for the Aether Agent.
 * <p>
 * Provides runtime configuration management without requiring restarts,
 * enabling gradual rollouts, A/B testing, and emergency kill switches.
 * Integrates with the consensus system to ensure consistent feature states
 * across all cluster nodes.
 * <p>
 * The feature toggle system supports hierarchical features with inheritance,
 * user segmentation for targeted rollouts, and percentage-based gradual
 * deployments. All toggle states are stored in consensus to maintain
 * consistency across the distributed system.
 */
public interface FeatureToggle {

    /**
     * Predefined feature flags for the Aether Agent system.
     * Following the specification from the design document.
     */
    enum KnownFeature {
        // Core Features
        AGENT_ENABLED("agent.enabled", "Master switch for entire agent", true),
        AGENT_SHADOW_MODE("agent.shadow_mode", "Run without user visibility", false),
        AGENT_RECOMMENDATIONS_ENABLED("agent.recommendations.enabled", "Enable recommendation generation", true),
        AGENT_CLI_NATURAL_LANGUAGE("agent.cli.natural_language", "Enable NLP in CLI", true),

        // LLM Features  
        LLM_LOCAL_ENABLED("llm.local.enabled", "Enable local Qwen3 models", true),
        LLM_CLOUD_ENABLED("llm.cloud.enabled", "Enable cloud providers", false),
        LLM_FALLBACK_ENABLED("llm.fallback.enabled", "Enable automatic fallback", true),
        LLM_COST_LIMITS_ENABLED("llm.cost_limits.enabled", "Enable cost circuit breakers", true),

        // Advanced Features
        AGENT_LEARNING_ENABLED("agent.learning.enabled", "Enable pattern learning", false),
        AGENT_AUTONOMY_ENABLED("agent.autonomy.enabled", "Enable autonomous actions", false),
        AGENT_PREDICTIVE_ENABLED("agent.predictive.enabled", "Enable predictive analytics", false),
        AGENT_CROSS_CLUSTER_ENABLED("agent.cross_cluster.enabled", "Enable cross-cluster insights", false);

        private final String key;
        private final String description;
        private final boolean defaultValue;

        KnownFeature(String key, String description, boolean defaultValue) {
            this.key = key;
            this.description = description;
            this.defaultValue = defaultValue;
        }

        public String key() {
            return key;
        }

        public String description() {
            return description;
        }

        public boolean defaultValue() {
            return defaultValue;
        }

        /**
         * Natural syntax for checking if a feature is enabled.
         * Usage: AGENT_ENABLED.in(toggle)
         */
        public boolean in(FeatureToggle toggle) {
            return toggle.isEnabled(this);
        }

        /**
         * Natural syntax for checking if a feature is disabled.
         * Usage: AGENT_ENABLED.notIn(toggle)
         */
        public boolean notIn(FeatureToggle toggle) {
            return !toggle.isEnabled(this);
        }

        /**
         * Lookup map for converting string keys to enum values.
         */
        private static final Map<String, KnownFeature> BY_KEY =
                Stream.of(values())
                      .collect(Collectors.toMap(KnownFeature::key, Function.identity()));

        /**
         * Convert string key to enum value.
         * Used for configuration loading and external integrations.
         *
         * @param key The string key to look up
         *
         * @return Option containing the feature if found, empty otherwise
         */
        public static Option<KnownFeature> fromKey(String key) {
            return Option.option(BY_KEY.get(key));
        }
    }

    /**
     * Simple evaluation context for basic feature toggle evaluation.
     * Currently simplified to just empty context, but can be extended later if needed.
     */
    record EvaluationContext() {
        public static EvaluationContext empty() {
            return new EvaluationContext();
        }
    }


    /**
     * Evaluates whether a feature is enabled for the given context.
     * Primary API - uses enum for type safety.
     *
     * @param feature The feature to evaluate
     * @param context The evaluation context with user/node/cluster information
     *
     * @return true if the feature is enabled for this context
     */
    boolean isEnabled(KnownFeature feature, EvaluationContext context);

    /**
     * Evaluates a feature with empty context (global setting).
     *
     * @param feature The feature to evaluate
     *
     * @return true if the feature is globally enabled
     */
    default boolean isEnabled(KnownFeature feature) {
        return isEnabled(feature, EvaluationContext.empty());
    }

    /**
     * Updates a feature toggle state.
     * Simple API for basic enable/disable operations.
     *
     * @param feature The feature to update
     * @param enabled Whether the feature should be enabled
     */
    void updateToggle(KnownFeature feature, boolean enabled);

    /**
     * Loads configuration from external sources (JSON, properties, etc.).
     * Converts string keys to enum values automatically.
     *
     * @param config Map of feature keys to enabled states
     */
    void loadFromConfig(Map<String, Boolean> config);

    /**
     * Exports current configuration as string map.
     * Useful for saving to files or external systems.
     *
     * @return Map of feature keys to their current states
     */
    Map<String, Boolean> exportConfig();

    /**
     * Registers a listener for feature toggle changes.
     *
     * @param feature  The feature to listen for
     * @param listener The listener to notify of changes
     */
    void addToggleListener(KnownFeature feature, ToggleListener listener);

    /**
     * Removes a feature toggle listener.
     *
     * @param feature  The feature
     * @param listener The listener to remove
     */
    void removeToggleListener(KnownFeature feature, ToggleListener listener);

    /**
     * Listener interface for feature toggle changes.
     */
    @FunctionalInterface
    interface ToggleListener {
        /**
         * Called when a feature toggle configuration changes.
         *
         * @param feature  The feature that changed
         * @param oldValue The previous value
         * @param newValue The new value
         */
        void onToggleChanged(KnownFeature feature, boolean oldValue, boolean newValue);
    }

    /**
     * Emergency kill switch that disables all features.
     * Used for immediate system-wide feature disabling in crisis situations.
     */
    void emergencyDisableAll();

    /**
     * Restores normal feature toggle operation after emergency disable.
     */
    void restoreFromEmergency();

    /**
     * Checks if the system is in emergency mode.
     *
     * @return true if emergency mode is active
     */
    boolean isEmergencyMode();
}