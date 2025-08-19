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
import org.pragmatica.message.MessageRouter;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public interface NodeDeploymentManager {
    
    record SliceDeployment(SliceNodeKey key, SliceState state, long timestamp, ScheduledFuture<?> timeoutFuture) {}
    
    sealed interface NodeDeploymentState {
        default void onValuePut(ValuePut<?,?> valuePut) {}
        default void onValueRemove(ValueRemove<?,?> valueRemove) {}

        record DormantNodeDeploymentState() implements NodeDeploymentState { }
        record ActiveNodeDeploymentState(
            Pattern pattern, 
            NodeId self, 
            SliceStore sliceStore,
            ScheduledExecutorService scheduler,
            ConcurrentHashMap<SliceNodeKey, SliceDeployment> deployments
        ) implements NodeDeploymentState {

            @Override
            public void onValuePut(ValuePut<?,?> valuePut) {
                // Simplified implementation - assume key/value are accessible as toString
                var key = valuePut.toString(); // Will need to fix based on actual API
                if (!pattern.matcher(key).matches()) {
                    return;
                }
                
                // TODO: Fix API access once actual ValuePut interface is available
                System.out.println("ValuePut received for key: " + key);
            }

            @Override
            public void onValueRemove(ValueRemove<?,?> valueRemove) {
                // Simplified implementation - assume key is accessible as toString  
                var key = valueRemove.toString(); // Will need to fix based on actual API
                System.out.println("ValueRemove received for key: " + key);
            }
            
            private void handleSliceStateUpdate(SliceNodeKey sliceKey, SliceStateValue stateValue) {
                var currentDeployment = deployments.get(sliceKey);
                
                // Cancel existing timeout if any
                if (currentDeployment != null && currentDeployment.timeoutFuture() != null) {
                    currentDeployment.timeoutFuture().cancel(false);
                }
                
                var state = stateValue.state();
                ScheduledFuture<?> timeoutFuture = null;
                
                // Set up timeout for transitional states (simplified implementation)
                if (state.isTransitional()) {
                    // TODO: Fix TimeSpan API usage - for now use default timeout
                    long timeoutMillis = 60000; // 1 minute default
                    timeoutFuture = scheduler.schedule(
                        () -> handleStateTimeout(sliceKey, state),
                        timeoutMillis,
                        TimeUnit.MILLISECONDS
                    );
                }
                
                var newDeployment = new SliceDeployment(sliceKey, state, stateValue.timestamp(), timeoutFuture);
                deployments.put(sliceKey, newDeployment);
                
                // Process state transition
                processStateTransition(sliceKey, state);
            }
            
            private void handleStateTimeout(SliceNodeKey sliceKey, SliceState state) {
                System.err.println("Slice state timeout for " + sliceKey + " in state " + state);
                
                // Transition to FAILED state on timeout
                var failedState = new SliceStateValue(SliceState.FAILED, System.currentTimeMillis(), 
                    deployments.get(sliceKey).timestamp() + 1);
                
                // Update KV store with FAILED state
                // This would typically use the cluster KV store API
                // For now, we'll just remove the deployment
                deployments.remove(sliceKey);
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
            }
            
            private void handleDeactivating(SliceNodeKey sliceKey) {
                // Delegate to SliceStore for slice deactivation
                sliceStore.deactivateSlice(sliceKey.artifact())
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
                    .onSuccess(result -> {
                        // Remove from deployments map
                        deployments.remove(sliceKey);
                    })
                    .onFailure(cause -> {
                        // Log error but still remove from tracking
                        System.err.println("Failed to unload slice " + sliceKey + ": " + cause.message());
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
        record deploymentManager(
            NodeId self, 
            SliceStore sliceManager, 
            ScheduledExecutorService scheduler,
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
                        new ConcurrentHashMap<>()
                    ));
                    case DISAPPEARED -> {
                        // Cancel all ongoing timeouts before going dormant
                        var currentState = state().get();
                        if (currentState instanceof NodeDeploymentState.ActiveNodeDeploymentState active) {
                            active.deployments().values().forEach(deployment -> {
                                if (deployment.timeoutFuture() != null) {
                                    deployment.timeoutFuture().cancel(false);
                                }
                            });
                        }
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
            new AtomicReference<>(new NodeDeploymentState.DormantNodeDeploymentState())
        );

        var mutableRouter = (MessageRouter.MutableRouter) router;
        mutableRouter.addRoute(ValuePut.class, deploymentManager::onValuePut);
        mutableRouter.addRoute(ValueRemove.class, deploymentManager::onValueRemove);
        mutableRouter.addRoute(QuorumStateNotification.class, deploymentManager::onQuorumStateChange);

        return deploymentManager;
    }
}
