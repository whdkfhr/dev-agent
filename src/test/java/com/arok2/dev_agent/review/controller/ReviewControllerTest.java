package com.arok2.dev_agent.review.controller;

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
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createReview_201_반환() throws Exception {
        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reviewId": "REVIEW-TEST-1",
                                  "implId": "IMPL-TEST-1",
                                  "taskId": "TASK-TEST-1",
                                  "status": "APPROVED",
                                  "comments": ["코드 구조가 명확합니다."]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewId").value("REVIEW-TEST-1"))
                .andExpect(jsonPath("$.implId").value("IMPL-TEST-1"))
                .andExpect(jsonPath("$.taskId").value("TASK-TEST-1"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.comments[0]").value("코드 구조가 명확합니다."));
    }

    @Test
    void createReview_중복_409_반환() throws Exception {
        String body = """
                {
                  "reviewId": "REVIEW-DUP",
                  "implId": "IMPL-DUP",
                  "taskId": "TASK-DUP",
                  "status": "APPROVED",
                  "comments": []
                }
                """;

        mockMvc.perform(post("/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_REVIEW"));
    }

    @Test
    void getAllReviews_200_반환() throws Exception {
        mockMvc.perform(get("/reviews"))
                .andExpect(status().isOk());
    }

    @Test
    void getReview_존재하지않는_reviewId_404_반환() throws Exception {
        mockMvc.perform(get("/reviews/REVIEW-NOTFOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REVIEW_NOT_FOUND"));
    }

    @Test
    void getReviewByImplId_존재하지않는_implId_404_반환() throws Exception {
        mockMvc.perform(get("/reviews/by-impl/IMPL-NOTFOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REVIEW_NOT_FOUND"));
    }
}
