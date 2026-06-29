package com.arok2.dev_agent.implementation.controller;

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
class ImplementationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createImplementation_201_반환() throws Exception {
        mockMvc.perform(post("/implementations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "implId": "IMPL-TEST-1",
                                  "taskId": "TASK-TEST-1",
                                  "designId": "DESIGN-TEST-1",
                                  "status": "GENERATED",
                                  "generatedFiles": ["src/main/java/Foo.java"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.implId").value("IMPL-TEST-1"))
                .andExpect(jsonPath("$.taskId").value("TASK-TEST-1"))
                .andExpect(jsonPath("$.designId").value("DESIGN-TEST-1"))
                .andExpect(jsonPath("$.status").value("GENERATED"))
                .andExpect(jsonPath("$.generatedFiles[0]").value("src/main/java/Foo.java"));
    }

    @Test
    void createImplementation_중복_409_반환() throws Exception {
        String body = """
                {
                  "implId": "IMPL-DUP",
                  "taskId": "TASK-DUP",
                  "designId": "DESIGN-DUP",
                  "status": "GENERATED",
                  "generatedFiles": []
                }
                """;

        mockMvc.perform(post("/implementations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(post("/implementations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_IMPLEMENTATION"));
    }

    @Test
    void getAllImplementations_200_반환() throws Exception {
        mockMvc.perform(get("/implementations"))
                .andExpect(status().isOk());
    }

    @Test
    void getImplementation_존재하지않는_implId_404_반환() throws Exception {
        mockMvc.perform(get("/implementations/IMPL-NOTFOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("IMPLEMENTATION_NOT_FOUND"));
    }

    @Test
    void getImplementationByTaskId_존재하지않는_taskId_404_반환() throws Exception {
        mockMvc.perform(get("/implementations/by-task/TASK-NOTFOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("IMPLEMENTATION_NOT_FOUND"));
    }
}
