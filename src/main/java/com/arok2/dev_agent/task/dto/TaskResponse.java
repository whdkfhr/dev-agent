package com.arok2.dev_agent.task.dto;

import java.time.Instant;

public record TaskResponse(
        String taskId,
        String title,
        String status,
        Instant createdAt
) {}
