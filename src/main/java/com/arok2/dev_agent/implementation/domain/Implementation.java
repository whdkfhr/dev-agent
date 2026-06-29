package com.arok2.dev_agent.implementation.domain;

import java.time.Instant;
import java.util.List;

public record Implementation(
        String implId,
        String taskId,
        String designId,
        String status,
        List<String> generatedFiles,
        Instant createdAt
) {}
