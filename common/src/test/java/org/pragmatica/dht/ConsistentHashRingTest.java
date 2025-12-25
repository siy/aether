package org.pragmatica.dht;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConsistentHashRingTest {

    @Test
    void create_empty_ring() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();

        assertThat(ring.isEmpty()).isTrue();
        assertThat(ring.nodeCount()).isZero();
        assertThat(ring.nodes()).isEmpty();
    }

    @Test
    void addNode_increases_node_count() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();

        ring.addNode("node-1");

        assertThat(ring.isEmpty()).isFalse();
        assertThat(ring.nodeCount()).isEqualTo(1);
        assertThat(ring.nodes()).containsExactly("node-1");
    }

    @Test
    void addNode_is_idempotent() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();

        ring.addNode("node-1");
        ring.addNode("node-1");

        assertThat(ring.nodeCount()).isEqualTo(1);
    }

    @Test
    void removeNode_removes_from_ring() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();
        ring.addNode("node-1");
        ring.addNode("node-2");

        ring.removeNode("node-1");

        assertThat(ring.nodeCount()).isEqualTo(1);
        assertThat(ring.nodes()).containsExactly("node-2");
    }

    @Test
    void removeNode_nonexistent_is_safe() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();

        ring.removeNode("nonexistent");

        assertThat(ring.isEmpty()).isTrue();
    }

    @Test
    void partitionFor_returns_valid_partition() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();
        ring.addNode("node-1");

        Partition partition = ring.partitionFor("test-key");

        assertThat(partition.value()).isGreaterThanOrEqualTo(0);
        assertThat(partition.value()).isLessThan(Partition.MAX_PARTITIONS);
    }

    @Test
    void partitionFor_is_deterministic() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();
        ring.addNode("node-1");

        Partition partition1 = ring.partitionFor("test-key");
        Partition partition2 = ring.partitionFor("test-key");

        assertThat(partition1).isEqualTo(partition2);
    }

    @Test
    void partitionFor_works_without_nodes() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();

        // Should not throw even with empty ring
        Partition partition = ring.partitionFor("test-key");

        assertThat(partition).isNotNull();
    }

    @Test
    void primaryFor_returns_node_when_ring_has_nodes() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();
        ring.addNode("node-1");

        var primary = ring.primaryFor("test-key");

        assertThat(primary).isPresent();
        assertThat(primary.get()).isEqualTo("node-1");
    }

    @Test
    void primaryFor_returns_empty_when_ring_empty() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();

        var primary = ring.primaryFor("test-key");

        assertThat(primary).isEmpty();
    }

    @Test
    void primaryFor_is_deterministic() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();
        ring.addNode("node-1");
        ring.addNode("node-2");
        ring.addNode("node-3");

        var primary1 = ring.primaryFor("test-key");
        var primary2 = ring.primaryFor("test-key");

        assertThat(primary1).isEqualTo(primary2);
    }

    @Test
    void nodesFor_returns_requested_number_of_replicas() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();
        ring.addNode("node-1");
        ring.addNode("node-2");
        ring.addNode("node-3");

        List<String> nodes = ring.nodesFor("test-key", 2);

        assertThat(nodes).hasSize(2);
    }

    @Test
    void nodesFor_returns_unique_nodes() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();
        ring.addNode("node-1");
        ring.addNode("node-2");
        ring.addNode("node-3");

        List<String> nodes = ring.nodesFor("test-key", 3);

        assertThat(nodes).hasSize(3);
        assertThat(nodes).containsExactlyInAnyOrder("node-1", "node-2", "node-3");
    }

    @Test
    void nodesFor_respects_node_count_limit() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();
        ring.addNode("node-1");
        ring.addNode("node-2");

        // Request more replicas than nodes
        List<String> nodes = ring.nodesFor("test-key", 5);

        assertThat(nodes).hasSize(2);
    }

    @Test
    void nodesFor_returns_empty_for_empty_ring() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();

        List<String> nodes = ring.nodesFor("test-key", 3);

        assertThat(nodes).isEmpty();
    }

    @Test
    void nodesFor_returns_empty_for_zero_replicas() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();
        ring.addNode("node-1");

        List<String> nodes = ring.nodesFor("test-key", 0);

        assertThat(nodes).isEmpty();
    }

    @Test
    void keys_are_distributed_across_nodes() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();
        ring.addNode("node-1");
        ring.addNode("node-2");
        ring.addNode("node-3");

        Map<String, Integer> distribution = new HashMap<>();
        int keyCount = 10000;

        for (int i = 0; i < keyCount; i++) {
            String key = "key-" + i;
            var primary = ring.primaryFor(key);
            assertThat(primary).isPresent();
            distribution.merge(primary.get(), 1, Integer::sum);
        }

        // All nodes should have keys
        assertThat(distribution).containsKeys("node-1", "node-2", "node-3");

        // Distribution should be roughly even (within 50% of ideal)
        int idealCount = keyCount / 3;
        for (int count : distribution.values()) {
            assertThat(count).isGreaterThan(idealCount / 2);
            assertThat(count).isLessThan(idealCount * 2);
        }
    }

    @Test
    void adding_node_redistributes_minimal_keys() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();
        ring.addNode("node-1");
        ring.addNode("node-2");

        // Record initial assignments
        Map<String, String> initialAssignments = new HashMap<>();
        int keyCount = 1000;
        for (int i = 0; i < keyCount; i++) {
            String key = "key-" + i;
            initialAssignments.put(key, ring.primaryFor(key).orElseThrow());
        }

        // Add a third node
        ring.addNode("node-3");

        // Count how many keys moved
        int movedCount = 0;
        for (int i = 0; i < keyCount; i++) {
            String key = "key-" + i;
            String newPrimary = ring.primaryFor(key).orElseThrow();
            if (!newPrimary.equals(initialAssignments.get(key))) {
                movedCount++;
            }
        }

        // With consistent hashing, roughly 1/3 of keys should move to the new node
        // Allow some variance due to virtual node distribution
        assertThat(movedCount).isGreaterThan(keyCount / 6);
        assertThat(movedCount).isLessThan(keyCount / 2);
    }

    @Test
    void removing_node_redistributes_only_its_keys() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();
        ring.addNode("node-1");
        ring.addNode("node-2");
        ring.addNode("node-3");

        // Record initial assignments
        Map<String, String> initialAssignments = new HashMap<>();
        int keyCount = 1000;
        for (int i = 0; i < keyCount; i++) {
            String key = "key-" + i;
            initialAssignments.put(key, ring.primaryFor(key).orElseThrow());
        }

        // Remove node-2
        ring.removeNode("node-2");

        // Keys that weren't on node-2 should stay with their original node
        for (int i = 0; i < keyCount; i++) {
            String key = "key-" + i;
            String originalNode = initialAssignments.get(key);
            if (!originalNode.equals("node-2")) {
                String newPrimary = ring.primaryFor(key).orElseThrow();
                assertThat(newPrimary).isEqualTo(originalNode);
            }
        }
    }

    @Test
    void string_and_bytes_produce_same_partition() {
        ConsistentHashRing<String> ring = ConsistentHashRing.create();
        ring.addNode("node-1");

        String key = "test-key";
        Partition fromString = ring.partitionFor(key);
        Partition fromBytes = ring.partitionFor(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThat(fromString).isEqualTo(fromBytes);
    }
}
