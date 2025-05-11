package org.pragmatica.cluster.net.serializer.kryo;

import org.junit.jupiter.api.Test;
import org.pragmatica.cluster.consensus.rabia.Batch;
import org.pragmatica.cluster.consensus.rabia.Phase;
import org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.kvstore.KVCommand;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KryoSerializerTest {

    @Test
    void testRoundTripForProtocolMessage() {
        var serializer = KryoSerializer.kryoSerializer();

        var commands = List.of(new KVCommand.Put<>("k1", "v1"),
                               new KVCommand.Get<>("k2"),
                               new KVCommand.Remove<>("k3"));
        var message = new RabiaProtocolMessage.Synchronous.Propose<>(NodeId.randomNodeId(),
                                                                   new Phase(123L),
                                                                   Batch.batch(commands));

        var serialized = serializer.encode(message);
        var deserialized = serializer.decode(serialized);

        assertEquals(message, deserialized);
    }
}