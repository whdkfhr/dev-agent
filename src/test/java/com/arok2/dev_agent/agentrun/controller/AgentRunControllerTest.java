package com.arok2.dev_agent.agentrun.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentRunControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    // 입력 오류 (DESIGN-021 Implementation Guide 4)

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCreateBodies")
    void createAgentRun_필수값_오류_400_저장안함(String description, Map<String, Object> body) throws Exception {
        int before = agentRunCount();

        mockMvc.perform(post("/agent-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AGENT_RUN_REQUEST"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertThat(agentRunCount()).isEqualTo(before);
    }

    @Test
    void createAgentRun_issueTitle_없으면_201_반환() throws Exception {
        mockMvc.perform(post("/agent-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "runId": "%s",
                                  "issueId": "ISSUE-104"
                                }
                                """.formatted(uniqueRunId())))
                .andExpect(status().isCreated());
    }

    @Test
    void createAgentRun_이미있는_runId와_빈_issueId는_400_기존값_유지() throws Exception {
        String runId = createAgentRun();

        mockMvc.perform(post("/agent-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "runId": "%s",
                                  "issueId": "   "
                                }
                                """.formatted(runId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AGENT_RUN_REQUEST"));

        mockMvc.perform(get("/agent-runs/" + runId))
                .andExpect(jsonPath("$.issueId").value("ISSUE-103"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidStepBodies")
    void addStepLog_필수값_오류_400_저장안함(String description, Map<String, Object> body) throws Exception {
        String runId = createAgentRun();

        mockMvc.perform(post("/agent-runs/" + runId + "/steps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AGENT_RUN_REQUEST"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertThat(stepLogCount(runId)).isZero();
    }

    @ParameterizedTest(name = "status={0}")
    @MethodSource("unparsableStatuses")
    void addStepLog_변환할수없는_status는_저장안함(String statusValue) throws Exception {
        String runId = createAgentRun();
        Map<String, Object> body = validStepBody();
        body.put("status", statusValue);

        // 응답 코드는 이번 범위 밖이라 검증하지 않는다.
        mockMvc.perform(post("/agent-runs/" + runId + "/steps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));

        assertThat(stepLogCount(runId)).isZero();
    }

    // 오류 우선순위 (DESIGN-021 Implementation Guide 4-1)

    @ParameterizedTest(name = "{0}")
    @MethodSource("advanceBodiesForMissingRun")
    void advanceStatus_없는_run은_nextStatus와_상관없이_404(String description, String body) throws Exception {
        mockMvc.perform(post("/agent-runs/" + uniqueRunId() + "/advance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AGENT_RUN_NOT_FOUND"));
    }

    @Test
    void addStepLog_없는_run과_미등록_tool은_AGENT_RUN_NOT_FOUND() throws Exception {
        Map<String, Object> body = validStepBody();
        body.put("toolName", "UnknownTool");

        mockMvc.perform(post("/agent-runs/" + uniqueRunId() + "/steps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AGENT_RUN_NOT_FOUND"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("missingRequiredStepFields")
    void addStepLog_없는_run이라도_필수값_누락은_400(String field) throws Exception {
        Map<String, Object> body = validStepBody();
        body.remove(field);

        mockMvc.perform(post("/agent-runs/" + uniqueRunId() + "/steps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AGENT_RUN_REQUEST"));
    }

    @Test
    void addStepLog_빈_stepName과_미등록_tool은_400() throws Exception {
        String runId = createAgentRun();
        Map<String, Object> body = validStepBody();
        body.put("stepName", "   ");
        body.put("toolName", "UnknownTool");

        mockMvc.perform(post("/agent-runs/" + runId + "/steps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_AGENT_RUN_REQUEST"));
    }

    static Stream<Arguments> invalidCreateBodies() {
        List<Arguments> cases = new ArrayList<>();
        for (String field : List.of("runId", "issueId")) {
            cases.add(Arguments.of(field + " 없음", createBodyWith(field, null, true)));
            cases.add(Arguments.of(field + " null", createBodyWith(field, null, false)));
            cases.add(Arguments.of(field + " 빈 문자열", createBodyWith(field, "", false)));
            cases.add(Arguments.of(field + " 공백", createBodyWith(field, "   ", false)));
        }
        return cases.stream();
    }

    static Stream<Arguments> invalidStepBodies() {
        List<Arguments> cases = new ArrayList<>();
        for (String field : List.of("stepName", "toolName")) {
            cases.add(Arguments.of(field + " 없음", stepBodyWith(field, null, true)));
            cases.add(Arguments.of(field + " null", stepBodyWith(field, null, false)));
            cases.add(Arguments.of(field + " 빈 문자열", stepBodyWith(field, "", false)));
            cases.add(Arguments.of(field + " 공백", stepBodyWith(field, "   ", false)));
        }
        cases.add(Arguments.of("status 없음", stepBodyWith("status", null, true)));
        cases.add(Arguments.of("status null", stepBodyWith("status", null, false)));
        return cases.stream();
    }

    static Stream<String> unparsableStatuses() {
        return Stream.of("", "DONE");
    }

    static Stream<Arguments> advanceBodiesForMissingRun() {
        return Stream.of(
                Arguments.of("nextStatus 없음", "{}"),
                Arguments.of("nextStatus null", "{\"nextStatus\": null}"),
                Arguments.of("건너뛰는 전이", "{\"nextStatus\": \"TESTING\"}")
        );
    }

    static Stream<String> missingRequiredStepFields() {
        return Stream.of("toolName", "status");
    }

    private static Map<String, Object> createBodyWith(String field, String value, boolean remove) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("runId", "RUN-" + UUID.randomUUID());
        body.put("issueId", "ISSUE-105");
        body.put("issueTitle", "입력 오류 테스트");
        if (remove) {
            body.remove(field);
        } else {
            body.put(field, value);
        }
        return body;
    }

    private static Map<String, Object> stepBodyWith(String field, String value, boolean remove) {
        Map<String, Object> body = validStepBody();
        if (remove) {
            body.remove(field);
        } else {
            body.put(field, value);
        }
        return body;
    }

    private static Map<String, Object> validStepBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("stepName", "Create task document");
        body.put("input", "input");
        body.put("output", "output");
        body.put("toolName", "TaskDocumentTool");
        body.put("status", "SUCCESS");
        body.put("retry", false);
        return body;
    }

    private int agentRunCount() throws Exception {
        String content = mockMvc.perform(get("/agent-runs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content).size();
    }

    private int stepLogCount(String runId) throws Exception {
        String content = mockMvc.perform(get("/agent-runs/" + runId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode stepLogs = objectMapper.readTree(content).get("stepLogs");
        return stepLogs.size();
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
