package com.arok2.dev_agent.design.domain;

import java.time.Instant;

public record Design(
        String designId,
        String taskId,
        String content,
        Instant createdAt
) {}
