package org.pragmatica.aether.agent.features;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.pragmatica.aether.agent.features.FeatureToggle.KnownFeature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.pragmatica.aether.agent.features.FeatureToggle.KnownFeature.AGENT_ENABLED;
import static org.pragmatica.aether.agent.features.FeatureToggle.KnownFeature.AGENT_SHADOW_MODE;

/**
 * Tests for the Option-based FeatureToggle API improvements.
 */
@DisplayName("Feature Toggle Option API Tests")
class FeatureToggleOptionTest {

    private SimpleFeatureToggle featureToggle;

    @BeforeEach
    void setUp() {
        featureToggle = new SimpleFeatureToggle();
    }

    @Test
    @DisplayName("Test Option-based fromKey method")
    void testFromKeyReturnsOption() {
        // Test with valid key
        KnownFeature.fromKey("agent.enabled")
                    .onEmpty(Assertions::fail)
                    .onPresent(value -> assertThat(value).isEqualTo(AGENT_ENABLED));

        // Test with invalid key
        KnownFeature.fromKey("non.existent.feature")
                    .onPresentRun(Assertions::fail);

        // Test Option chaining with valid key
        KnownFeature.fromKey("llm.local.enabled")
                    .map(KnownFeature::key)
                    .onEmpty(Assertions::fail)
                    .onPresent(value -> assertThat(value).isEqualTo("llm.local.enabled"));

        // Test Option chaining with invalid key
        KnownFeature.fromKey("unknown")
                    .map(KnownFeature::key)
                    .onPresentRun(Assertions::fail);
    }

    @Test
    @DisplayName("Test basic feature state checking")
    void testFeatureStateChecking() {
        // Test with enabled feature
        assertThat(featureToggle.isEnabled(AGENT_ENABLED)).isTrue();

        // Test with disabled feature
        assertThat(featureToggle.isEnabled(AGENT_SHADOW_MODE)).isFalse();

        // Test after changing feature state
        featureToggle.setEnabled(AGENT_SHADOW_MODE, true);
        assertThat(featureToggle.isEnabled(AGENT_SHADOW_MODE)).isTrue();
    }

    @Test
    @DisplayName("Test Option-based config loading with onPresent/onEmpty")
    void testOptionBasedConfigLoading() {
        // Track which features were processed
        var processedFeatures = new ArrayList<String>();
        var unknownKeys = new ArrayList<String>();

        var config = Map.of(
                "agent.enabled", false,
                "unknown.feature", true,
                "llm.local.enabled", true,
                "another.unknown", false
        );

        // Test the Option-based processing logic similar to SimpleFeatureToggle
        config.forEach((key, enabled) -> {
            KnownFeature.fromKey(key)
                        .onPresent(feature -> processedFeatures.add(feature.key()))
                        .onEmpty(() -> unknownKeys.add(key));
        });

        // Verify results
        assertThat(processedFeatures).containsExactlyInAnyOrder("agent.enabled", "llm.local.enabled");
        assertThat(unknownKeys).containsExactlyInAnyOrder("unknown.feature", "another.unknown");
    }

    @Test
    @DisplayName("Test Option convenience methods")
    void testOptionConvenienceMethods() {
        // Test onPresent with side effects
        var actions = new ArrayList<String>();

        KnownFeature.fromKey("agent.enabled")
                    .onPresent(feature -> actions.add("processed: " + feature.key()));

        KnownFeature.fromKey("unknown")
                    .onPresentRun(() -> actions.add("should not be called"));

        assertThat(actions).containsExactly("processed: agent.enabled");

        // Test functional combination without extraction
        KnownFeature.fromKey("agent.enabled")
                    .map(feature -> featureToggle.isEnabled(feature))
                    .onEmpty(Assertions::fail)
                    .onPresent(enabled -> assertThat(enabled).isTrue());

        // Test with fallback using or() for missing features
        var unknownFeatureEnabled = KnownFeature.fromKey("unknown")
                                                .map(feature -> featureToggle.isEnabled(feature))
                                                .or(() -> false);
        assertThat(unknownFeatureEnabled).isFalse();
    }

    @Test
    @DisplayName("Test clean Option patterns without value extraction")
    void testCleanOptionPatterns() {
        // Pattern 1: Direct validation without extraction
        KnownFeature.fromKey("agent.enabled")
                    .onEmpty(() -> fail("Feature should exist"))
                    .onPresent(feature -> {
                        assertThat(feature.key()).isEqualTo("agent.enabled");
                        assertThat(feature.defaultValue()).isTrue();
                        assertThat(featureToggle.isEnabled(feature)).isTrue();
                    });

        // Pattern 2: Chained operations with validation
        KnownFeature.fromKey("llm.cloud.enabled")
                    .filter(feature -> !feature.defaultValue()) // Should pass - default is false
                    .onEmpty(() -> fail("Feature should be found and pass filter"))
                    .onPresent(feature -> assertThat(!featureToggle.isEnabled(feature)).isTrue()); // Should be disabled by default

        // Pattern 3: Side effects with onPresent, then validation
        KnownFeature.fromKey("agent.shadow_mode")
                    .onPresent(feature -> featureToggle.setEnabled(feature, true)) // Enable it first
                    .onPresent(feature -> {
                        assertThat(featureToggle.isEnabled(feature)).isTrue();
                    });

        // Pattern 4: Conditional execution based on feature existence
        var configUpdates = new ArrayList<String>();

        List.of("agent.enabled", "unknown.feature", "llm.local.enabled", "another.unknown")
            .forEach(key -> KnownFeature.fromKey(key)
                                        .onPresent(feature -> configUpdates.add("Found: " + feature.key()))
                                        .onEmpty(() -> configUpdates.add("Missing: " + key)));

        assertThat(configUpdates).containsExactly(
                "Found: agent.enabled",
                "Missing: unknown.feature",
                "Found: llm.local.enabled",
                "Missing: another.unknown"
        );
    }
}