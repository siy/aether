package org.pragmatica.aether.infra.aspect;

import org.pragmatica.lang.Option;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import static org.pragmatica.lang.Option.none;
import static org.pragmatica.lang.Option.option;
import static org.pragmatica.lang.Option.some;

/**
 * Default CacheStorage implementation using ConcurrentHashMap.
 * Provides thread-safe in-memory caching with optional TTL support,
 * max entries enforcement with LRU eviction, and eviction callbacks.
 */
public final class ConcurrentMapCacheStorage implements CacheStorage {
    private static final System.Logger LOG = System.getLogger(ConcurrentMapCacheStorage.class.getName());
    private static final int MAX_EVICTION_ATTEMPTS = 10;

    private final ConcurrentHashMap<ByteArrayWrapper, CacheEntry> cache = new ConcurrentHashMap<>();
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong memoryUsed = new AtomicLong(0);
    private final ScheduledExecutorService cleanupExecutor;
    private final Option<Duration> defaultTtl;
    private final int maxEntries;
    private final Clock clock;
    private final AtomicReference<EvictionListener> evictionListener = new AtomicReference<>();

    // LRU tracking - LinkedHashMap in access-order mode protected by lock
    private final LinkedHashMap<ByteArrayWrapper, Long> accessOrder;
    private final ReentrantLock lruLock = new ReentrantLock();

