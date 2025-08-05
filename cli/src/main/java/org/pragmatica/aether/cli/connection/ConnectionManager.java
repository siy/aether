package org.pragmatica.aether.cli.connection;

import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.message.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages connections to Aether cluster nodes for CLI operations.
 * 
 * The ConnectionManager provides:
 * - Connection establishment and management to cluster nodes
 * - Automatic agent discovery within connected clusters
 * - Connection health monitoring and failover
 * - Integration with MessageRouter for cluster communication
 * 
 * This component enables the CLI to seamlessly interact with distributed
 * Aether clusters by maintaining reliable connections and providing
 * transparent access to cluster functionality through the existing
 * MessageRouter infrastructure.
 */
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DISCOVERY_TIMEOUT = Duration.ofSeconds(10);
    
    private final Map<String, NodeConnection> connections;
    private final AtomicReference<NodeConnection> activeConnection;
    private final AtomicBoolean isInitialized;
    private final AgentDiscovery agentDiscovery;
    
    public ConnectionManager() {
        this.connections = new ConcurrentHashMap<>();
        this.activeConnection = new AtomicReference<>();
        this.isInitialized = new AtomicBoolean(false);
        this.agentDiscovery = new AgentDiscovery();
    }
    
    /**
     * Initializes the connection manager.
     */
    public void initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            logger.info("Connection manager initialized");
        }
    }
    
    /**
     * Connects to a specific cluster node.
     * 
     * @param nodeAddress Address in format "host:port"
     * @return Future completing when connection is established
     */
    public CompletableFuture<NodeConnection> connectToNode(String nodeAddress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Connecting to node: {}", nodeAddress);
                
                // Parse address
                var address = parseNodeAddress(nodeAddress);
                var nodeId = NodeId.nodeId(nodeAddress); // Simple node ID generation
                
                // Check if already connected
                var existing = connections.get(nodeAddress);
                if (existing != null && existing.isConnected()) {
                    logger.debug("Already connected to node: {}", nodeAddress);
                    activeConnection.set(existing);
                    return existing;
                }
                
                // Create new connection
                var connection = createConnection(nodeId, address);
                
                // Establish connection
                connection.connect().join();
                
                // Store connection and set as active
                connections.put(nodeAddress, connection);
                activeConnection.set(connection);
                
                // Perform agent discovery
                discoverAgents(connection);
                
                logger.info("Successfully connected to node: {}", nodeAddress);
                return connection;
                
            } catch (Exception e) {
                logger.error("Failed to connect to node {}: {}", nodeAddress, e.getMessage(), e);
                throw new RuntimeException("Connection failed: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Disconnects from a specific node.
     */
    public CompletableFuture<Void> disconnectFromNode(String nodeAddress) {
        return CompletableFuture.runAsync(() -> {
            try {
                var connection = connections.remove(nodeAddress);
                if (connection != null) {
                    connection.disconnect();
                    
                    // Clear active connection if this was it
                    activeConnection.compareAndSet(connection, null);
                    
                    logger.info("Disconnected from node: {}", nodeAddress);
                }
            } catch (Exception e) {
                logger.error("Error disconnecting from node {}: {}", nodeAddress, e.getMessage(), e);
            }
        });
    }
    
    /**
     * Disconnects from all nodes.
     */
    public CompletableFuture<Void> disconnectAll() {
        return CompletableFuture.runAsync(() -> {
            logger.info("Disconnecting from all nodes");
            
            var disconnectFutures = connections.keySet()
                .stream()
                .map(this::disconnectFromNode)
                .toArray(CompletableFuture[]::new);
            
            CompletableFuture.allOf(disconnectFutures).join();
            
            connections.clear();
            activeConnection.set(null);
            
            logger.info("Disconnected from all nodes");
        });
    }
    
    /**
     * Gets the currently active connection.
     */
    public NodeConnection getActiveConnection() {
        return activeConnection.get();
    }
    
    /**
     * Gets all established connections.
     */
    public List<NodeConnection> getAllConnections() {
        return List.copyOf(connections.values());
    }
    
    /**
     * Checks if connected to any nodes.
     */
    public boolean isConnected() {
        return activeConnection.get() != null;
    }
    
    /**
     * Gets connection information for monitoring.
     */
    public ConnectionInfo getConnectionInfo() {
        var active = activeConnection.get();
        var totalConnections = connections.size();
        var healthyConnections = (int) connections.values().stream()
            .mapToInt(conn -> conn.isConnected() ? 1 : 0)
            .sum();
        
        return new ConnectionInfo(
            active != null ? active.getNodeId() : null,
            totalConnections,
            healthyConnections,
            isInitialized.get()
        );
    }
    
    /**
     * Shuts down the connection manager and closes all connections.
     */
    public void shutdown() {
        if (isInitialized.compareAndSet(true, false)) {
            logger.info("Shutting down connection manager");
            
            try {
                disconnectAll().join();
                agentDiscovery.shutdown();
                
                logger.info("Connection manager shutdown completed");
            } catch (Exception e) {
                logger.error("Error during connection manager shutdown: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * Creates a new node connection.
     */
    private NodeConnection createConnection(NodeId nodeId, InetSocketAddress address) {
        // For now, create a simple connection wrapper
        // In a full implementation, this would integrate with actual networking
        return new NodeConnection(nodeId, address);
    }
    
    /**
     * Parses a node address string into InetSocketAddress.
     */
    private InetSocketAddress parseNodeAddress(String address) {
        var parts = address.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid node address format. Expected 'host:port'");
        }
        
        try {
            var host = parts[0];
            var port = Integer.parseInt(parts[1]);
            return new InetSocketAddress(host, port);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number in address: " + address);
        }
    }
    
    /**
     * Performs agent discovery on a connected node.
     */
    private void discoverAgents(NodeConnection connection) {
        try {
            logger.debug("Discovering agents on node: {}", connection.getNodeId());
            
            var agents = agentDiscovery.discoverAgents(connection)
                .orTimeout(DISCOVERY_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
                .join();
            
            connection.setDiscoveredAgents(agents);
            
            logger.info("Discovered {} agents on node {}", agents.size(), connection.getNodeId());
            
        } catch (Exception e) {
            logger.warn("Agent discovery failed for node {}: {}", connection.getNodeId(), e.getMessage());
        }
    }
    
    /**
     * Information about current connections.
     */
    public record ConnectionInfo(
        NodeId activeNode,
        int totalConnections,
        int healthyConnections,
        boolean isInitialized
    ) {
        
        public boolean hasActiveConnection() {
            return activeNode != null;
        }
        
        public double getHealthRatio() {
            return totalConnections > 0 ? (double) healthyConnections / totalConnections : 0.0;
        }
    }
}