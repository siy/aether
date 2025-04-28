package org.pragmatica.cluster.consensus.rabia;

import org.pragmatica.cluster.consensus.ProtocolMessage;

public sealed interface RabiaMessage extends ProtocolMessage permits CommitMessage,
        ProposalMessage,
        SelectionProposal,
        SelectionVote,
        StateSyncRequest,
        StateSyncResponse,
        VoteMessage {
}
