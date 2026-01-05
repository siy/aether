package org.pragmatica.aether.metrics.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe ring buffer for cluster events with time-based retention.
 * <p>
 * Features:
 * - Fixed capacity (1000 events default)
 * - Time-based eviction (2 hours default)
 * - Lock-free reads when possible
 * - Supports filtering by timestamp
 */
public final class EventBuffer {
    private static final int DEFAULT_CAPACITY = 1000;
    private static final long DEFAULT_RETENTION_MS = 2 * 60 * 60 * 1000L;

    // 2 hours
    private final ClusterEvent[] buffer;
    private final int capacity;
    private final long retentionMs;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private int head = 0;
    private int size = 0;

    private EventBuffer(int capacity, long retentionMs) {
        this.capacity = capacity;
        this.retentionMs = retentionMs;
        this.buffer = new ClusterEvent[capacity];
    }

    public static EventBuffer eventBuffer() {
        return new EventBuffer(DEFAULT_CAPACITY, DEFAULT_RETENTION_MS);
    }

    public static EventBuffer eventBuffer(int capacity) {
        return new EventBuffer(capacity, DEFAULT_RETENTION_MS);
    }

    public static EventBuffer eventBuffer(int capacity, long retentionMs) {
        return new EventBuffer(capacity, retentionMs);
    }

    /**
     * Add an event to the buffer.
     */
    public void publish(ClusterEvent event) {
        if (event == null) {
            return;
        }
        lock.writeLock()
            .lock();
        try{
            evictExpired();
            buffer[head] = event;
            head = (head + 1) % capacity;
            if (size < capacity) {
                size++;
            }
        } finally{
            lock.writeLock()
                .unlock();
        }
    }

    /**
     * Get all events since the given timestamp (inclusive).
     */
    public List<ClusterEvent> since(long timestamp) {
        lock.readLock()
            .lock();
        try{
            var result = new ArrayList<ClusterEvent>(size);
            long cutoff = System.currentTimeMillis() - retentionMs;
            for (int i = 0; i < size; i++) {
                int idx = (head - size + i + capacity) % capacity;
                var event = buffer[idx];
                if (event != null && event.timestamp() >= timestamp && event.timestamp() >= cutoff) {
                    result.add(event);
                }
            }
            return result;
        } finally{
            lock.readLock()
                .unlock();
        }
    }

    /**
     * Get the most recent N events.
     */
    public List<ClusterEvent> recent(int count) {
        lock.readLock()
            .lock();
        try{
            int actualCount = Math.min(count, size);
            var result = new ArrayList<ClusterEvent>(actualCount);
            long cutoff = System.currentTimeMillis() - retentionMs;
            int added = 0;
            // Iterate from newest to oldest
            for (int i = 0; i < size && added < actualCount; i++) {
                int idx = (head - 1 - i + capacity) % capacity;
                var event = buffer[idx];
                if (event != null && event.timestamp() >= cutoff) {
                    result.addFirst(event);
                    added++;
                }
            }
            return result;
        } finally{
            lock.readLock()
                .unlock();
        }
    }

    /**
     * Get all events (up to retention period).
     */
    public List<ClusterEvent> all() {
        return since(0);
    }

    /**
     * Get events of a specific type.
     */
    public <T extends ClusterEvent> List<T> ofType(Class<T> type) {
        lock.readLock()
            .lock();
        try{
            var result = new ArrayList<T>();
            long cutoff = System.currentTimeMillis() - retentionMs;
            for (int i = 0; i < size; i++) {
                int idx = (head - size + i + capacity) % capacity;
                var event = buffer[idx];
                if (event != null && type.isInstance(event) && event.timestamp() >= cutoff) {
                    result.add(type.cast(event));
                }
            }
            return result;
        } finally{
            lock.readLock()
                .unlock();
        }
    }

    /**
     * Get current event count (excluding expired).
     */
    public int count() {
        lock.readLock()
            .lock();
        try{
            long cutoff = System.currentTimeMillis() - retentionMs;
            int count = 0;
            for (int i = 0; i < size; i++) {
                int idx = (head - size + i + capacity) % capacity;
                var event = buffer[idx];
                if (event != null && event.timestamp() >= cutoff) {
                    count++;
                }
            }
            return count;
        } finally{
            lock.readLock()
                .unlock();
        }
    }

    /**
     * Clear all events.
     */
    public void clear() {
        lock.writeLock()
            .lock();
        try{
            for (int i = 0; i < capacity; i++) {
                buffer[i] = null;
            }
            head = 0;
            size = 0;
        } finally{
            lock.writeLock()
                .unlock();
        }
    }

    private void evictExpired() {
        // Called with write lock held
        long cutoff = System.currentTimeMillis() - retentionMs;
        int evicted = 0;
        for (int i = 0; i < size; i++) {
            int idx = (head - size + i + capacity) % capacity;
            var event = buffer[idx];
            if (event != null && event.timestamp() < cutoff) {
                buffer[idx] = null;
                evicted++;
            } else {
                break;
            }
        }
        size -= evicted;
    }
}
