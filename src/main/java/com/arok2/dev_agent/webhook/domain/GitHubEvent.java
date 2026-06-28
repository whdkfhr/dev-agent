package com.arok2.dev_agent.webhook.domain;

import java.time.Instant;

public record GitHubEvent(
        String id,
        String eventType,
        String action,
        String rawPayload,
        Instant receivedAt
) {}
