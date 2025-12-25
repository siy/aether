package org.pragmatica.dht.storage;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory storage engine backed by ConcurrentHashMap.
 * Thread-safe and suitable for development and testing.
 * Data is not persisted across restarts.
 */
public final class MemoryStorageEngine implements StorageEngine {
    private final ConcurrentHashMap<ByteArrayKey, byte[]> data = new ConcurrentHashMap<>();

    private MemoryStorageEngine() {}

    public static MemoryStorageEngine create() {
        return new MemoryStorageEngine();
    }

    @Override
    public Promise<Option<byte[]>> get(byte[] key) {
        byte[] value = data.get(new ByteArrayKey(key));
        return Promise.success(value != null ? Option.some(value.clone()) : Option.none());
    }

    @Override
    public Promise<Unit> put(byte[] key, byte[] value) {
        data.put(new ByteArrayKey(key), value.clone());
        return Promise.success(Unit.unit());
    }

    @Override
    public Promise<Boolean> remove(byte[] key) {
        byte[] removed = data.remove(new ByteArrayKey(key));
        return Promise.success(removed != null);
    }

    @Override
    public Promise<Boolean> exists(byte[] key) {
        return Promise.success(data.containsKey(new ByteArrayKey(key)));
    }

    @Override
    public long size() {
        return data.size();
    }

    @Override
    public Promise<Unit> clear() {
        data.clear();
        return Promise.success(Unit.unit());
    }

    @Override
    public Promise<Unit> shutdown() {
        data.clear();
        return Promise.success(Unit.unit());
    }

    /**
     * Wrapper for byte[] to use as HashMap key with proper equals/hashCode.
     */
    private record ByteArrayKey(byte[] data) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ByteArrayKey that)) return false;
            return Arrays.equals(data, that.data);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }
    }
}
