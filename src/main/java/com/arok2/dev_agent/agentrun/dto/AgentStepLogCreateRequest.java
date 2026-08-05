package com.arok2.dev_agent.agentrun.dto;

import com.arok2.dev_agent.agentrun.domain.AgentStepStatus;

public record AgentStepLogCreateRequest(
        String stepName,
        String input,
        String output,
        String toolName,
        AgentStepStatus status,
        boolean retry
) {}
