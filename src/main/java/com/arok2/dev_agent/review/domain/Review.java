package com.arok2.dev_agent.review.domain;

import java.time.Instant;
import java.util.List;

public record Review(
        String reviewId,
        String implId,
        String taskId,
        String status,
        List<String> comments,
        Instant createdAt
) {}
