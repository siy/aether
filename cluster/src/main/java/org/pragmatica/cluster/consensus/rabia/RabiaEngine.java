package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.consensus.Consensus;
import org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage.*;
import org.pragmatica.cluster.net.AddressBook;
import org.pragmatica.cluster.net.ClusterNetwork;
import org.pragmatica.cluster.net.NodeId;
import org.pragmatica.cluster.state.StateMachine;
import org.pragmatica.cluster.state.Command;
import org.pragmatica.lang.io.TimeSpan;
import org.pragmatica.lang.utils.SharedScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/// Implementation of the Rabia state machine replication consensus protocol for distributed systems.
///
/// @param <T> Type of protocol messages used for communication
/// @param <C> Type of commands to be processed by the state machine
public class RabiaEngine<T extends RabiaProtocolMessage, C extends Command> implements Consensus<T, C> {
    private static final Logger log = LoggerFactory.getLogger(RabiaEngine.class);
    private final NodeId self;
    private final AddressBook addressBook;
    private final ClusterNetwork<T> network;
    private final StateMachine<C> stateMachine;

    //TODO: make queue size configurable
    private final BlockingQueue<C> pendingCommands = new ArrayBlockingQueue<>(1024);
    private final Map<Integer, RoundData<C>> instances = new ConcurrentHashMap<>();
    private final AtomicInteger nextSlot = new AtomicInteger(1);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private volatile NodeMode mode = NodeMode.FOLLOWER;

    public RabiaEngine(NodeId self, AddressBook addressBook, ClusterNetwork<T> network, StateMachine<C> stateMachine) {
        this.self = self;
        this.addressBook = addressBook;
        this.network = network;
        this.stateMachine = stateMachine;

        //TODO: make interval configurable
        SharedScheduler.scheduleAtFixedRate(this::cleanupOldInstances, TimeSpan.timeSpan(10).seconds());
    }

    @Override
    public void submitCommands(List<C> commands) {
        pendingCommands.addAll(commands);

        if (mode == NodeMode.NORMAL) {
            executor.execute(this::startRound);
        }
    }

    private void startRound() {
        int slot = nextSlot.get();

        if (instances.containsKey(slot)) {
            return;
        }

        var batch = new ArrayList<C>();
        pendingCommands.drainTo(batch, 10);

        if (batch.isEmpty()) {
            return;
        }

        var instance = RoundData.create(slot, batch);
        instances.put(slot, instance);
        network.broadcast(new Propose<>(self, slot, batch));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void processMessage(RabiaProtocolMessage message) {
        executor.execute(() -> {
            switch (message) {
                case Propose<?> propose -> onPropose((Propose<C>) propose);
                case Vote vote -> onVote(vote);
                case Decide<?> decide -> onDecide((Decide<C>) decide);
                case SnapshotRequest snapshotRequest -> onSnapshotRequest(snapshotRequest);
                case SnapshotResponse snapshotResponse -> onSnapshotResponse(snapshotResponse);
            }
        });
    }

    private void onPropose(Propose<C> msg) {
        var instance = instances.computeIfAbsent(msg.slot(), s -> RoundData.create(s, msg.commands()));

        instance.proposals.put(msg.sender(), msg.commands());

        if (instance.proposals.size() >= addressBook.consensusSize() && !instance.voted.get()) {
            boolean match = instance.proposals.values()
                                              .stream()
                                              .filter(commands -> commands.equals(instance.commands))
                                              .count() >= addressBook.consensusSize();

            network.broadcast(new Vote(self, msg.slot(), match));
            instance.voted.set(true);
        }
    }

    private void onVote(Vote msg) {
        var instance = instances.get(msg.slot());

        if (instance == null || instance.decided.get()) {
            return;
        }

        instance.votes.merge(msg.match(), 1, Integer::sum);

        if (instance.votes.getOrDefault(true, 0) >= addressBook.consensusSize()) {
            decide(instance, instance.commands);
        } else if (instance.votes.getOrDefault(false, 0) >= addressBook.consensusSize()) {
            decide(instance, List.of());
        }
    }

    private void onDecide(Decide<C> msg) {
        var instance = instances.computeIfAbsent(msg.slot(), s -> RoundData.create(s, msg.commands()));
        decide(instance, msg.commands());
    }

    private void decide(RoundData<C> instance, List<C> commands) {
        if (instance.decided.compareAndSet(false, true)) {
            if (!commands.isEmpty()) {
                commands.forEach(stateMachine::process);
            }
            network.broadcast(new Decide<>(self, instance.slot, commands));
            nextSlot.incrementAndGet();
            startRound();
        }
    }

    private void onSnapshotRequest(SnapshotRequest snapshotRequest) {
        stateMachine.makeSnapshot()
                    .onSuccess(snapshot -> network.send(snapshotRequest.sender(),
                                                        new SnapshotResponse(self, snapshot)))
                    .onFailure(cause -> log.warn("Unable to make snapshot of state machine: {}", cause));
    }

    private void onSnapshotResponse(SnapshotResponse snapshotResponse) {
        stateMachine.restoreSnapshot(snapshotResponse.snapshot())
                    .onSuccessRun(() -> {
                        log.info("Successfully restored snapshot from {}", snapshotResponse.sender());
                        mode = NodeMode.NORMAL;
                        startRound();
                    })
                    .onFailure(cause -> log.error("Failed to restore snapshot from {}, {}",
                                                  snapshotResponse.sender(),
                                                  cause));
    }

    //TODO: make configurable
    private void cleanupOldInstances() {
        int currentSlot = nextSlot.get();
        instances.keySet()
                 .removeIf(slot -> slot < currentSlot - 100);
    }

    enum NodeMode {FOLLOWER, NORMAL}

    record RoundData<C extends Command>(int slot,
                                        List<C> commands,
                                        Map<NodeId, List<C>> proposals,
                                        Map<Boolean, Integer> votes,
                                        AtomicBoolean voted,
                                        AtomicBoolean decided) {
        static <C extends Command> RoundData<C> create(int slot, List<C> commands) {
            return new RoundData<>(slot, commands,
                                   new ConcurrentHashMap<>(),
                                   new ConcurrentHashMap<>(),
                                   new AtomicBoolean(false),
                                   new AtomicBoolean(false));
        }
    }
}
