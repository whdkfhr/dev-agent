package com.arok2.dev_agent.agentrun.dto;

import com.arok2.dev_agent.agentrun.domain.AgentStepStatus;

import java.time.Instant;

public record AgentStepLogResponse(
        String stepName,
        String input,
        String output,
        String toolName,
        AgentStepStatus status,
        boolean retry,
        Instant createdAt
) {}
