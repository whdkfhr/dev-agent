package com.arok2.dev_agent.review.dto;

import java.time.Instant;
import java.util.List;

public record ReviewResponse(
        String reviewId,
        String implId,
        String taskId,
        String status,
        List<String> comments,
        Instant createdAt
) {}
