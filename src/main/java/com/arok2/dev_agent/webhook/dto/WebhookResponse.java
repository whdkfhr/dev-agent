package com.arok2.dev_agent.webhook.dto;

public record WebhookResponse(
        String eventId,
        String eventType,
        String status
) {}
