package com.arok2.dev_agent.agentrun.domain;

import java.util.Arrays;
import java.util.Optional;

public enum AgentRunStatus {
    PLANNING,
    DESIGNING,
    IMPLEMENTING,
    TESTING,
    REVIEWING,
    PR_READY;

    public boolean canTransitionTo(AgentRunStatus nextStatus) {
        return next().filter(next -> next == nextStatus).isPresent();
    }

    public Optional<AgentRunStatus> next() {
        int nextOrdinal = ordinal() + 1;
        return Arrays.stream(values())
                .filter(status -> status.ordinal() == nextOrdinal)
                .findFirst();
    }
}
