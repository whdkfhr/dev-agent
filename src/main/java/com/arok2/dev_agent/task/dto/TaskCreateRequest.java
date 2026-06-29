package com.arok2.dev_agent.task.dto;

public record TaskCreateRequest(
        String taskId,
        String title,
        String content,
        String status
) {}
