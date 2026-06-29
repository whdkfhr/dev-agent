package com.arok2.dev_agent.design.dto;

public record DesignCreateRequest(
        String designId,
        String taskId,
        String content
) {}
