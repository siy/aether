package org.pragmatica.aether.metrics.events;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static facade for publishing cluster events from anywhere in the codebase.
 * <p>
 * Backed by a singleton EventBuffer. Must be initialized before use.
 * <p>
 * Usage:
 * <pre>{@code
 * // At startup
 * EventPublisher.initialize();
 *
 * // From anywhere
 * EventPublisher.publish(ClusterEvent.nodeJoined("node-1", "192.168.1.1:5150"));
 *
 * // Query events
 * var events = EventPublisher.since(timestamp);
 * var recent = EventPublisher.recent(10);
 * }</pre>
 */
public final class EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private static volatile EventBuffer buffer;

    private EventPublisher() {}

    /**
     * Initialize the event publisher with default settings.
     */
    public static void initialize() {
        if (buffer == null) {
            synchronized (EventPublisher.class) {
                if (buffer == null) {
                    buffer = EventBuffer.eventBuffer();
                    log.info("EventPublisher initialized with default buffer");
                }
            }
        }
    }

    /**
     * Initialize with custom buffer.
     */
    public static void initialize(EventBuffer eventBuffer) {
        synchronized (EventPublisher.class) {
            buffer = eventBuffer;
            log.info("EventPublisher initialized with custom buffer");
        }
    }

    /**
     * Get the underlying buffer (for direct queries).
     */
    public static EventBuffer buffer() {
        ensureInitialized();
        return buffer;
    }

    /**
     * Publish an event.
     */
    public static void publish(ClusterEvent event) {
        if (buffer == null) {
            log.trace("EventPublisher not initialized, dropping event: {}", event);
            return;
        }
        buffer.publish(event);
        log.debug("Published event: {}", event);
    }

    /**
     * Get events since timestamp.
     */
    public static List<ClusterEvent> since(long timestamp) {
        ensureInitialized();
        return buffer.since(timestamp);
    }

    /**
     * Get recent events.
     */
    public static List<ClusterEvent> recent(int count) {
        ensureInitialized();
        return buffer.recent(count);
    }

    /**
     * Get all events.
     */
    public static List<ClusterEvent> all() {
        ensureInitialized();
        return buffer.all();
    }

    /**
     * Get events of specific type.
     */
    public static <T extends ClusterEvent> List<T> ofType(Class<T> type) {
        ensureInitialized();
        return buffer.ofType(type);
    }

    /**
     * Get event count.
     */
    public static int count() {
        if (buffer == null) {
            return 0;
        }
        return buffer.count();
    }

    /**
     * Clear all events.
     */
    public static void clear() {
        if (buffer != null) {
            buffer.clear();
        }
    }

    /**
     * Shutdown and release buffer.
     */
    public static void shutdown() {
        synchronized (EventPublisher.class) {
            if (buffer != null) {
                buffer.clear();
                buffer = null;
                log.info("EventPublisher shutdown");
            }
        }
    }

    private static void ensureInitialized() {
        if (buffer == null) {
            throw new IllegalStateException("EventPublisher not initialized. Call initialize() first.");
        }
    }
}
