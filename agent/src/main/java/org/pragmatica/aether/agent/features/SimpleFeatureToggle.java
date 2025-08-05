package org.pragmatica.aether.agent.features;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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

    // Thread-safe listener storage
    private final ConcurrentHashMap<KnownFeature, CopyOnWriteArrayList<ToggleListener>> listeners = new ConcurrentHashMap<>();

    // Emergency mode flag
    private volatile boolean emergencyMode = false;

    public SimpleFeatureToggle() {
        initializeDefaults();
        logger.info("SimpleFeatureToggle initialized with {} default features", features.size());
    }

    @Override
    public boolean isEnabled(KnownFeature feature, EvaluationContext context) {
        if (emergencyMode) {
            return false;
        }

        return features.getOrDefault(feature, feature.defaultValue());
    }

    // WARNING: update is not thread safe, but in real life it might not be that
    // important given that feature toggles most likely will come from single source
    // anyway.
    @Override
    public void updateToggle(KnownFeature feature, boolean enabled) {
        boolean oldValue = features.getOrDefault(feature, feature.defaultValue());
        features.put(feature, enabled);

        if (oldValue != enabled) {
            notifyListeners(feature, oldValue, enabled);
            logger.debug("Feature {} changed: {} -> {}", feature.key(), oldValue, enabled);
        }
    }

    @Override
    public void loadFromConfig(Map<String, Boolean> config) {
        logger.info("Loading configuration with {} entries", config.size());

        config.forEach((key, enabled) -> {
            KnownFeature.fromKey(key)
                        .onPresent(feature -> updateToggle(feature, enabled))
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
    public void addToggleListener(KnownFeature feature, ToggleListener listener) {
        listeners.computeIfAbsent(feature, k -> new CopyOnWriteArrayList<>()).add(listener);
        logger.debug("Added listener for feature: {}", feature.key());
    }

    @Override
    public void removeToggleListener(KnownFeature feature, ToggleListener listener) {
        listeners.computeIfPresent(feature, (k, list) -> {
            list.remove(listener);
            return list.isEmpty() ? null : list;
        });
        logger.debug("Removed listener for feature: {}", feature.key());
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
     * Notify all listeners of a feature change.
     */
    private void notifyListeners(KnownFeature feature, boolean oldValue, boolean newValue) {
        Option.option(listeners.get(feature))
              .filter(list -> !list.isEmpty())
              .onPresent(featureListeners -> {
                  for (var listener : featureListeners) {
                      try {
                          listener.onToggleChanged(feature, oldValue, newValue);
                      } catch (Exception e) {
                          logger.error("Error notifying toggle listener for feature {}: {}",
                                       feature.key(),
                                       e.getMessage(),
                                       e);
                      }
                  }
              });
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
