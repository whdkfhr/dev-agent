package com.arok2.dev_agent.agentrun.domain;

public record AgentTool(
        String name,
        String description,
        String inputSchema,
        String outputSchema
) {}
