package com.arok2.dev_agent.agentrun.domain;

import java.time.Instant;
import java.util.List;

public record AgentRun(
        String runId,
        String issueId,
        String issueTitle,
        AgentRunStatus status,
        List<AgentStepLog> stepLogs,
        Instant createdAt,
        Instant updatedAt
) {}