    private ConcurrentMapCacheStorage(Option<Duration> defaultTtl,
                                      Duration cleanupInterval,
                                      int maxEntries,
                                      Clock clock) {
        this.defaultTtl = Objects.requireNonNull(defaultTtl, "defaultTtl must not be null");
        Objects.requireNonNull(cleanupInterval, "cleanupInterval must not be null");
        this.maxEntries = Math.max(0, maxEntries);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        // Access-order LinkedHashMap for LRU tracking
        this.accessOrder = new LinkedHashMap<>(16, 0.75f, true);
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                                                                              var thread = new Thread(r, "cache-cleanup");
                                                                              thread.setDaemon(true);
                                                                              return thread;
                                                                          });
        this.cleanupExecutor.scheduleAtFixedRate(this::removeExpiredEntries,
                                                 cleanupInterval.toMillis(),
                                                 cleanupInterval.toMillis(),
                                                 TimeUnit.MILLISECONDS);
    }

    /**
     * Create a ConcurrentMapCacheStorage with default settings.
     * No default TTL (entries never expire unless explicitly set).
     * Cleanup runs every minute.
     * No max entries limit.
     *
     * @return New ConcurrentMapCacheStorage instance
     */
    public static ConcurrentMapCacheStorage concurrentMapCacheStorage() {
        return new ConcurrentMapCacheStorage(none(), Duration.ofMinutes(1), 0, Clock.systemUTC());
    }

    /**
     * Create a ConcurrentMapCacheStorage with a default TTL.
     *
     * @param defaultTtl Default time-to-live for entries without explicit TTL
     * @return New ConcurrentMapCacheStorage instance
     */
    public static ConcurrentMapCacheStorage concurrentMapCacheStorage(Duration defaultTtl) {
        Objects.requireNonNull(defaultTtl, "defaultTtl must not be null");
        return new ConcurrentMapCacheStorage(some(defaultTtl), Duration.ofMinutes(1), 0, Clock.systemUTC());
    }

    /**
     * Create a ConcurrentMapCacheStorage with custom settings.
     *
     * @param defaultTtl      Default time-to-live for entries (empty for no expiry)
     * @param cleanupInterval How often to run expired entry cleanup
     * @return New ConcurrentMapCacheStorage instance
     */
    public static ConcurrentMapCacheStorage concurrentMapCacheStorage(Option<Duration> defaultTtl,
                                                                      Duration cleanupInterval) {
        return new ConcurrentMapCacheStorage(defaultTtl, cleanupInterval, 0, Clock.systemUTC());
    }

    /**
     * Create a ConcurrentMapCacheStorage with custom settings including max entries.
     *
     * @param defaultTtl      Default time-to-live for entries (empty for no expiry)
     * @param cleanupInterval How often to run expired entry cleanup
     * @param maxEntries      Maximum number of entries (0 for unlimited)
     * @return New ConcurrentMapCacheStorage instance
     */
    public static ConcurrentMapCacheStorage concurrentMapCacheStorage(Option<Duration> defaultTtl,
                                                                      Duration cleanupInterval,
                                                                      int maxEntries) {
        return new ConcurrentMapCacheStorage(defaultTtl, cleanupInterval, maxEntries, Clock.systemUTC());
    }

    /**
     * Create a ConcurrentMapCacheStorage with custom settings including max entries and clock.
     *
     * @param defaultTtl      Default time-to-live for entries (empty for no expiry)
     * @param cleanupInterval How often to run expired entry cleanup
     * @param maxEntries      Maximum number of entries (0 for unlimited)
     * @param clock           Clock for time-based operations (for testability)
     * @return New ConcurrentMapCacheStorage instance
     */
    public static ConcurrentMapCacheStorage concurrentMapCacheStorage(Option<Duration> defaultTtl,
                                                                      Duration cleanupInterval,
                                                                      int maxEntries,
                                                                      Clock clock) {
        return new ConcurrentMapCacheStorage(defaultTtl, cleanupInterval, maxEntries, clock);
    }

    @Override
    public Option<byte[]> get(byte[] key) {
        Objects.requireNonNull(key, "key must not be null");
        var wrappedKey = new ByteArrayWrapper(key);
        var entry = cache.get(wrappedKey);
        if (entry == null) {
            misses.incrementAndGet();
            return none();
        }
        if (entry.isExpired(clock)) {
            // Atomic removal: only remove if the entry hasn't changed
            if (cache.remove(wrappedKey, entry)) {
                memoryUsed.addAndGet(- entry.value.length);
                removeLruEntry(wrappedKey);
                notifyEviction(key.clone(), entry.value.clone(), EvictionReason.EXPIRED);
            }
            misses.incrementAndGet();
            return none();
        }
        hits.incrementAndGet();
        updateLruAccess(wrappedKey);
        return some(entry.value.clone());
    }

    @Override
    public Option<byte[]> put(byte[] key, byte[] value, Option<Duration> ttl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        var wrappedKey = new ByteArrayWrapper(key);
        var effectiveTtl = ttl.isPresent()
                           ? ttl
                           : defaultTtl;
        var expiry = effectiveTtl.map(d -> clock.instant()
                                                .plus(d));
        lruLock.lock();
        try{
            // Only evict for new entries, not updates - check inside lock to avoid race
            if (!cache.containsKey(wrappedKey)) {
                evictIfNeededUnderLock();
            }
            var oldEntry = cache.put(wrappedKey, new CacheEntry(value.clone(), expiry));
            updateMemoryUsage(oldEntry, value);
            accessOrder.put(wrappedKey, System.nanoTime());
            return option(oldEntry)
                         .map(e -> e.value.clone());
        } finally{
            lruLock.unlock();
        }
    }

    @Override
    public boolean refresh(byte[] key, Duration newTtl) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(newTtl, "newTtl must not be null");
        var wrappedKey = new ByteArrayWrapper(key);
        var entry = cache.get(wrappedKey);
        if (entry == null || entry.isExpired(clock)) {
            return false;
        }
        var newExpiry = some(clock.instant()
                                  .plus(newTtl));
        cache.put(wrappedKey, new CacheEntry(entry.value, newExpiry));
        updateLruAccess(wrappedKey);
        return true;
    }

    @Override
    public Option<byte[]> getAndRefresh(byte[] key) {
        Objects.requireNonNull(key, "key must not be null");
        var wrappedKey = new ByteArrayWrapper(key);
        var result = new byte[1][];
        var wasExpired = new boolean[1];
        // Use computeIfPresent for atomic get-and-refresh
        cache.computeIfPresent(wrappedKey,
                               (k, entry) -> {
                                   if (entry.isExpired(clock)) {
                                       wasExpired[0] = true;
                                       memoryUsed.addAndGet(- entry.value.length);
                                       removeLruEntry(k);
                                       notifyEviction(k.data()
                                                       .clone(),
                                                      entry.value.clone(),
                                                      EvictionReason.EXPIRED);
                                       return null;
                                   }
                                   result[0] = entry.value.clone();
                                   // Refresh TTL using default TTL
        return defaultTtl.map(ttl -> new CacheEntry(entry.value,
                                                    some(clock.instant()
                                                              .plus(ttl))))
                         .or(entry);
                               });
        if (result[0] == null) {
            misses.incrementAndGet();
            return none();
        }
        hits.incrementAndGet();
        updateLruAccess(wrappedKey);
        return some(result[0]);
    }

    @Override
    public Option<byte[]> remove(byte[] key) {
        Objects.requireNonNull(key, "key must not be null");
        var wrappedKey = new ByteArrayWrapper(key);
        var removed = cache.remove(wrappedKey);
        if (removed != null) {
            memoryUsed.addAndGet(- removed.value.length);
            removeLruEntry(wrappedKey);
            notifyEviction(key.clone(), removed.value.clone(), EvictionReason.MANUAL);
            return some(removed.value.clone());
        }
        return none();
    }

    @Override
    public void clear() {
        lruLock.lock();
        try{
            cache.clear();
            accessOrder.clear();
            memoryUsed.set(0);
        } finally{
            lruLock.unlock();
        }
    }

    @Override
    public long size() {
        return cache.size();
    }

    @Override
    public long hits() {
        return hits.get();
    }

    @Override
    public long misses() {
        return misses.get();
    }

    @Override
    public long memoryUsedBytes() {
        return memoryUsed.get();
    }

    @Override
    public void resetMetrics() {
        hits.set(0);
        misses.set(0);
    }

    @Override
    public int invalidateMatching(Predicate<byte[]> keyPredicate) {
        Objects.requireNonNull(keyPredicate, "keyPredicate must not be null");
        var removed = 0;
        var iterator = cache.entrySet()
                            .iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (keyPredicate.test(entry.getKey()
                                       .data())) {
                iterator.remove();
                memoryUsed.addAndGet(- entry.getValue().value.length);
                removeLruEntry(entry.getKey());
                notifyEviction(entry.getKey()
                                    .data()
                                    .clone(),
                               entry.getValue().value
                                    .clone(),
                               EvictionReason.MANUAL);
                removed++;
            }
        }
        return removed;
    }

    @Override
    public void shutdown() {
        cleanupExecutor.shutdown();
        try{
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread()
                  .interrupt();
        }
    }

    @Override
    public void setEvictionListener(EvictionListener listener) {
        evictionListener.set(listener);
    }

    private void updateMemoryUsage(CacheEntry oldEntry, byte[] newValue) {
        var oldSize = oldEntry != null
                      ? oldEntry.value.length
                      : 0;
        memoryUsed.addAndGet(newValue.length - oldSize);
    }

    private void removeExpiredEntries() {
        long removedBytes = 0;
        var iterator = cache.entrySet()
                            .iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue()
                     .isExpired(clock)) {
                removedBytes += entry.getValue().value.length;
                iterator.remove();
                removeLruEntry(entry.getKey());
                notifyEviction(entry.getKey()
                                    .data()
                                    .clone(),
                               entry.getValue().value
                                    .clone(),
                               EvictionReason.EXPIRED);
            }
        }
        if (removedBytes > 0) {
            memoryUsed.addAndGet(- removedBytes);
        }
    }

    private void evictIfNeeded() {
        if (maxEntries <= 0) {
            return;
        }
        int attempts = 0;
        while (cache.size() >= maxEntries && attempts < MAX_EVICTION_ATTEMPTS) {
            attempts++;
            lruLock.lock();
            try{
                var lruKey = findLruKeyUnderLock();
                if (lruKey.isEmpty()) {
                    break;
                }
                lruKey.onPresent(this::evictEntry);
            } finally{
                lruLock.unlock();
            }
        }
    }

    /**
     * Evict entries if needed - must be called while holding lruLock.
     */
    private void evictIfNeededUnderLock() {
        if (maxEntries <= 0) {
            return;
        }
        int attempts = 0;
        while (cache.size() >= maxEntries && attempts < MAX_EVICTION_ATTEMPTS) {
            attempts++;
            var lruKey = findLruKeyUnderLock();
            if (lruKey.isEmpty()) {
                break;
            }
            lruKey.onPresent(this::evictEntry);
        }
    }

    private void evictEntry(ByteArrayWrapper evictedKey) {
        // Remove from LRU first (already under lock)
        accessOrder.remove(evictedKey);
        var evicted = cache.remove(evictedKey);
        if (evicted != null) {
            memoryUsed.addAndGet(- evicted.value.length);
            notifyEviction(evictedKey.data()
                                     .clone(),
                           evicted.value.clone(),
                           EvictionReason.CAPACITY);
        }
    }

    private void updateLruAccess(ByteArrayWrapper key) {
        lruLock.lock();
        try{
            accessOrder.put(key, System.nanoTime());
        } finally{
            lruLock.unlock();
        }
    }

    private void removeLruEntry(ByteArrayWrapper key) {
        lruLock.lock();
        try{
            accessOrder.remove(key);
        } finally{
            lruLock.unlock();
        }
    }

    /**
     * Find LRU key - must be called while holding lruLock.
     */
    private Option<ByteArrayWrapper> findLruKeyUnderLock() {
        // First entry in access-order LinkedHashMap is the LRU entry
        var it = accessOrder.entrySet()
                            .iterator();
        if (it.hasNext()) {
            return some(it.next()
                          .getKey());
        }
        return none();
    }

    private void notifyEviction(byte[] key, byte[] value, EvictionReason reason) {
        var listener = evictionListener.get();
        if (listener != null) {
            try{
                listener.onEviction(key, value, reason);
            } catch (Exception e) {
                LOG.log(System.Logger.Level.WARNING, "Eviction listener threw exception", e);
            }
        }
    }

    private record CacheEntry(byte[] value, Option<Instant> expiry) {
        boolean isExpired(Clock clock) {
            return expiry.map(exp -> clock.instant()
                                          .isAfter(exp))
                         .or(false);
        }
    }

    /**
     * Wrapper for byte[] to provide proper equals/hashCode for use as HashMap keys.
     * Defensively copies the input array to prevent external mutation.
     */
    private record ByteArrayWrapper(byte[] data) {
        ByteArrayWrapper(byte[] data) {
            this.data = data.clone();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ByteArrayWrapper w && Arrays.equals(data, w.data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }
    }
}
