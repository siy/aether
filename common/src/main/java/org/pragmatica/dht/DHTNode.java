package org.pragmatica.dht;

import org.pragmatica.dht.storage.StorageEngine;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

/**
 * Local DHT node that handles storage operations.
 * Provides local data access and can be integrated with MessageRouter
 * for handling remote requests.
 *
 * @param <N> Node identifier type
 */
public final class DHTNode<N extends Comparable<N>> {
    private final N nodeId;
    private final StorageEngine storage;
    private final ConsistentHashRing<N> ring;
    private final DHTConfig config;

    private DHTNode(N nodeId, StorageEngine storage, ConsistentHashRing<N> ring, DHTConfig config) {
        this.nodeId = nodeId;
        this.storage = storage;
        this.ring = ring;
        this.config = config;
    }

    /**
     * Create a new DHT node.
     *
     * @param nodeId  this node's identifier
     * @param storage storage engine for local data
     * @param ring    consistent hash ring for routing
     * @param config  DHT configuration
     */
    public static <N extends Comparable<N>> DHTNode<N> dhtNode(N nodeId,
                                                               StorageEngine storage,
                                                               ConsistentHashRing<N> ring,
                                                               DHTConfig config) {
        return new DHTNode<>(nodeId, storage, ring, config);
    }

    /**
     * Get the node's identifier.
     */
    public N nodeId() {
        return nodeId;
    }

    /**
     * Get the configuration.
     */
    public DHTConfig config() {
        return config;
    }

    /**
     * Get the consistent hash ring.
     */
    public ConsistentHashRing<N> ring() {
        return ring;
    }

    /**
     * Get a value from local storage.
     */
    public Promise<Option<byte[] >> getLocal(byte[] key) {
        return storage.get(key);
    }

    /**
     * Put a value to local storage.
     */
    public Promise<Unit> putLocal(byte[] key, byte[] value) {
        return storage.put(key, value);
    }

    /**
     * Remove a value from local storage.
     */
    public Promise<Boolean> removeLocal(byte[] key) {
        return storage.remove(key);
    }

    /**
     * Check if key exists in local storage.
     */
    public Promise<Boolean> existsLocal(byte[] key) {
        return storage.exists(key);
    }

    /**
     * Get the partition for a key.
     */
    public Partition partitionFor(byte[] key) {
        return ring.partitionFor(key);
    }

    /**
     * Check if this node is responsible for a key (as primary or replica).
     */
    public boolean isResponsibleFor(byte[] key) {
        return ring.nodesFor(key,
                             config.replicationFactor())
                   .contains(nodeId);
    }

    /**
     * Check if this node is the primary for a key.
     */
    public boolean isPrimaryFor(byte[] key) {
        return ring.primaryFor(key)
                   .map(nodeId::equals)
                   .orElse(false);
    }

    /**
     * Get the local storage size.
     */
    public long localSize() {
        return storage.size();
    }

    /**
     * Clear local storage.
     */
    public Promise<Unit> clearLocal() {
        return storage.clear();
    }

    /**
     * Shutdown the node and release resources.
     */
    public Promise<Unit> shutdown() {
        return storage.shutdown();
    }

    /**
     * Handle a get request (for message routing integration).
     */
    public void handleGetRequest(DHTMessage.GetRequest request,
                                 java.util.function.Consumer<DHTMessage.GetResponse> responseHandler) {
        storage.get(request.key())
               .onSuccess(value -> responseHandler.accept(
        new DHTMessage.GetResponse(request.requestId(),
                                   value)));
    }

    /**
     * Handle a put request (for message routing integration).
     */
    public void handlePutRequest(DHTMessage.PutRequest request,
                                 java.util.function.Consumer<DHTMessage.PutResponse> responseHandler) {
        storage.put(request.key(),
                    request.value())
               .onSuccess(_ -> responseHandler.accept(
        new DHTMessage.PutResponse(request.requestId(),
                                   true)))
               .onFailure(_ -> responseHandler.accept(
        new DHTMessage.PutResponse(request.requestId(),
                                   false)));
    }

    /**
     * Handle a remove request (for message routing integration).
     */
    public void handleRemoveRequest(DHTMessage.RemoveRequest request,
                                    java.util.function.Consumer<DHTMessage.RemoveResponse> responseHandler) {
        storage.remove(request.key())
               .onSuccess(found -> responseHandler.accept(
        new DHTMessage.RemoveResponse(request.requestId(),
                                      found)));
    }

    /**
     * Handle an exists request (for message routing integration).
     */
    public void handleExistsRequest(DHTMessage.ExistsRequest request,
                                    java.util.function.Consumer<DHTMessage.ExistsResponse> responseHandler) {
        storage.exists(request.key())
               .onSuccess(exists -> responseHandler.accept(
        new DHTMessage.ExistsResponse(request.requestId(),
                                      exists)));
    }
}
