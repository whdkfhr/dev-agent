package com.arok2.dev_agent.pullrequest.dto;

import java.time.Instant;

public record PullRequestResponse(
        String prId,
        String taskId,
        String implId,
        String reviewId,
        String title,
        String body,
        String prUrl,
        String status,
        Instant createdAt
) {}
