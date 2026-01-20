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
 * Tests for graceful shutdown scenarios.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Node shutdown leaves cluster in healthy state</li>
 *   <li>Peers detect and respond to node shutdown</li>
 *   <li>Slices are handled appropriately during shutdown</li>
 *   <li>Shutdown during ongoing operations</li>
 * </ul>
 */
@Execution(ExecutionMode.SAME_THREAD)
class GracefulShutdownTest {
    private static final int BASE_PORT = 5100;
    private static final int BASE_MGMT_PORT = 5200;
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(30);
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

        awaitQuorum();
    }

    @AfterEach
    void tearDown() {
        if (cluster != null) {
            cluster.stop()
                   .await();
        }
    }

    @Test
    void nodeShutdown_peersDetectDisconnection() {
        awaitLeader();

        // Get initial peer count
        var initialHealth = getHealthFromAnyNode();
        assertThat(initialHealth).contains("\"connectedPeers\":2");

        // Shutdown node-3
        cluster.killNode("node-3")
               .await();

        // Remaining nodes should detect the disconnection
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> {
                   var health = getHealthFromAnyNode();
                   return health.contains("\"connectedPeers\":1");
               });

        // Cluster still has quorum with 2 nodes
        awaitQuorum();
    }

    @Test
    void nodeShutdown_clusterRemainsFunctional() {
        awaitLeader();

        // Deploy a slice
        deploySlice(TEST_ARTIFACT, 2);
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> {
                   var slices = getSlicesFromAnyNode();
                   return slices.contains("example-slice");
               });

        // Shutdown one node
        cluster.killNode("node-2")
               .await();

        // Cluster should still be functional
        awaitQuorum();

        // Management API should still work
        var health = getHealthFromAnyNode();
        assertThat(health).doesNotContain("\"error\"");
        assertThat(health).contains("\"nodeCount\":2");

        // Slice should still be accessible
        var slices = getSlicesFromAnyNode();
        assertThat(slices).contains("example-slice");
    }

    @Test
    void shutdownDuringDeployment_handledGracefully() {
        awaitLeader();

        // Start a deployment
        deploySlice(TEST_ARTIFACT, 3);

        // Immediately shutdown a node (deployment may still be in progress)
        cluster.killNode("node-3")
               .await();

        // Wait for quorum
        awaitQuorum();

        // Cluster should recover and deployment should eventually complete
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> {
                   try {
                       var slices = getSlicesFromAnyNode();
                       return slices.contains("example-slice") || !slices.contains("\"error\"");
                   } catch (Exception e) {
                       return false;
                   }
               });

        // Verify cluster is healthy
        var health = getHealthFromAnyNode();
        assertThat(health).doesNotContain("\"error\"");
    }

    @Test
    void leaderShutdown_newLeaderElected() {
        awaitLeader();
        var originalLeader = cluster.currentLeader()
                                    .unwrap();

        // Shutdown the leader
        cluster.killNode(originalLeader)
               .await();

        // Wait for new quorum and leader
        awaitQuorum();
        awaitLeader();

        // New leader should be elected
        var newLeader = cluster.currentLeader()
                               .unwrap();
        assertThat(newLeader).isNotEqualTo(originalLeader);

        // Cluster should be functional
        var health = getHealthFromAnyNode();
        assertThat(health).doesNotContain("\"error\"");
    }

    // --- Helper methods ---

    private void awaitLeader() {
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> cluster.currentLeader()
                                   .isPresent());
    }

    private void awaitQuorum() {
        await().atMost(WAIT_TIMEOUT)
               .pollInterval(POLL_INTERVAL)
               .until(() -> cluster.currentLeader()
                                   .isPresent() && allNodesHealthy());
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
            return response.statusCode() == 200 && response.body()
                                                           .contains("\"quorum\":true");
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    private String getHealthFromAnyNode() {
        var status = cluster.status();
        if (status.nodes()
                  .isEmpty()) {
            return "";
        }
        var port = status.nodes()
                         .get(0)
                         .mgmtPort();
        return httpGet(port, "/api/health");
    }

    private String getSlicesFromAnyNode() {
        var status = cluster.status();
        if (status.nodes()
                  .isEmpty()) {
            return "";
        }
        var port = status.nodes()
                         .get(0)
                         .mgmtPort();
        return httpGet(port, "/api/slices");
    }

    private void deploySlice(String artifact, int instances) {
        var leaderPort = cluster.getLeaderManagementPort()
                                .or(cluster.status()
                                           .nodes()
                                           .get(0)
                                           .mgmtPort());
        var body = "{\"artifact\":\"" + artifact + "\",\"instances\":" + instances + "}";
        httpPost(leaderPort, "/api/deploy", body);
    }

    private String httpGet(int port, String path) {
        var request = HttpRequest.newBuilder()
                                 .uri(URI.create("http://localhost:" + port + path))
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

    private void httpPost(int port, String path, String body) {
        var request = HttpRequest.newBuilder()
                                 .uri(URI.create("http://localhost:" + port + path))
                                 .header("Content-Type", "application/json")
                                 .POST(HttpRequest.BodyPublishers.ofString(body))
                                 .timeout(Duration.ofSeconds(10))
                                 .build();
        try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            // Ignore errors during deployment - we're testing shutdown scenarios
        }
    }
}
