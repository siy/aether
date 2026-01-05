package org.pragmatica.aether.ttm.model;
/**
 * Feature indices matching {@link org.pragmatica.aether.metrics.MinuteAggregate#toFeatureArray()} order.
 * <p>
 * Features: cpu_usage, heap_usage, event_loop_lag_ms, latency_ms, invocations,
 * gc_pause_ms, latency_p50, latency_p95, latency_p99, error_rate, event_count
 */
public final class FeatureIndex {
    public static final int CPU_USAGE = 0;
    public static final int HEAP_USAGE = 1;
    public static final int EVENT_LOOP_LAG_MS = 2;
    public static final int LATENCY_MS = 3;
    public static final int INVOCATIONS = 4;
    public static final int GC_PAUSE_MS = 5;
    public static final int LATENCY_P50 = 6;
    public static final int LATENCY_P95 = 7;
    public static final int LATENCY_P99 = 8;
    public static final int ERROR_RATE = 9;
    public static final int EVENT_COUNT = 10;
    public static final int FEATURE_COUNT = 11;

    private FeatureIndex() {}

    /**
     * Feature names in order (matches MinuteAggregate.featureNames()).
     */
    public static String[] featureNames() {
        return new String[] {"cpu_usage", "heap_usage", "event_loop_lag_ms", "latency_ms", "invocations", "gc_pause_ms", "latency_p50", "latency_p95",
        "latency_p99", "error_rate", "event_count"};
    }
}
