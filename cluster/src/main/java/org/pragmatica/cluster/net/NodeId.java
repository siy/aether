package org.pragmatica.cluster.net;

import org.pragmatica.utility.ULID;

/// Cluster node ID
public interface NodeId extends Comparable<NodeId> {
    String id();

    /// Create new node ID from the given string.
    static NodeId nodeId(String id) {
        record nodeId(String id) implements NodeId {
            @Override
            public int compareTo(NodeId o) {
                return id().compareTo(o.id());
            }
        }
        return new nodeId(id);
    }

    /// Automatically generate unique node ID.
    static NodeId randomNodeId() {
        return nodeId(ULID.randomULID().encoded());
    }
}
