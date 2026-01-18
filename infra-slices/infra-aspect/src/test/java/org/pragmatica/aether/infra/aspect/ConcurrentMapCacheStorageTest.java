package org.pragmatica.aether.infra.aspect;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pragmatica.aether.infra.aspect.CacheStorage.EvictionReason;
import org.pragmatica.lang.Option;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.some;

class ConcurrentMapCacheStorageTest {
    private ConcurrentMapCacheStorage storage;

    @AfterEach
    void tearDown() {
        if (storage != null) {
            storage.shutdown();
        }
    }

    private static byte[] key(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] value(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Nested
    class BasicOperations {
        @BeforeEach
        void setUp() {
            storage = ConcurrentMapCacheStorage.concurrentMapCacheStorage();
        }

        @Test
        void put_and_get_returns_value() {
            var key = key("test-key");
            var value = value("test-value");

            storage.put(key, value, none());

            storage.get(key)
                   .onEmpty(Assertions::fail)
                   .onPresent(retrieved -> assertThat(retrieved).isEqualTo(value));
        }

        @Test
        void get_returns_empty_for_missing_key() {
            storage.get(key("nonexistent"))
                   .onPresent(_ -> Assertions.fail());
        }

        @Test
        void put_returns_old_value_on_update() {
            var key = key("test-key");
            var oldValue = value("old");
            var newValue = value("new");

            storage.put(key, oldValue, none());
            var result = storage.put(key, newValue, none());

            result.onEmpty(Assertions::fail)
                  .onPresent(old -> assertThat(old).isEqualTo(oldValue));
        }

        @Test
        void remove_returns_removed_value() {
            var key = key("test-key");
            var value = value("test-value");
            storage.put(key, value, none());

            storage.remove(key)
                   .onEmpty(Assertions::fail)
                   .onPresent(removed -> assertThat(removed).isEqualTo(value));

            storage.get(key)
                   .onPresent(_ -> Assertions.fail());
        }

        @Test
        void remove_returns_empty_for_missing_key() {
            storage.remove(key("nonexistent"))
                   .onPresent(_ -> Assertions.fail());
        }

        @Test
        void clear_removes_all_entries() {
            storage.put(key("key1"), value("value1"), none());
            storage.put(key("key2"), value("value2"), none());

            storage.clear();

            assertThat(storage.size()).isZero();
            storage.get(key("key1")).onPresent(_ -> Assertions.fail());
            storage.get(key("key2")).onPresent(_ -> Assertions.fail());
        }

        @Test
        void size_returns_entry_count() {
            assertThat(storage.size()).isZero();

            storage.put(key("key1"), value("value1"), none());
            assertThat(storage.size()).isEqualTo(1);

            storage.put(key("key2"), value("value2"), none());
            assertThat(storage.size()).isEqualTo(2);

            storage.remove(key("key1"));
            assertThat(storage.size()).isEqualTo(1);
        }
    }

    @Nested
    class TtlExpiration {
        private MutableClock testClock;

        @BeforeEach
        void setUp() {
            testClock = new MutableClock(Instant.now());
            storage = ConcurrentMapCacheStorage.concurrentMapCacheStorage(
                some(Duration.ofMinutes(5)),
                Duration.ofHours(1), // Cleanup won't run during test
                0,
                testClock
            );
        }

        @Test
        void get_returns_empty_for_expired_entry() {
            var key = key("test-key");
            storage.put(key, value("value"), some(Duration.ofMinutes(1)));

            // Advance clock past expiry
            testClock.advance(Duration.ofMinutes(2));

            storage.get(key)
                   .onPresent(_ -> Assertions.fail());
        }

        @Test
        void get_returns_value_before_expiry() {
            var key = key("test-key");
            var value = value("value");
            storage.put(key, value, some(Duration.ofMinutes(5)));

            // Advance clock but not past expiry
            testClock.advance(Duration.ofMinutes(3));

            storage.get(key)
                   .onEmpty(Assertions::fail)
                   .onPresent(retrieved -> assertThat(retrieved).isEqualTo(value));
        }

        @Test
        void refresh_extends_ttl() {
            var key = key("test-key");
            storage.put(key, value("value"), some(Duration.ofMinutes(1)));

            // Advance clock but refresh before expiry
            testClock.advance(Duration.ofSeconds(30));
            var refreshed = storage.refresh(key, Duration.ofMinutes(2));

            assertThat(refreshed).isTrue();

            // Advance past original expiry
            testClock.advance(Duration.ofSeconds(45));

            // Should still exist due to refresh
            storage.get(key)
                   .onEmpty(Assertions::fail);
        }

        @Test
        void refresh_returns_false_for_missing_key() {
            var refreshed = storage.refresh(key("nonexistent"), Duration.ofMinutes(1));
            assertThat(refreshed).isFalse();
        }

        @Test
        void getAndRefresh_extends_ttl() {
            var key = key("test-key");
            var value = value("value");
            storage.put(key, value, none()); // Uses default TTL of 5 minutes

            // Advance clock
            testClock.advance(Duration.ofMinutes(3));

            // Get and refresh
            storage.getAndRefresh(key)
                   .onEmpty(Assertions::fail)
                   .onPresent(retrieved -> assertThat(retrieved).isEqualTo(value));

            // Advance 4 more minutes (would be 7 total from original, past 5min default)
            testClock.advance(Duration.ofMinutes(4));

            // Should still exist due to refresh
            storage.get(key)
                   .onEmpty(Assertions::fail);
        }
    }

    @Nested
    class LruEviction {
        @BeforeEach
        void setUp() {
            storage = ConcurrentMapCacheStorage.concurrentMapCacheStorage(
                none(),
                Duration.ofHours(1),
                3 // Max 3 entries
            );
        }

        @Test
        void evicts_lru_when_max_reached() {
            storage.put(key("key1"), value("value1"), none());
            storage.put(key("key2"), value("value2"), none());
            storage.put(key("key3"), value("value3"), none());

            // Access key1 to make it recently used
            storage.get(key("key1"));

            // Add key4 - should evict key2 (LRU)
            storage.put(key("key4"), value("value4"), none());

            assertThat(storage.size()).isEqualTo(3);
            storage.get(key("key1")).onEmpty(Assertions::fail); // Still present
            storage.get(key("key2")).onPresent(_ -> Assertions.fail()); // Evicted
            storage.get(key("key3")).onEmpty(Assertions::fail); // Still present
            storage.get(key("key4")).onEmpty(Assertions::fail); // Just added
        }

        @Test
        void update_does_not_trigger_eviction() {
            storage.put(key("key1"), value("value1"), none());
            storage.put(key("key2"), value("value2"), none());
            storage.put(key("key3"), value("value3"), none());

            // Update existing key - should not evict
            storage.put(key("key1"), value("updated"), none());

            assertThat(storage.size()).isEqualTo(3);
            storage.get(key("key1"))
                   .onEmpty(Assertions::fail)
                   .onPresent(v -> assertThat(new String(v, StandardCharsets.UTF_8)).isEqualTo("updated"));
            storage.get(key("key2")).onEmpty(Assertions::fail);
            storage.get(key("key3")).onEmpty(Assertions::fail);
        }
    }

    @Nested
    class ConcurrentAccess {
        @BeforeEach
        void setUp() {
            storage = ConcurrentMapCacheStorage.concurrentMapCacheStorage(
                none(),
                Duration.ofHours(1),
                100
            );
        }

        @Test
        void concurrent_puts_and_gets_are_thread_safe() throws InterruptedException {
            int threadCount = 10;
            int operationsPerThread = 1000;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            List<Throwable> errors = new CopyOnWriteArrayList<>();

            for (int t = 0; t < threadCount; t++) {
                int threadId = t;
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < operationsPerThread; i++) {
                            var key = key("key-" + threadId + "-" + i);
                            var value = value("value-" + i);
                            storage.put(key, value, none());
                            storage.get(key);
                        }
                    } catch (Throwable e) {
                        errors.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            assertThat(errors).isEmpty();
        }
    }

    @Nested
    class MemoryTracking {
        @BeforeEach
        void setUp() {
            storage = ConcurrentMapCacheStorage.concurrentMapCacheStorage();
        }

        @Test
        void tracks_memory_usage() {
            assertThat(storage.memoryUsedBytes()).isZero();

            var value1 = value("12345"); // 5 bytes
            storage.put(key("key1"), value1, none());
            assertThat(storage.memoryUsedBytes()).isEqualTo(5);

            var value2 = value("1234567890"); // 10 bytes
            storage.put(key("key2"), value2, none());
            assertThat(storage.memoryUsedBytes()).isEqualTo(15);

            storage.remove(key("key1"));
            assertThat(storage.memoryUsedBytes()).isEqualTo(10);

            storage.clear();
            assertThat(storage.memoryUsedBytes()).isZero();
        }

        @Test
        void update_adjusts_memory_correctly() {
            storage.put(key("key"), value("short"), none()); // 5 bytes
            assertThat(storage.memoryUsedBytes()).isEqualTo(5);

            storage.put(key("key"), value("much longer value"), none()); // 17 bytes
            assertThat(storage.memoryUsedBytes()).isEqualTo(17);
        }
    }

    @Nested
    class InvalidationPatterns {
        @BeforeEach
        void setUp() {
            storage = ConcurrentMapCacheStorage.concurrentMapCacheStorage();
        }

        @Test
        void invalidateMatching_removes_matching_entries() {
            storage.put(key("cache:user:1"), value("data1"), none());
            storage.put(key("cache:order:2"), value("data2"), none());
            storage.put(key("cache:user:3"), value("data3"), none());

            int removed = storage.invalidateMatching(k -> {
                var keyStr = new String(k, StandardCharsets.UTF_8);
                return keyStr.contains(":user:");
            });

            assertThat(removed).isEqualTo(2);
            assertThat(storage.size()).isEqualTo(1);
            storage.get(key("cache:order:2")).onEmpty(Assertions::fail);
        }

        @Test
        void invalidateMatching_with_prefix_pattern() {
            storage.put(key("user:1:profile"), value("data1"), none());
            storage.put(key("user:1:settings"), value("data2"), none());
            storage.put(key("user:2:profile"), value("data3"), none());
            storage.put(key("order:1"), value("data4"), none());

            int removed = storage.invalidateMatching(k -> {
                var keyStr = new String(k, StandardCharsets.UTF_8);
                return keyStr.startsWith("user:1:");
            });

            assertThat(removed).isEqualTo(2);
            assertThat(storage.size()).isEqualTo(2);
            storage.get(key("user:1:profile")).onPresent(_ -> Assertions.fail());
            storage.get(key("user:1:settings")).onPresent(_ -> Assertions.fail());
            storage.get(key("user:2:profile")).onEmpty(Assertions::fail);
            storage.get(key("order:1")).onEmpty(Assertions::fail);
        }
    }

    @Nested
    class EvictionCallbacks {
        private List<EvictionEvent> events;

        @BeforeEach
        void setUp() {
            events = Collections.synchronizedList(new ArrayList<>());
        }

        @Test
        void notifies_on_manual_removal() {
            storage = ConcurrentMapCacheStorage.concurrentMapCacheStorage();
            storage.setEvictionListener((key, value, reason) ->
                events.add(new EvictionEvent(new String(key, StandardCharsets.UTF_8),
                                             new String(value, StandardCharsets.UTF_8),
                                             reason)));

            storage.put(key("key"), value("value"), none());
            storage.remove(key("key"));

            assertThat(events).hasSize(1);
            assertThat(events.getFirst().key()).isEqualTo("key");
            assertThat(events.getFirst().value()).isEqualTo("value");
            assertThat(events.getFirst().reason()).isEqualTo(EvictionReason.MANUAL);
        }

        @Test
        void notifies_on_capacity_eviction() {
            storage = ConcurrentMapCacheStorage.concurrentMapCacheStorage(
                none(),
                Duration.ofHours(1),
                2
            );
            storage.setEvictionListener((key, value, reason) ->
                events.add(new EvictionEvent(new String(key, StandardCharsets.UTF_8),
                                             new String(value, StandardCharsets.UTF_8),
                                             reason)));

            storage.put(key("key1"), value("value1"), none());
            storage.put(key("key2"), value("value2"), none());
            storage.put(key("key3"), value("value3"), none()); // Triggers eviction

            assertThat(events.stream()
                             .filter(e -> e.reason() == EvictionReason.CAPACITY)
                             .count()).isEqualTo(1);
        }

        @Test
        void notifies_on_expiration() {
            var testClock = new MutableClock(Instant.now());
            storage = ConcurrentMapCacheStorage.concurrentMapCacheStorage(
                some(Duration.ofMinutes(1)),
                Duration.ofHours(1),
                0,
                testClock
            );
            storage.setEvictionListener((key, value, reason) ->
                events.add(new EvictionEvent(new String(key, StandardCharsets.UTF_8),
                                             new String(value, StandardCharsets.UTF_8),
                                             reason)));

            storage.put(key("key"), value("value"), none());

            // Advance past expiry
            testClock.advance(Duration.ofMinutes(2));

            // Trigger expiration check via get
            storage.get(key("key"));

            assertThat(events).hasSize(1);
            assertThat(events.getFirst().reason()).isEqualTo(EvictionReason.EXPIRED);
        }

        record EvictionEvent(String key, String value, EvictionReason reason) {}
    }

    @Nested
    class Metrics {
        @BeforeEach
        void setUp() {
            storage = ConcurrentMapCacheStorage.concurrentMapCacheStorage();
        }

        @Test
        void tracks_hits_and_misses() {
            storage.put(key("key"), value("value"), none());

            // Hits
            storage.get(key("key"));
            storage.get(key("key"));

            // Misses
            storage.get(key("nonexistent"));

            assertThat(storage.hits()).isEqualTo(2);
            assertThat(storage.misses()).isEqualTo(1);
        }

        @Test
        void resetMetrics_clears_counters() {
            storage.put(key("key"), value("value"), none());
            storage.get(key("key"));
            storage.get(key("nonexistent"));

            storage.resetMetrics();

            assertThat(storage.hits()).isZero();
            assertThat(storage.misses()).isZero();
        }
    }

    @Nested
    class DefensiveCopies {
        @BeforeEach
        void setUp() {
            storage = ConcurrentMapCacheStorage.concurrentMapCacheStorage();
        }

        @Test
        void put_stores_copy_of_value() {
            var original = value("original");
            storage.put(key("key"), original, none());

            // Modify original array
            original[0] = 'X';

            // Stored value should be unchanged
            storage.get(key("key"))
                   .onEmpty(Assertions::fail)
                   .onPresent(retrieved -> assertThat(new String(retrieved, StandardCharsets.UTF_8)).isEqualTo("original"));
        }

        @Test
        void get_returns_copy_of_value() {
            storage.put(key("key"), value("value"), none());

            var retrieved = new AtomicReference<byte[]>();
            storage.get(key("key")).onPresent(retrieved::set);

            // Modify retrieved array
            retrieved.get()[0] = 'X';

            // Stored value should be unchanged
            storage.get(key("key"))
                   .onEmpty(Assertions::fail)
                   .onPresent(v -> assertThat(new String(v, StandardCharsets.UTF_8)).isEqualTo("value"));
        }
    }

    /**
     * Mutable clock for testing time-dependent behavior.
     */
    private static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
