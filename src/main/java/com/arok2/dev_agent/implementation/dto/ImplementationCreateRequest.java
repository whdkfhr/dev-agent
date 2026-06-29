package com.arok2.dev_agent.implementation.dto;

import java.util.List;

public record ImplementationCreateRequest(
        String implId,
        String taskId,
        String designId,
        String status,
        List<String> generatedFiles
) {}
