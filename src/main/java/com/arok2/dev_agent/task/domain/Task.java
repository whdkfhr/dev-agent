package com.arok2.dev_agent.task.domain;

import java.time.Instant;

public record Task(
        String taskId,
        String title,
        String content,
        String status,
        Instant createdAt
) {}
