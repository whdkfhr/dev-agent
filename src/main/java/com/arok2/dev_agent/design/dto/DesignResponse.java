package com.arok2.dev_agent.design.dto;

import java.time.Instant;

public record DesignResponse(
        String designId,
        String taskId,
        Instant createdAt
) {}
