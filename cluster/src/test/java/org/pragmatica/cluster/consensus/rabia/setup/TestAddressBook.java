package org.pragmatica.cluster.consensus.rabia.setup;

import org.pragmatica.cluster.net.AddressBook;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.net.NodeInfo;
import org.pragmatica.lang.Option;

import java.net.SocketAddress;

// Address book implementation for testing
record TestAddressBook(int clusterSize) implements AddressBook {
    @Override
    public Option<NodeInfo> get(NodeId id) {
        throw new UnsupportedOperationException("Not implemented for tests");
    }

    @Override
    public Option<NodeId> reverseLookup(SocketAddress socketAddress) {
        throw new UnsupportedOperationException("Not implemented for tests");
    }

    @Override
    public int quorumSize() {
        return (clusterSize / 2) + 1;
    }
}
