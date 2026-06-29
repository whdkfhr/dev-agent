package com.arok2.dev_agent.pullrequest.domain;

import java.time.Instant;

public record PullRequest(
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
