package com.arok2.dev_agent.webhook.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postWebhook_returns200WithEventId() throws Exception {
        String payload = "{\"action\": \"opened\"}";

        mockMvc.perform(post("/webhook/github")
                        .header("X-GitHub-Event", "issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").isNotEmpty())
                .andExpect(jsonPath("$.eventType").value("issues"))
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void postWebhook_returns400WhenMissingHeader() throws Exception {
        mockMvc.perform(post("/webhook/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\": \"opened\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_HEADER"));
    }

    @Test
    void postWebhook_acceptsVariousEventTypes() throws Exception {
        mockMvc.perform(post("/webhook/github")
                        .header("X-GitHub-Event", "push")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ref\": \"refs/heads/main\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventType").value("push"));
    }
}
