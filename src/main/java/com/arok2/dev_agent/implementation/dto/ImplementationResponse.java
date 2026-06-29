package com.arok2.dev_agent.implementation.dto;

import java.time.Instant;
import java.util.List;

public record ImplementationResponse(
        String implId,
        String taskId,
        String designId,
        String status,
        List<String> generatedFiles,
        Instant createdAt
) {}
