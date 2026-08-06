package com.arok2.dev_agent.agentrun.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ToolRegistryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAllTools_200_반환() throws Exception {
        mockMvc.perform(get("/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].name").value("RepositoryContextTool"));
    }

    @Test
    void getTool_200_반환() throws Exception {
        mockMvc.perform(get("/tools/TestRunnerTool"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TestRunnerTool"))
                .andExpect(jsonPath("$.outputSchema").value("exitCode, stdout, stderr"));
    }

    @Test
    void getTool_존재하지않는_tool_404_반환() throws Exception {
        mockMvc.perform(get("/tools/UnknownTool"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TOOL_NOT_FOUND"));
    }
}
