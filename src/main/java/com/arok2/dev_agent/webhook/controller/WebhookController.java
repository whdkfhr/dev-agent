package com.arok2.dev_agent.webhook.controller;

import com.arok2.dev_agent.webhook.dto.WebhookResponse;
import com.arok2.dev_agent.webhook.service.WebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/github")
    public ResponseEntity<WebhookResponse> receiveGitHubEvent(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestBody String rawPayload) {
        WebhookResponse response = webhookService.handleEvent(eventType, rawPayload);
        return ResponseEntity.ok(response);
    }
}
