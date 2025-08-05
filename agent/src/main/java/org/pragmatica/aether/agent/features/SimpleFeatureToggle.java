package org.pragmatica.aether.agent.features;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.pragmatica.lang.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.pragmatica.aether.agent.features.FeatureToggle.KnownFeature;

/**
 * Simple, efficient implementation of FeatureToggle using enum-first design.
 * <p>
 * Features:
 * - ConcurrentHashMap for thread-safe concurrent access
 * - Enum-based keys for type safety and performance
 * - Simple API: just enable/disable, no complex configuration
 * - Configuration loading/exporting for external integration
 * - Emergency mode for system-wide disable
 */
public class SimpleFeatureToggle implements FeatureToggle {
    private static final Logger logger = LoggerFactory.getLogger(SimpleFeatureToggle.class);

    // Primary storage: concurrent map with enum keys for efficiency
    private final ConcurrentHashMap<KnownFeature, Boolean> features = new ConcurrentHashMap<>();

    // Emergency mode flag
    private volatile boolean emergencyMode = false;

    public SimpleFeatureToggle() {
        initializeDefaults();
        logger.info("SimpleFeatureToggle initialized with {} default features", features.size());
    }

    @Override
    public boolean isEnabled(KnownFeature feature) {
        if (emergencyMode) {
            return false;
        }

        return features.getOrDefault(feature, feature.defaultValue());
    }

    @Override
    public void setEnabled(KnownFeature feature, boolean enabled) {
        boolean oldValue = features.getOrDefault(feature, feature.defaultValue());
        features.put(feature, enabled);

        if (oldValue != enabled) {
            logger.debug("Feature {} changed: {} -> {}", feature.key(), oldValue, enabled);
        }
    }

    @Override
    public void loadConfig(Map<String, Boolean> config) {
        logger.info("Loading configuration with {} entries", config.size());

        config.forEach((key, enabled) -> {
            KnownFeature.fromKey(key)
                        .onPresent(feature -> setEnabled(feature, enabled))
                        .onEmpty(() -> logger.warn("Unknown feature key in config: {}", key));
        });

        logger.info("Configuration loaded successfully");
    }

    @Override
    public Map<String, Boolean> exportConfig() {
        return features.entrySet()
                       .stream()
                       .collect(Collectors.toMap(
                               entry -> entry.getKey().key(),
                               Map.Entry::getValue
                       ));
    }


    @Override
    public void emergencyDisableAll() {
        logger.warn("EMERGENCY MODE ACTIVATED - All features disabled");
        emergencyMode = true;
    }

    @Override
    public void restoreFromEmergency() {
        logger.info("Emergency mode deactivated - Normal feature operation restored");
        emergencyMode = false;
    }

    @Override
    public boolean isEmergencyMode() {
        return emergencyMode;
    }

    /**
     * Initialize all known features with their default values.
     */
    private void initializeDefaults() {
        for (KnownFeature feature : KnownFeature.values()) {
            features.put(feature, feature.defaultValue());
        }
        logger.debug("Initialized {} features with default values", features.size());
    }


    /**
     * Get current feature count for monitoring.
     */
    public int featureCount() {
        return features.size();
    }

    /**
     * Get enabled feature count for monitoring.
     */
    public long enabledFeatureCount() {
        return features.values()
                       .stream()
                       .mapToLong(enabled -> enabled ? 1 : 0)
                       .sum();
    }
}
