package org.pragmatica.dht;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

/**
 * Client interface for distributed DHT operations.
 * Coordinates reads and writes across multiple nodes with quorum semantics.
 *
 * <p>Implementations handle:
 * <ul>
 *   <li>Routing to appropriate nodes based on consistent hashing</li>
 *   <li>Quorum reads and writes</li>
 *   <li>Retry logic for failed nodes</li>
 *   <li>Timeout handling</li>
 * </ul>
 */
public interface DHTClient {

    /**
     * Get a value by key.
     * Reads from readQuorum nodes and returns the value if found.
     *
     * @param key the key to look up
     * @return the value if found, or empty if not present on any replica
     */
    Promise<Option<byte[]>> get(byte[] key);

    /**
     * Get a value by string key.
     */
    default Promise<Option<byte[]>> get(String key) {
        return get(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Put a value.
     * Writes to writeQuorum nodes before returning success.
     *
     * @param key   the key
     * @param value the value to store
     * @return promise that completes when quorum writes succeed
     */
    Promise<Unit> put(byte[] key, byte[] value);

    /**
     * Put a value with string key.
     */
    default Promise<Unit> put(String key, byte[] value) {
        return put(key.getBytes(java.nio.charset.StandardCharsets.UTF_8), value);
    }

    /**
     * Remove a value.
     * Removes from writeQuorum nodes before returning success.
     *
     * @param key the key to remove
     * @return true if the key was found and removed from at least one node
     */
    Promise<Boolean> remove(byte[] key);

    /**
     * Remove a value with string key.
     */
    default Promise<Boolean> remove(String key) {
        return remove(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Check if a key exists.
     * Checks readQuorum nodes.
     *
     * @param key the key to check
     * @return true if the key exists on any responding node
     */
    Promise<Boolean> exists(byte[] key);

    /**
     * Check if a key exists with string key.
     */
    default Promise<Boolean> exists(String key) {
        return exists(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Get the partition for a key.
     */
    Partition partitionFor(byte[] key);

    /**
     * Get the partition for a string key.
     */
    default Partition partitionFor(String key) {
        return partitionFor(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
