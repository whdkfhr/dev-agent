package com.arok2.dev_agent.review.dto;

import java.util.List;

public record ReviewCreateRequest(
        String reviewId,
        String implId,
        String taskId,
        String status,
        List<String> comments
) {}
