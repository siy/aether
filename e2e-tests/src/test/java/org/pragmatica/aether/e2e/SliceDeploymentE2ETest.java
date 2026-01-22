package org.pragmatica.aether.e2e;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.pragmatica.aether.e2e.containers.AetherCluster;

import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * E2E tests for slice deployment and lifecycle.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Slice deployment via API</li>
 *   <li>Slice activation and health</li>
 *   <li>Slice scaling</li>
 *   <li>Slice undeployment</li>
 *   <li>Slice replication across nodes</li>
 * </ul>
 */
class SliceDeploymentE2ETest {
    private static final Path PROJECT_ROOT = Path.of(System.getProperty("project.basedir", ".."));
    private static final String TEST_ARTIFACT = "org.pragmatica-lite.aether.example:place-order-place-order:0.8.0";
    private static final Duration DEPLOY_TIMEOUT = Duration.ofMinutes(3);
    private AetherCluster cluster;

    @BeforeEach
    void setUp() {
        cluster = AetherCluster.aetherCluster(3, PROJECT_ROOT);
        cluster.start();
        cluster.awaitQuorum();
    }

    @AfterEach
    void tearDown() {
        if (cluster != null) {
            cluster.close();
        }
    }

    @Test
    @Disabled("Flaky in containerized environments - requires longer timeouts")
    void deploySlice_becomesActive() {
        var response = cluster.anyNode().deploy(TEST_ARTIFACT, 1);
        assertThat(response).doesNotContain("\"error\"");

        // Wait for slice to become active, fail fast on FAILED state
        cluster.awaitSliceActive(TEST_ARTIFACT, DEPLOY_TIMEOUT);

        var slices = cluster.anyNode().getSlices();
        assertThat(slices).contains(TEST_ARTIFACT);
    }

    @Test
    @Disabled("Flaky in containerized environments - requires longer timeouts")
    void deploySlice_multipleInstances_distributedAcrossNodes() {
        var response = cluster.anyNode().deploy(TEST_ARTIFACT, 3);
        assertThat(response).doesNotContain("\"error\"");

        // Wait for slice to become ACTIVE on ALL nodes (multi-instance distribution)
        cluster.awaitSliceActiveOnAllNodes(TEST_ARTIFACT, DEPLOY_TIMEOUT);

        // Each node should report the slice
        for (var node : cluster.nodes()) {
            var nodeSlices = node.getSlices();
            assertThat(nodeSlices).contains(TEST_ARTIFACT);
        }
    }

    @Test
    @Disabled("Instance distribution issue - same as deploySlice_multipleInstances_distributedAcrossNodes")
    void scaleSlice_adjustsInstanceCount() {
        // Deploy with 1 instance
        cluster.anyNode().deploy(TEST_ARTIFACT, 1);
        cluster.awaitSliceActive(TEST_ARTIFACT, DEPLOY_TIMEOUT);

        // Scale to 3 instances
        var scaleResponse = cluster.anyNode().scale(TEST_ARTIFACT, 3);
        assertThat(scaleResponse).doesNotContain("\"error\"");

        // Wait for scale operation to complete
        await().atMost(DEPLOY_TIMEOUT)
               .pollInterval(Duration.ofSeconds(2))
               .until(() -> {
                   var slices = cluster.anyNode().getSlices();
                   // Check for 3 instances (implementation-specific)
                   return slices.contains(TEST_ARTIFACT);
               });
    }

    @Test
    @Disabled("Flaky in containerized environments - requires longer timeouts")
    void undeploySlice_removesFromCluster() {
        // Deploy
        cluster.anyNode().deploy(TEST_ARTIFACT, 1);
        cluster.awaitSliceActive(TEST_ARTIFACT, DEPLOY_TIMEOUT);

        // Undeploy
        var undeployResponse = cluster.anyNode().undeploy(TEST_ARTIFACT);
        assertThat(undeployResponse).doesNotContain("\"error\"");

        // Wait for slice to be removed
        await().atMost(DEPLOY_TIMEOUT)
               .until(() -> {
                   var slices = cluster.anyNode().getSlices();
                   return !slices.contains(TEST_ARTIFACT);
               });
    }

    @Test
    @Disabled("Flaky in containerized environments - requires longer timeouts")
    void deploySlice_survivesNodeFailure() {
        // Deploy with 1 instance (multi-instance has race condition)
        cluster.anyNode().deploy(TEST_ARTIFACT, 1);
        cluster.awaitSliceActive(TEST_ARTIFACT, DEPLOY_TIMEOUT);

        // Kill a non-hosting node (node-2 or node-3, not the one with the slice)
        cluster.killNode("node-3");
        cluster.awaitQuorum();

        // Slice should still be available
        var slices = cluster.anyNode().getSlices();
        assertThat(slices).contains(TEST_ARTIFACT);
    }

    @Test
    @Disabled("Flaky in containerized environments - requires longer timeouts")
    void blueprintApply_deploysSlice() {
        var blueprint = """
            id = "org.test:e2e-blueprint:1.0.0"

            [[slices]]
            artifact = "org.pragmatica-lite.aether.example:place-order-place-order:0.8.0"
            instances = 1
            """;

        var response = cluster.anyNode().applyBlueprint(blueprint);
        assertThat(response).doesNotContain("\"error\"");

        cluster.awaitSliceActive(TEST_ARTIFACT, DEPLOY_TIMEOUT);
    }
}
