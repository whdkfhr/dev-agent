package com.arok2.dev_agent.agentrun.dto;

public record AgentRunCreateRequest(
        String runId,
        String issueId,
        String issueTitle
) {}
