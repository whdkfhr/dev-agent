package com.arok2.dev_agent.agentrun.dto;

import com.arok2.dev_agent.agentrun.domain.AgentRunStatus;

import java.time.Instant;
import java.util.List;

public record AgentRunResponse(
        String runId,
        String issueId,
        String issueTitle,
        AgentRunStatus status,
        List<AgentStepLogResponse> stepLogs,
        Instant createdAt,
        Instant updatedAt
) {}
