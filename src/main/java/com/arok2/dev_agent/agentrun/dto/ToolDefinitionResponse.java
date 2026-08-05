package com.arok2.dev_agent.agentrun.dto;

public record ToolDefinitionResponse(
        String name,
        String description,
        String inputSchema,
        String outputSchema
) {}
