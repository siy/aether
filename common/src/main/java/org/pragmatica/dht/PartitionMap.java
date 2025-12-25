package org.pragmatica.dht;

import org.pragmatica.lang.Promise;

import java.util.List;
import java.util.Set;

/**
 * Maps partitions to nodes.
 * Implementations may use local computation or distributed consensus.
 *
 * @param <N> Node identifier type
 */
public interface PartitionMap<N> {

    /**
     * Get the nodes responsible for a partition.
     * Returns the primary node first, followed by replicas.
     *
     * @param partition the partition to look up
     * @param replicaCount number of nodes to return (primary + replicas)
     * @return ordered list of nodes, with primary first
     */
    Promise<List<N>> nodesFor(Partition partition, int replicaCount);

    /**
     * Get the primary node for a partition.
     */
    default Promise<N> primaryFor(Partition partition) {
        return nodesFor(partition, 1)
            .map(nodes -> nodes.isEmpty() ? null : nodes.getFirst());
    }

    /**
     * Get all partitions owned by a node (as primary).
     */
    Promise<Set<Partition>> partitionsFor(N node);

    /**
     * Get all partitions where a node is a replica (including primary).
     */
    Promise<Set<Partition>> allPartitionsFor(N node, int replicaCount);

    /**
     * Update partition assignments when topology changes.
     * Called when nodes join or leave the cluster.
     *
     * @param nodes current set of nodes in the cluster
     */
    Promise<Void> updateTopology(Set<N> nodes);
}
