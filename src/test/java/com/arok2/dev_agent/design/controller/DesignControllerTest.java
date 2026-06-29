package com.arok2.dev_agent.design.controller;

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
class DesignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createDesign_201_반환() throws Exception {
        mockMvc.perform(post("/designs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "designId": "DESIGN-TEST-1",
                                  "taskId": "TASK-TEST-1",
                                  "content": "설계 내용"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.designId").value("DESIGN-TEST-1"))
                .andExpect(jsonPath("$.taskId").value("TASK-TEST-1"));
    }

    @Test
    void createDesign_중복_409_반환() throws Exception {
        String body = """
                {
                  "designId": "DESIGN-DUP",
                  "taskId": "TASK-DUP",
                  "content": "내용"
                }
                """;

        mockMvc.perform(post("/designs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(post("/designs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_DESIGN"));
    }

    @Test
    void getAllDesigns_200_반환() throws Exception {
        mockMvc.perform(get("/designs"))
                .andExpect(status().isOk());
    }

    @Test
    void getDesign_존재하지않는_designId_404_반환() throws Exception {
        mockMvc.perform(get("/designs/DESIGN-NOTFOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DESIGN_NOT_FOUND"));
    }

    @Test
    void getDesignByTaskId_존재하지않는_taskId_404_반환() throws Exception {
        mockMvc.perform(get("/designs/by-task/TASK-NOTFOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DESIGN_NOT_FOUND"));
    }
}
