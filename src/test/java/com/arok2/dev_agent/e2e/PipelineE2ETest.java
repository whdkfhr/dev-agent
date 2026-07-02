package com.arok2.dev_agent.e2e;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GitHub Issue → Task → Design → Implementation → Review → PullRequest
 * 전체 파이프라인 API 체인 E2E 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PipelineE2ETest {

    static final String TASK_ID   = "TASK-E2E-001";
    static final String DESIGN_ID = "DESIGN-E2E-001";
    static final String IMPL_ID   = "IMPL-E2E-001";
    static final String REVIEW_ID = "REVIEW-E2E-001";
    static final String PR_ID     = "PR-E2E-001";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void step1_Task_생성() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taskId": "%s",
                                  "title": "E2E 파이프라인 테스트 Task",
                                  "content": "GitHub Issue를 분석해 생성된 Task",
                                  "status": "TODO"
                                }
                                """.formatted(TASK_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(TASK_ID))
                .andExpect(jsonPath("$.title").value("E2E 파이프라인 테스트 Task"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @Order(2)
    void step2_Design_생성() throws Exception {
        mockMvc.perform(post("/designs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "designId": "%s",
                                  "taskId": "%s",
                                  "content": "Task를 기반으로 생성된 아키텍처 설계"
                                }
                                """.formatted(DESIGN_ID, TASK_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.designId").value(DESIGN_ID))
                .andExpect(jsonPath("$.taskId").value(TASK_ID))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @Order(3)
    void step3_Implementation_생성() throws Exception {
        mockMvc.perform(post("/implementations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "implId": "%s",
                                  "taskId": "%s",
                                  "designId": "%s",
                                  "status": "GENERATED",
                                  "generatedFiles": [
                                    "src/main/java/com/arok2/dev_agent/example/controller/ExampleController.java",
                                    "src/main/java/com/arok2/dev_agent/example/service/ExampleService.java"
                                  ]
                                }
                                """.formatted(IMPL_ID, TASK_ID, DESIGN_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.implId").value(IMPL_ID))
                .andExpect(jsonPath("$.taskId").value(TASK_ID))
                .andExpect(jsonPath("$.designId").value(DESIGN_ID))
                .andExpect(jsonPath("$.status").value("GENERATED"))
                .andExpect(jsonPath("$.generatedFiles").isArray())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @Order(4)
    void step4_Review_생성() throws Exception {
        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewId": "%s",
                                  "implId": "%s",
                                  "taskId": "%s",
                                  "status": "APPROVED",
                                  "comments": [
                                    "레이어 분리가 명확합니다.",
                                    "예외 처리가 일관되게 적용되어 있습니다."
                                  ]
                                }
                                """.formatted(REVIEW_ID, IMPL_ID, TASK_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewId").value(REVIEW_ID))
                .andExpect(jsonPath("$.implId").value(IMPL_ID))
                .andExpect(jsonPath("$.taskId").value(TASK_ID))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @Order(5)
    void step5_PullRequest_생성() throws Exception {
        mockMvc.perform(post("/pull-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prId": "%s",
                                  "taskId": "%s",
                                  "implId": "%s",
                                  "reviewId": "%s",
                                  "title": "feat: E2E 파이프라인 테스트 구현",
                                  "body": "## Summary\\n- E2E 파이프라인 검증 완료",
                                  "prUrl": "https://github.com/whdkfhr/dev-agent/pull/100",
                                  "status": "OPEN"
                                }
                                """.formatted(PR_ID, TASK_ID, IMPL_ID, REVIEW_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prId").value(PR_ID))
                .andExpect(jsonPath("$.taskId").value(TASK_ID))
                .andExpect(jsonPath("$.implId").value(IMPL_ID))
                .andExpect(jsonPath("$.reviewId").value(REVIEW_ID))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @Order(6)
    void step6_taskId로_전체_파이프라인_조회() throws Exception {
        mockMvc.perform(get("/designs/by-task/" + TASK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.designId").value(DESIGN_ID))
                .andExpect(jsonPath("$.taskId").value(TASK_ID));

        mockMvc.perform(get("/implementations/by-task/" + TASK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.implId").value(IMPL_ID))
                .andExpect(jsonPath("$.taskId").value(TASK_ID));

        mockMvc.perform(get("/reviews/by-impl/" + IMPL_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(REVIEW_ID))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/pull-requests/by-task/" + TASK_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prId").value(PR_ID))
                .andExpect(jsonPath("$.prUrl").value("https://github.com/whdkfhr/dev-agent/pull/100"));
    }
}
