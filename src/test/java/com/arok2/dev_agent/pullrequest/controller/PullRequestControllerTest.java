package com.arok2.dev_agent.pullrequest.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PullRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createPullRequest_201_반환() throws Exception {
        mockMvc.perform(post("/pull-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prId": "PR-TEST-1",
                                  "taskId": "TASK-TEST-1",
                                  "implId": "IMPL-TEST-1",
                                  "reviewId": "REVIEW-TEST-1",
                                  "title": "feat: TASK-TEST-1 구현",
                                  "body": "PR 본문",
                                  "prUrl": "https://github.com/whdkfhr/dev-agent/pull/1",
                                  "status": "OPEN"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prId").value("PR-TEST-1"))
                .andExpect(jsonPath("$.taskId").value("TASK-TEST-1"))
                .andExpect(jsonPath("$.implId").value("IMPL-TEST-1"))
                .andExpect(jsonPath("$.reviewId").value("REVIEW-TEST-1"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.prUrl").value("https://github.com/whdkfhr/dev-agent/pull/1"));
    }

    @Test
    void createPullRequest_중복_409_반환() throws Exception {
        String body = """
                {
                  "prId": "PR-DUP",
                  "taskId": "TASK-DUP",
                  "implId": "IMPL-DUP",
                  "reviewId": "REVIEW-DUP",
                  "title": "제목",
                  "body": "본문",
                  "prUrl": "https://github.com/whdkfhr/dev-agent/pull/99",
                  "status": "OPEN"
                }
                """;

        mockMvc.perform(post("/pull-requests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(post("/pull-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PULL_REQUEST"));
    }

    @Test
    void getAllPullRequests_200_반환() throws Exception {
        mockMvc.perform(get("/pull-requests"))
                .andExpect(status().isOk());
    }

    @Test
    void getPullRequest_존재하지않는_prId_404_반환() throws Exception {
        mockMvc.perform(get("/pull-requests/PR-NOTFOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PULL_REQUEST_NOT_FOUND"));
    }

    @Test
    void getPullRequestByTaskId_존재하지않는_taskId_404_반환() throws Exception {
        mockMvc.perform(get("/pull-requests/by-task/TASK-NOTFOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PULL_REQUEST_NOT_FOUND"));
    }
}
