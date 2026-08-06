package com.arok2.dev_agent.agentrun.dto;

import com.arok2.dev_agent.agentrun.domain.AgentRunStatus;

public record AgentRunStatusUpdateRequest(
        AgentRunStatus nextStatus
) {}
