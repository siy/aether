package org.pragmatica.aether.forge;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.pragmatica.aether.forge.ForgeCluster.forgeCluster;

/**
 * Tests for cluster bootstrap and recovery scenarios.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Single node restart and recovery</li>
 *   <li>State persistence across restarts</li>
 *   <li>Rolling restart behavior</li>
 *   <li>Multiple node restarts</li>
 * </ul>
 */
@Execution(ExecutionMode.SAME_THREAD)
class BootstrapTest {
    private static final int BASE_PORT = 5110;
    private static final int BASE_MGMT_PORT = 5210;
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);
    private static final String TEST_ARTIFACT = "org.pragmatica-lite.aether:example-slice:0.7.2";

    private ForgeCluster cluster;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        cluster = forgeCluster(3, BASE_PORT, BASE_MGMT_PORT);
        httpClient = HttpClient.newBuilder()
                               .connectTimeout(Duration.ofSeconds(5))
                               .build();

        cluster.start()
               .await()
               .onFailure(cause -> {
                   throw new AssertionError("Cluster start failed: " + cause.message());
               });

        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> cluster.currentLeader().isPresent());
    }

    @AfterEach
    void tearDown() {
        if (cluster != null) {
            cluster.stop()
                   .await();
        }
    }

    @Test
    void nodeRestart_rejoinsCluster() {
        // Kill node-2
        cluster.killNode("node-2")
               .await();

        // Wait for cluster to stabilize with 2 nodes
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> cluster.nodeCount() == 2);

        // Cluster should still have quorum with 2 out of 3 nodes
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> cluster.currentLeader().isPresent());

        // Add a new node (ForgeCluster uses addNode instead of restartNode)
        cluster.addNode()
               .await();

        // Wait for all 3 nodes to be running
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> cluster.nodeCount() == 3);

        // Cluster should be fully formed again
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> cluster.currentLeader().isPresent());

        // Verify all nodes are healthy
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(this::allNodesHealthy);

        for (var node : cluster.status().nodes()) {
            var health = getNodeHealth(node.mgmtPort());
            assertThat(health).doesNotContain("\"error\"");
        }
    }

    @Test
    void nodeRestart_recoversState() {
        // Wait for all nodes to be healthy before deploying
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(this::allNodesHealthy);

        // Deploy a slice
        var leaderPort = cluster.getLeaderManagementPort()
                                .unwrap();
        deploySlice(leaderPort, TEST_ARTIFACT, 1);

        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> {
                   var slices = getSlices(leaderPort);
                   return slices.contains("example-slice");
               });

        // Kill a node
        cluster.killNode("node-3")
               .await();

        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> cluster.nodeCount() == 2);

        // Add a new node
        cluster.addNode()
               .await();

        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> cluster.nodeCount() == 3);

        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> cluster.currentLeader().isPresent());

        // Slice should still be visible (state recovered from consensus)
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> {
                   var currentLeaderPort = cluster.getLeaderManagementPort()
                                                  .or(leaderPort);
                   var slices = getSlices(currentLeaderPort);
                   return slices.contains("example-slice");
               });
    }

    @Test
    void rollingRestart_maintainsAvailability() {
        // Wait for all nodes to be healthy
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(this::allNodesHealthy);

        // Perform a rolling restart
        cluster.rollingRestart()
               .await()
               .onFailure(cause -> {
                   throw new AssertionError("Rolling restart failed: " + cause.message());
               });

        // After rolling restart, cluster should be healthy
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> cluster.currentLeader().isPresent());

        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(this::allNodesHealthy);

        var leaderPort = cluster.getLeaderManagementPort()
                                .unwrap();
        var health = getNodeHealth(leaderPort);
        assertThat(health).doesNotContain("\"error\"");
        assertThat(health).contains("\"nodeCount\":3");
    }

    @Test
    void multipleNodeRestarts_clusterRemainsFunctional() {
        // Wait for all nodes to be healthy
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(this::allNodesHealthy);

        // Restart each non-leader node one at a time
        for (int i = 2; i <= 3; i++) {
            var nodeId = "node-" + i;

            cluster.killNode(nodeId)
                   .await();

            await().atMost(WAIT_TIMEOUT)
                   .pollInterval(POLL_INTERVAL)
                   .until(() -> cluster.nodeCount() == 2);

            await().atMost(WAIT_TIMEOUT)
                   .pollInterval(POLL_INTERVAL)
                   .until(() -> cluster.currentLeader().isPresent());

            // Add a new node (ForgeCluster uses addNode instead of restartNode)
            cluster.addNode()
                   .await();

            await().atMost(WAIT_TIMEOUT)
                   .pollInterval(POLL_INTERVAL)
                   .until(() -> cluster.nodeCount() == 3);

            await().atMost(WAIT_TIMEOUT)
                   .pollInterval(POLL_INTERVAL)
                   .until(() -> cluster.currentLeader().isPresent());
        }

        // Final verification
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(this::allNodesHealthy);

        assertThat(cluster.nodeCount()).isEqualTo(3);
    }

    private boolean allNodesHealthy() {
        var status = cluster.status();
        return status.nodes()
                     .stream()
                     .allMatch(node -> checkNodeHealth(node.mgmtPort()));
    }

    private boolean checkNodeHealth(int port) {
        var request = HttpRequest.newBuilder()
                                 .uri(URI.create("http://localhost:" + port + "/api/health"))
                                 .GET()
                                 .timeout(Duration.ofSeconds(5))
                                 .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().contains("\"quorum\":true");
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private String getNodeHealth(int port) {
        var request = HttpRequest.newBuilder()
                                 .uri(URI.create("http://localhost:" + port + "/api/health"))
                                 .GET()
                                 .timeout(Duration.ofSeconds(5))
                                 .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    private String getSlices(int port) {
        var request = HttpRequest.newBuilder()
                                 .uri(URI.create("http://localhost:" + port + "/api/slices"))
                                 .GET()
                                 .timeout(Duration.ofSeconds(5))
                                 .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (IOException | InterruptedException e) {
            return "";
        }
    }

    private void deploySlice(int port, String artifact, int instances) {
        var body = String.format("{\"artifact\":\"%s\",\"instances\":%d}", artifact, instances);
        var request = HttpRequest.newBuilder()
                                 .uri(URI.create("http://localhost:" + port + "/api/deploy"))
                                 .header("Content-Type", "application/json")
                                 .POST(HttpRequest.BodyPublishers.ofString(body))
                                 .timeout(Duration.ofSeconds(10))
                                 .build();
        try {
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode())
                .as("Deploy should succeed. Response: %s", response.body())
                .isIn(200, 201, 202);
        } catch (IOException | InterruptedException e) {
            throw new AssertionError("Deploy request failed: " + e.getMessage(), e);
        }
    }
}
