package com.arok2.dev_agent.pullrequest.dto;

public record PullRequestCreateRequest(
        String prId,
        String taskId,
        String implId,
        String reviewId,
        String title,
        String body,
        String prUrl,
        String status
) {}
