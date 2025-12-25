package org.pragmatica.dht.storage;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

/**
 * Storage engine interface for DHT data storage.
 * Implementations may use in-memory storage, off-heap memory, or persistent storage.
 */
public interface StorageEngine {

    /**
     * Get a value by key.
     *
     * @param key the key to look up
     * @return the value if present, or empty option
     */
    Promise<Option<byte[]>> get(byte[] key);

    /**
     * Store a value.
     *
     * @param key   the key
     * @param value the value to store
     * @return promise that completes when value is stored
     */
    Promise<Unit> put(byte[] key, byte[] value);

    /**
     * Remove a value.
     *
     * @param key the key to remove
     * @return true if value was present and removed, false if not found
     */
    Promise<Boolean> remove(byte[] key);

    /**
     * Check if a key exists.
     *
     * @param key the key to check
     * @return true if key exists
     */
    Promise<Boolean> exists(byte[] key);

    /**
     * Get approximate number of entries.
     */
    long size();

    /**
     * Clear all entries.
     */
    Promise<Unit> clear();

    /**
     * Shutdown the storage engine and release resources.
     */
    Promise<Unit> shutdown();
}
