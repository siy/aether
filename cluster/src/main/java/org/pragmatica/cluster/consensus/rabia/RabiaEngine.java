package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.consensus.Command;
import org.pragmatica.cluster.consensus.Consensus;
import org.pragmatica.cluster.consensus.StateMachine;
import org.pragmatica.cluster.consensus.rabia.RabiaProtocolMessage.*;
import org.pragmatica.cluster.net.AddressBook;
import org.pragmatica.cluster.net.ClusterNetwork;
import org.pragmatica.cluster.net.NodeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RabiaEngine<T extends RabiaProtocolMessage> implements Consensus<T> {
    private static final Logger log = LoggerFactory.getLogger(RabiaEngine.class);
    private final NodeId self;
    private final AddressBook addressBook;
    private final ClusterNetwork<T> network;
    private final StateMachine stateMachine;

    //TODO: make queue size configurable
    private final BlockingQueue<Command> pendingCommands = new ArrayBlockingQueue<>(1024);
    private final Map<Integer, RoundData> instances = new ConcurrentHashMap<>();
    private final AtomicInteger nextSlot = new AtomicInteger(1);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    private volatile NodeMode mode = NodeMode.FOLLOWER;

    public RabiaEngine(NodeId self, AddressBook addressBook, ClusterNetwork<T> network, StateMachine stateMachine) {
        this.self = self;
        this.addressBook = addressBook;
        this.network = network;
        this.stateMachine = stateMachine;
        cleanupExecutor.scheduleAtFixedRate(this::cleanupOldInstances, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    public void submitCommands(List<Command> commands) {
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

        var batch = new ArrayList<Command>();
        pendingCommands.drainTo(batch, 10);

        if (batch.isEmpty()) {
            return;
        }

        var instance = new RoundData(slot, batch);
        instances.put(slot, instance);
        network.broadcast(new Propose(self, slot, batch));
    }

    @Override
    public void processMessage(RabiaProtocolMessage message) {
        executor.execute(() -> {
            switch (message) {
                case Propose propose -> onPropose(propose);
                case Vote vote -> onVote(vote);
                case RabiaProtocolMessage.Decide decide -> onDecide(decide);
                case RabiaProtocolMessage.SnapshotRequest snapshotRequest -> onSnapshotRequest(snapshotRequest);
                case SnapshotResponse snapshotResponse -> onSnapshotResponse(snapshotResponse);
            }
        });
    }

    private void onPropose(Propose msg) {
        var instance = instances.computeIfAbsent(msg.slot(), s -> new RoundData(s, msg.commands()));

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

    private void onDecide(RabiaProtocolMessage.Decide msg) {
        var instance = instances.computeIfAbsent(msg.slot(), s -> new RoundData(s, msg.commands()));
        decide(instance, msg.commands());
    }

    private void decide(RoundData instance, List<Command> commands) {
        if (instance.decided.compareAndSet(false, true)) {
            if (!commands.isEmpty()) {
                stateMachine.process(commands);
            }
            network.broadcast(new RabiaProtocolMessage.Decide(self, instance.slot, commands));
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

    private void cleanupOldInstances() {
        int currentSlot = nextSlot.get();
        instances.keySet()
                 .removeIf(slot -> slot < currentSlot - 100); //TODO: make configurable
    }

    enum NodeMode {FOLLOWER, NORMAL}

    static class RoundData {
        final int slot;
        final List<Command> commands;
        final Map<NodeId, List<Command>> proposals = new ConcurrentHashMap<>();
        final Map<Boolean, Integer> votes = new ConcurrentHashMap<>();
        final AtomicBoolean voted = new AtomicBoolean(false);
        final AtomicBoolean decided = new AtomicBoolean(false);

        RoundData(int slot, List<Command> commands) {
            this.slot = slot;
            this.commands = commands;
        }
    }
}
