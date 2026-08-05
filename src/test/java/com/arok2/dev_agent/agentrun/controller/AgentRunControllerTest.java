package com.arok2.dev_agent.agentrun.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentRunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createAgentRun_201_반환() throws Exception {
        String runId = uniqueRunId();

        mockMvc.perform(post("/agent-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "runId": "%s",
                                  "issueId": "ISSUE-101",
                                  "issueTitle": "AgentRun API 추가"
                                }
                                """.formatted(runId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.runId").value(runId))
                .andExpect(jsonPath("$.status").value("PLANNING"))
                .andExpect(jsonPath("$.stepLogs").isArray());
    }

    @Test
    void createAgentRun_중복_409_반환() throws Exception {
        String runId = uniqueRunId();
        String body = """
                {
                  "runId": "%s",
                  "issueId": "ISSUE-102",
                  "issueTitle": "중복 AgentRun"
                }
                """.formatted(runId);

        mockMvc.perform(post("/agent-runs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        mockMvc.perform(post("/agent-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_AGENT_RUN"));
    }

    @Test
    void advanceStatus_다음_상태로_전이() throws Exception {
        String runId = createAgentRun();

        mockMvc.perform(post("/agent-runs/" + runId + "/advance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nextStatus": "DESIGNING"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DESIGNING"));
    }

    @Test
    void advanceStatus_상태_건너뛰기_400_반환() throws Exception {
        String runId = createAgentRun();

        mockMvc.perform(post("/agent-runs/" + runId + "/advance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nextStatus": "IMPLEMENTING"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AGENT_RUN_STATUS_TRANSITION"));
    }

    @Test
    void advanceStatus_nextStatus_누락_400_반환() throws Exception {
        String runId = createAgentRun();

        mockMvc.perform(post("/agent-runs/" + runId + "/advance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AGENT_RUN_STATUS_TRANSITION"));
    }

    @Test
    void addStepLog_201_반환() throws Exception {
        String runId = createAgentRun();

        mockMvc.perform(post("/agent-runs/" + runId + "/steps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stepName": "Create task document",
                                  "input": "GitHub Issue body",
                                  "output": "docs/tasks/TASK-007.md",
                                  "toolName": "TaskDocumentTool",
                                  "status": "SUCCESS",
                                  "retry": false
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stepLogs[0].toolName").value("TaskDocumentTool"))
                .andExpect(jsonPath("$.stepLogs[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.stepLogs[0].retry").value(false));
    }

    @Test
    void addStepLog_존재하지않는_tool_404_반환() throws Exception {
        String runId = createAgentRun();

        mockMvc.perform(post("/agent-runs/" + runId + "/steps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stepName": "Unknown step",
                                  "input": "input",
                                  "output": "output",
                                  "toolName": "UnknownTool",
                                  "status": "FAILED",
                                  "retry": true
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TOOL_NOT_FOUND"));
    }

    @Test
    void getAgentRun_존재하지않는_runId_404_반환() throws Exception {
        mockMvc.perform(get("/agent-runs/RUN-NOTFOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AGENT_RUN_NOT_FOUND"));
    }

    private String createAgentRun() throws Exception {
        String runId = uniqueRunId();
        mockMvc.perform(post("/agent-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "runId": "%s",
                                  "issueId": "ISSUE-103",
                                  "issueTitle": "AgentRun 테스트"
                                }
                                """.formatted(runId)))
                .andExpect(status().isCreated());
        return runId;
    }

    private String uniqueRunId() {
        return "RUN-" + UUID.randomUUID();
    }
}
