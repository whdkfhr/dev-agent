package com.arok2.dev_agent.webhook.service;

import com.arok2.dev_agent.webhook.domain.GitHubEvent;
import com.arok2.dev_agent.webhook.dto.WebhookResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final Map<String, GitHubEvent> eventStore = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public WebhookService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public WebhookResponse handleEvent(String eventType, String rawPayload) {
        String action = extractAction(rawPayload);
        String id = UUID.randomUUID().toString();

        GitHubEvent event = new GitHubEvent(id, eventType, action, rawPayload, Instant.now());
        eventStore.put(id, event);

        log.info("Webhook received: eventId={}, eventType={}, action={}", id, eventType, action);

        return new WebhookResponse(id, eventType, "RECEIVED");
    }

    public Map<String, GitHubEvent> getAllEvents() {
        return Map.copyOf(eventStore);
    }

    private String extractAction(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            JsonNode actionNode = root.get("action");
            return actionNode != null ? actionNode.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
