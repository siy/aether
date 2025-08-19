package org.pragmatica.aether.cluster;

import org.pragmatica.aether.slice.SliceState;
import org.pragmatica.aether.slice.kvstore.SliceKVSchema.SliceNodeKey;
import org.pragmatica.aether.slice.kvstore.SliceKVSchema.SliceStateValue;
import org.pragmatica.aether.slice.manager.SliceStore;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.kvstore.KVStoreNotification.ValuePut;
import org.pragmatica.cluster.state.kvstore.KVStoreNotification.ValueRemove;
import org.pragmatica.cluster.topology.QuorumStateNotification;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.lang.Promise;
import java.util.concurrent.ScheduledExecutorService;
import org.pragmatica.message.MessageRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public interface NodeDeploymentManager {
    
    record SliceDeployment(SliceNodeKey key, SliceState state, long timestamp) {}
    
    record NodeDeploymentConfiguration(
        TimeSpan loadingTimeout,
        TimeSpan activatingTimeout,
        TimeSpan deactivatingTimeout,
        TimeSpan unloadingTimeout
    ) {
        public static NodeDeploymentConfiguration defaultConfiguration() {
            return new NodeDeploymentConfiguration(
                TimeSpan.timeSpan(2).minutes(),
                TimeSpan.timeSpan(1).minutes(),
                TimeSpan.timeSpan(30).seconds(),
                TimeSpan.timeSpan(2).minutes()
            );
        }
        
        public TimeSpan timeoutFor(SliceState state) {
            return switch (state) {
                case LOADING -> loadingTimeout;
                case ACTIVATING -> activatingTimeout;
                case DEACTIVATING -> deactivatingTimeout;
                case UNLOADING -> unloadingTimeout;
                default -> throw new IllegalArgumentException("No timeout configured for state: " + state);
            };
        }
    }
    
    sealed interface NodeDeploymentState {
        default void onValuePut(ValuePut<?,?> valuePut) {}
        default void onValueRemove(ValueRemove<?,?> valueRemove) {}

        record DormantNodeDeploymentState() implements NodeDeploymentState { }
        record ActiveNodeDeploymentState(
            Pattern pattern, 
            NodeId self, 
            SliceStore sliceStore,
            ScheduledExecutorService scheduler,
            NodeDeploymentConfiguration configuration,
            ConcurrentHashMap<SliceNodeKey, SliceDeployment> deployments
        ) implements NodeDeploymentState {
            
            private static final Logger log = LoggerFactory.getLogger(ActiveNodeDeploymentState.class);

            @Override
            public void onValuePut(ValuePut<?,?> valuePut) {
                // Simplified implementation - assume key/value are accessible as toString
                var key = valuePut.toString(); // Will need to fix based on actual API
                if (!pattern.matcher(key).matches()) {
                    return;
                }
                
                // TODO: Fix API access once actual ValuePut interface is available
                log.debug("ValuePut received for key: {}", key);
            }

            @Override
            public void onValueRemove(ValueRemove<?,?> valueRemove) {
                // Simplified implementation - assume key is accessible as toString  
                var key = valueRemove.toString(); // Will need to fix based on actual API
                log.debug("ValueRemove received for key: {}", key);
            }
            
            private void handleSliceStateUpdate(SliceNodeKey sliceKey, SliceStateValue stateValue) {
                var currentDeployment = deployments.get(sliceKey);
                
                // Timeouts are now handled by Promise.timeout() in individual handlers
                
                var state = stateValue.state();
                // Timeout handling moved to individual Promise chains
                
                // Note: Timeout handling is now done via Promise.timeout() in individual handlers
                // No need for manual timeout scheduling here
                
                var newDeployment = new SliceDeployment(sliceKey, state, stateValue.timestamp());
                deployments.put(sliceKey, newDeployment);
                
                // Process state transition
                processStateTransition(sliceKey, state);
            }
            
            
            private void processStateTransition(SliceNodeKey sliceKey, SliceState state) {
                switch (state) {
                    case LOADING -> handleLoading(sliceKey);
                    case LOADED -> handleLoaded(sliceKey);
                    case ACTIVATING -> handleActivating(sliceKey);
                    case ACTIVE -> handleActive(sliceKey);
                    case DEACTIVATING -> handleDeactivating(sliceKey);
                    case FAILED -> handleFailed(sliceKey);
                    case UNLOADING -> handleUnloading(sliceKey);
                    default -> { /* No action needed for other states */ }
                }
            }
            
            private void handleLoading(SliceNodeKey sliceKey) {
                // Delegate to SliceStore for actual slice loading
                sliceStore.loadSlice(sliceKey.artifact())
                    .timeout(configuration.timeoutFor(SliceState.LOADING))
                    .onSuccess(slice -> {
                        // Transition to LOADED state
                        updateSliceState(sliceKey, SliceState.LOADED);
                    })
                    .onFailure(cause -> {
                        // Transition to FAILED state
                        updateSliceState(sliceKey, SliceState.FAILED);
                    });
            }
            
            private void handleLoaded(SliceNodeKey sliceKey) {
                // Slice is loaded and ready for activation
                // No automatic action - waiting for ACTIVATE command
            }
            
            private void handleActivating(SliceNodeKey sliceKey) {
                // Delegate to SliceStore for slice activation
                sliceStore.activateSlice(sliceKey.artifact())
                    .timeout(configuration.timeoutFor(SliceState.ACTIVATING))
                    .onSuccess(slice -> {
                        // Transition to ACTIVE state
                        updateSliceState(sliceKey, SliceState.ACTIVE);
                    })
                    .onFailure(cause -> {
                        // Transition to FAILED state  
                        updateSliceState(sliceKey, SliceState.FAILED);
                    });
            }
            
            private void handleActive(SliceNodeKey sliceKey) {
                // Slice is now active and serving requests
                // Register endpoints in EndpointRegistry (future implementation)
                // TODO: We may want to generate a notification for slice activation. No need to implement this now, just leave todo.
            }
            
            private void handleDeactivating(SliceNodeKey sliceKey) {
                // Delegate to SliceStore for slice deactivation
                sliceStore.deactivateSlice(sliceKey.artifact())
                    .timeout(configuration.timeoutFor(SliceState.DEACTIVATING))
                    .onSuccess(slice -> {
                        // Transition back to LOADED state
                        updateSliceState(sliceKey, SliceState.LOADED);
                    })
                    .onFailure(cause -> {
                        // Transition to FAILED state
                        updateSliceState(sliceKey, SliceState.FAILED);
                    });
            }
            
            private void handleFailed(SliceNodeKey sliceKey) {
                // Slice has failed - cleanup and prepare for unloading
                // Log the failure and await UNLOAD command
            }
            
            private void handleUnloading(SliceNodeKey sliceKey) {
                // Delegate to SliceStore for slice unloading
                sliceStore.unloadSlice(sliceKey.artifact())
                    .timeout(configuration.timeoutFor(SliceState.UNLOADING))
                    .onSuccess(result -> {
                        // Remove from deployments map
                        deployments.remove(sliceKey);
                    })
                    .onFailure(cause -> {
                        // Log error but still remove from tracking
                        log.error("Failed to unload slice {}: {}", sliceKey, cause.message());
                        deployments.remove(sliceKey);
                    });
            }
            
            private void updateSliceState(SliceNodeKey sliceKey, SliceState newState) {
                var newStateValue = new SliceStateValue(newState, System.currentTimeMillis(), 
                    deployments.get(sliceKey).timestamp() + 1);
                
                // This would typically update the cluster KV store
                // For now, we'll simulate by handling the state update locally
                handleSliceStateUpdate(sliceKey, newStateValue);
            }
        }
    }

    static NodeDeploymentManager nodeDeploymentManager(NodeId self, MessageRouter router, SliceStore sliceStore, ScheduledExecutorService scheduler) {
        return nodeDeploymentManager(self, router, sliceStore, scheduler, NodeDeploymentConfiguration.defaultConfiguration());
    }
    
    static NodeDeploymentManager nodeDeploymentManager(NodeId self, MessageRouter router, SliceStore sliceStore, ScheduledExecutorService scheduler, NodeDeploymentConfiguration configuration) {
        record deploymentManager(
            NodeId self, 
            SliceStore sliceManager, 
            ScheduledExecutorService scheduler,
            NodeDeploymentConfiguration configuration,
            AtomicReference<NodeDeploymentState> state
        ) implements NodeDeploymentManager {

             public void onValuePut(ValuePut<?,?> valuePut) {
                state.get().onValuePut(valuePut);
            }

            public void onValueRemove(ValueRemove<?,?> valueRemove) {
                state.get().onValueRemove(valueRemove);
            }

            public void onQuorumStateChange(QuorumStateNotification quorumStateNotification) {
                switch (quorumStateNotification) {
                    case ESTABLISHED -> state().set(new NodeDeploymentState.ActiveNodeDeploymentState(
                        buildPattern(), 
                        self(), 
                        sliceManager(),
                        scheduler(),
                        configuration(),
                        new ConcurrentHashMap<>()
                    ));
                    case DISAPPEARED -> {
                        // Clean up any pending operations before going dormant
                        // Individual Promise timeouts will handle their own cleanup
                        state().set(new NodeDeploymentState.DormantNodeDeploymentState());
                    }
                }
            }
            
            private Pattern buildPattern() {
                // Create pattern based on node ID for slice deployment matching
                return Pattern.compile("slices/" + self.id() + "/.*");
            }
        }

        var deploymentManager = new deploymentManager(
            self, 
            sliceStore, 
            scheduler,
            configuration,
            new AtomicReference<>(new NodeDeploymentState.DormantNodeDeploymentState())
        );

        var mutableRouter = (MessageRouter.MutableRouter) router;
        mutableRouter.addRoute(ValuePut.class, deploymentManager::onValuePut);
        mutableRouter.addRoute(ValueRemove.class, deploymentManager::onValueRemove);
        mutableRouter.addRoute(QuorumStateNotification.class, deploymentManager::onQuorumStateChange);

        return deploymentManager;
    }
}
