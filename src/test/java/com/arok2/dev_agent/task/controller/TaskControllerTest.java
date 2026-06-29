package com.arok2.dev_agent.task.controller;

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
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createTask_201_반환() throws Exception {
        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taskId": "TASK-TEST-1",
                                  "title": "테스트 Task",
                                  "content": "내용",
                                  "status": "TODO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value("TASK-TEST-1"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void createTask_중복_409_반환() throws Exception {
        String body = """
                {
                  "taskId": "TASK-DUP",
                  "title": "중복 Task",
                  "content": "내용",
                  "status": "TODO"
                }
                """;

        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_TASK"));
    }

    @Test
    void getAllTasks_200_반환() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk());
    }

    @Test
    void getTask_존재하지않는_taskId_404_반환() throws Exception {
        mockMvc.perform(get("/tasks/TASK-NOTFOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }
}
