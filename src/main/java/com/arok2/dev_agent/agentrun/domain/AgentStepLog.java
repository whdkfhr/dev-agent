package com.arok2.dev_agent.agentrun.domain;

import java.time.Instant;

public record AgentStepLog(
        String stepName,
        String input,
        String output,
        String toolName,
        AgentStepStatus status,
        boolean retry,
        Instant createdAt
) {}
