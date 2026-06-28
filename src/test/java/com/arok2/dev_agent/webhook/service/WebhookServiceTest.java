package com.arok2.dev_agent.webhook.service;

import com.arok2.dev_agent.webhook.domain.GitHubEvent;
import com.arok2.dev_agent.webhook.dto.WebhookResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookServiceTest {

    private WebhookService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookService(new ObjectMapper());
    }

    @Test
    void handleEvent_returnsReceivedResponse() {
        String payload = "{\"action\": \"opened\"}";
        WebhookResponse response = webhookService.handleEvent("issues", payload);

        assertThat(response.eventId()).isNotNull();
        assertThat(response.eventType()).isEqualTo("issues");
        assertThat(response.status()).isEqualTo("RECEIVED");
    }

    @Test
    void handleEvent_storesEventInMemory() {
        String payload = "{\"action\": \"closed\"}";
        WebhookResponse response = webhookService.handleEvent("issues", payload);

        Map<String, GitHubEvent> events = webhookService.getAllEvents();
        assertThat(events).containsKey(response.eventId());

        GitHubEvent stored = events.get(response.eventId());
        assertThat(stored.eventType()).isEqualTo("issues");
        assertThat(stored.action()).isEqualTo("closed");
        assertThat(stored.rawPayload()).isEqualTo(payload);
        assertThat(stored.receivedAt()).isNotNull();
    }

    @Test
    void handleEvent_extractsActionFromPayload() {
        String payload = "{\"action\": \"labeled\", \"label\": {\"name\": \"bug\"}}";
        WebhookResponse response = webhookService.handleEvent("issues", payload);

        GitHubEvent stored = webhookService.getAllEvents().get(response.eventId());
        assertThat(stored.action()).isEqualTo("labeled");
    }

    @Test
    void handleEvent_nullActionWhenPayloadHasNoAction() {
        String payload = "{\"repository\": {\"name\": \"test\"}}";
        WebhookResponse response = webhookService.handleEvent("push", payload);

        GitHubEvent stored = webhookService.getAllEvents().get(response.eventId());
        assertThat(stored.action()).isNull();
    }

    @Test
    void handleEvent_assignsUniqueIdPerEvent() {
        String payload = "{\"action\": \"opened\"}";
        WebhookResponse first = webhookService.handleEvent("issues", payload);
        WebhookResponse second = webhookService.handleEvent("issues", payload);

        assertThat(first.eventId()).isNotEqualTo(second.eventId());
    }
}
