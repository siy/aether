package org.pragmatica.dht;

/**
 * Configuration for the distributed hash table.
 *
 * @param replicationFactor number of copies of each piece of data (including primary)
 * @param writeQuorum       minimum number of successful writes for operation to succeed
 * @param readQuorum        minimum number of successful reads for operation to succeed
 */
public record DHTConfig(
    int replicationFactor,
    int writeQuorum,
    int readQuorum
) {
    /**
     * Default configuration: 3 replicas, quorum of 2 for both reads and writes.
     */
    public static final DHTConfig DEFAULT = new DHTConfig(3, 2, 2);

    /**
     * Single-node configuration for testing.
     */
    public static final DHTConfig SINGLE_NODE = new DHTConfig(1, 1, 1);

    public DHTConfig {
        if (replicationFactor < 1) {
            throw new IllegalArgumentException("replicationFactor must be at least 1");
        }
        if (writeQuorum < 1 || writeQuorum > replicationFactor) {
            throw new IllegalArgumentException("writeQuorum must be between 1 and replicationFactor");
        }
        if (readQuorum < 1 || readQuorum > replicationFactor) {
            throw new IllegalArgumentException("readQuorum must be between 1 and replicationFactor");
        }
    }

    /**
     * Create a config with the given replication factor and majority quorum.
     */
    public static DHTConfig withReplication(int replicationFactor) {
        int quorum = (replicationFactor / 2) + 1;
        return new DHTConfig(replicationFactor, quorum, quorum);
    }

    /**
     * Check if reads and writes overlap (strong consistency guarantee).
     * R + W > N ensures that any read will see the most recent write.
     */
    public boolean isStronglyConsistent() {
        return readQuorum + writeQuorum > replicationFactor;
    }
}
