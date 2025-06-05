package org.pragmatica.aether.cluster;

import org.pragmatica.aether.slice.manager.SliceStore;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.kvstore.KVStoreNotification.ValuePut;
import org.pragmatica.cluster.state.kvstore.KVStoreNotification.ValueRemove;
import org.pragmatica.cluster.topology.QuorumStateNotification;
import org.pragmatica.message.MessageRouter;

import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public interface NodeDeploymentManager {
    sealed interface NodeDeploymentState {
        default <K extends CharSequence,V> void onValuePut(ValuePut<K,V> valuePut) {}
        default <K extends CharSequence,V> void onValueRemove(ValueRemove<K, V> valueRemove) {}

        record DormantNodeDeploymentState() implements NodeDeploymentState { }
        record ActiveNodeDeploymentState(Pattern pattern, NodeId self) implements NodeDeploymentState {

            @Override
            public <K extends CharSequence, V>  void onValuePut(ValuePut<K,V> valuePut) {

            }

            @Override
            public <K extends CharSequence,V> void onValueRemove(ValueRemove<K, V> valueRemove) {

            }
        }
    }

    static NodeDeploymentManager nodeSliceManager(NodeId self, MessageRouter router, SliceStore sliceStore) {
        record deploymentManager(NodeId self, SliceStore sliceManager, AtomicReference<NodeDeploymentState> state) implements NodeDeploymentManager {

             public <K extends CharSequence,V> void onValuePut(ValuePut<K,V> valuePut) {
                state.get().onValuePut(valuePut);
            }

            public <K extends CharSequence,V> void onValueRemove(ValueRemove<K, V> valueRemove) {
                state.get().onValueRemove(valueRemove);
            }

            public void onQuorumStateChange(QuorumStateNotification quorumStateNotification) {
                switch (quorumStateNotification) {
                    case ESTABLISHED -> state().set(new NodeDeploymentState.ActiveNodeDeploymentState(buildPattern(), self()));
                    case DISAPPEARED -> state().set(new NodeDeploymentState.DormantNodeDeploymentState());
                }
            }
        }

        var deploymentManager = new deploymentManager(self, sliceStore, new AtomicReference<>(new NodeDeploymentState.DormantNodeDeploymentState()));

        router.addRoute(ValuePut.class, deploymentManager::onValuePut);
        router.addRoute(ValueRemove.class, deploymentManager::onValueRemove);
        router.addRoute(QuorumStateNotification.class, deploymentManager::onQuorumStateChange);

        return deploymentManager;
    }
}
