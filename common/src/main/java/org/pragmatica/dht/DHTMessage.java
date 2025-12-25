package org.pragmatica.dht;

import org.pragmatica.lang.Option;
import org.pragmatica.message.Message;

/**
 * Messages for DHT operations between nodes.
 */
public sealed interface DHTMessage extends Message.Wired {

    /**
     * Request to get a value.
     */
    record GetRequest(String requestId, byte[] key) implements DHTMessage {}

    /**
     * Response to a get request.
     */
    record GetResponse(String requestId, Option<byte[]> value) implements DHTMessage {}

    /**
     * Request to put a value.
     */
    record PutRequest(String requestId, byte[] key, byte[] value) implements DHTMessage {}

    /**
     * Response to a put request.
     */
    record PutResponse(String requestId, boolean success) implements DHTMessage {}

    /**
     * Request to remove a value.
     */
    record RemoveRequest(String requestId, byte[] key) implements DHTMessage {}

    /**
     * Response to a remove request.
     */
    record RemoveResponse(String requestId, boolean found) implements DHTMessage {}

    /**
     * Request to check if key exists.
     */
    record ExistsRequest(String requestId, byte[] key) implements DHTMessage {}

    /**
     * Response to exists request.
     */
    record ExistsResponse(String requestId, boolean exists) implements DHTMessage {}
}
