package com.arok2.dev_agent.agentrun.service;

import com.arok2.dev_agent.agentrun.domain.AgentRunStatus;
import com.arok2.dev_agent.agentrun.domain.AgentStepStatus;
import com.arok2.dev_agent.agentrun.dto.AgentRunCreateRequest;
import com.arok2.dev_agent.agentrun.dto.AgentRunResponse;
import com.arok2.dev_agent.agentrun.dto.AgentStepLogCreateRequest;
import com.arok2.dev_agent.common.exception.AgentRunAlreadyExistsException;
import com.arok2.dev_agent.common.exception.AgentRunNotFoundException;
import com.arok2.dev_agent.common.exception.InvalidAgentRunRequestException;
import com.arok2.dev_agent.common.exception.InvalidAgentRunStatusTransitionException;
import com.arok2.dev_agent.common.exception.ToolNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentRunServiceTest {

    private AgentRunService agentRunService;

    @BeforeEach
    void setUp() {
        agentRunService = new AgentRunService(new ToolRegistryService());
    }

    @Test
    void createAgentRun_PLANNING_상태로_생성() {
        AgentRunCreateRequest request = new AgentRunCreateRequest("RUN-001", "ISSUE-1", "AgentRun 추가");

        AgentRunResponse response = agentRunService.createAgentRun(request);

        assertThat(response.runId()).isEqualTo("RUN-001");
        assertThat(response.issueId()).isEqualTo("ISSUE-1");
        assertThat(response.status()).isEqualTo(AgentRunStatus.PLANNING);
        assertThat(response.stepLogs()).isEmpty();
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    void createAgentRun_중복_runId_예외() {
        AgentRunCreateRequest request = new AgentRunCreateRequest("RUN-001", "ISSUE-1", "AgentRun 추가");
        agentRunService.createAgentRun(request);

        assertThatThrownBy(() -> agentRunService.createAgentRun(request))
                .isInstanceOf(AgentRunAlreadyExistsException.class)
                .hasMessageContaining("RUN-001");
    }

    @Test
    void getAllAgentRuns_전체_목록_반환() {
        agentRunService.createAgentRun(new AgentRunCreateRequest("RUN-001", "ISSUE-1", "첫 번째"));
        agentRunService.createAgentRun(new AgentRunCreateRequest("RUN-002", "ISSUE-2", "두 번째"));

        List<AgentRunResponse> agentRuns = agentRunService.getAllAgentRuns();

        assertThat(agentRuns).hasSize(2);
    }

    @Test
    void getAgentRun_존재하지않는_runId_예외() {
        assertThatThrownBy(() -> agentRunService.getAgentRun("RUN-404"))
                .isInstanceOf(AgentRunNotFoundException.class)
                .hasMessageContaining("RUN-404");
    }

    @Test
    void advanceStatus_다음_상태로_전이() {
        agentRunService.createAgentRun(new AgentRunCreateRequest("RUN-001", "ISSUE-1", "AgentRun 추가"));

        AgentRunResponse response = agentRunService.advanceStatus("RUN-001", AgentRunStatus.DESIGNING);

        assertThat(response.status()).isEqualTo(AgentRunStatus.DESIGNING);
        assertThat(response.updatedAt()).isAfterOrEqualTo(response.createdAt());
    }

    @Test
    void advanceStatus_상태_건너뛰기_예외() {
        agentRunService.createAgentRun(new AgentRunCreateRequest("RUN-001", "ISSUE-1", "AgentRun 추가"));

        assertThatThrownBy(() -> agentRunService.advanceStatus("RUN-001", AgentRunStatus.IMPLEMENTING))
                .isInstanceOf(InvalidAgentRunStatusTransitionException.class)
                .hasMessageContaining("PLANNING -> IMPLEMENTING");
    }

    @Test
    void advanceStatus_다음_상태가_null이면_예외() {
        agentRunService.createAgentRun(new AgentRunCreateRequest("RUN-001", "ISSUE-1", "AgentRun 추가"));

        assertThatThrownBy(() -> agentRunService.advanceStatus("RUN-001", null))
                .isInstanceOf(InvalidAgentRunStatusTransitionException.class)
                .hasMessageContaining("PLANNING -> null");
    }

    @Test
    void addStepLog_tool_검증_후_로그_저장() {
        agentRunService.createAgentRun(new AgentRunCreateRequest("RUN-001", "ISSUE-1", "AgentRun 추가"));

        AgentRunResponse response = agentRunService.addStepLog("RUN-001", new AgentStepLogCreateRequest(
                "Create task document",
                "Issue body",
                "docs/tasks/TASK-007.md",
                "TaskDocumentTool",
                AgentStepStatus.SUCCESS,
                false
        ));

        assertThat(response.stepLogs()).hasSize(1);
        assertThat(response.stepLogs().get(0).toolName()).isEqualTo("TaskDocumentTool");
        assertThat(response.stepLogs().get(0).status()).isEqualTo(AgentStepStatus.SUCCESS);
        assertThat(response.stepLogs().get(0).retry()).isFalse();
    }

    @Test
    void addStepLog_존재하지않는_tool_예외() {
        agentRunService.createAgentRun(new AgentRunCreateRequest("RUN-001", "ISSUE-1", "AgentRun 추가"));

        assertThatThrownBy(() -> agentRunService.addStepLog("RUN-001", new AgentStepLogCreateRequest(
                "Unknown step",
                "input",
                "output",
                "UnknownTool",
                AgentStepStatus.FAILED,
                true
        )))
                .isInstanceOf(ToolNotFoundException.class)
                .hasMessageContaining("UnknownTool");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void createAgentRun_runId가_비어있으면_예외_저장안함(String runId) {
        assertThatThrownBy(() -> agentRunService.createAgentRun(new AgentRunCreateRequest(runId, "ISSUE-1", "제목")))
                .isInstanceOf(InvalidAgentRunRequestException.class)
                .hasMessage("runId must not be blank");

        assertThat(agentRunService.getAllAgentRuns()).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void createAgentRun_issueId가_비어있으면_예외_저장안함(String issueId) {
        assertThatThrownBy(() -> agentRunService.createAgentRun(new AgentRunCreateRequest("RUN-001", issueId, "제목")))
                .isInstanceOf(InvalidAgentRunRequestException.class)
                .hasMessage("issueId must not be blank");

        assertThat(agentRunService.getAllAgentRuns()).isEmpty();
    }

    @Test
    void createAgentRun_issueTitle_없어도_생성() {
        AgentRunResponse response = agentRunService.createAgentRun(new AgentRunCreateRequest("RUN-001", "ISSUE-1", null));

        assertThat(response.issueTitle()).isNull();
        assertThat(agentRunService.getAllAgentRuns()).hasSize(1);
    }

    @Test
    void createAgentRun_이미있는_runId라도_입력오류가_먼저_기존값_유지() {
        agentRunService.createAgentRun(new AgentRunCreateRequest("RUN-001", "ISSUE-1", "원래 제목"));

        assertThatThrownBy(() -> agentRunService.createAgentRun(new AgentRunCreateRequest("RUN-001", "  ", "새 제목")))
                .isInstanceOf(InvalidAgentRunRequestException.class);

        AgentRunResponse stored = agentRunService.getAgentRun("RUN-001");
        assertThat(stored.issueId()).isEqualTo("ISSUE-1");
        assertThat(stored.issueTitle()).isEqualTo("원래 제목");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void addStepLog_stepName이_비어있으면_예외_저장안함(String stepName) {
        createRun("RUN-001");

        assertThatThrownBy(() -> agentRunService.addStepLog("RUN-001",
                stepRequest(stepName, "TaskDocumentTool", AgentStepStatus.SUCCESS)))
                .isInstanceOf(InvalidAgentRunRequestException.class)
                .hasMessage("stepName must not be blank");

        assertThat(agentRunService.getAgentRun("RUN-001").stepLogs()).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void addStepLog_toolName이_비어있으면_예외_저장안함(String toolName) {
        createRun("RUN-001");

        assertThatThrownBy(() -> agentRunService.addStepLog("RUN-001",
                stepRequest("step", toolName, AgentStepStatus.SUCCESS)))
                .isInstanceOf(InvalidAgentRunRequestException.class)
                .hasMessage("toolName must not be blank");

        assertThat(agentRunService.getAgentRun("RUN-001").stepLogs()).isEmpty();
    }

    @Test
    void addStepLog_status가_null이면_예외_저장안함() {
        createRun("RUN-001");

        assertThatThrownBy(() -> agentRunService.addStepLog("RUN-001",
                stepRequest("step", "TaskDocumentTool", null)))
                .isInstanceOf(InvalidAgentRunRequestException.class)
                .hasMessage("status must not be null");

        assertThat(agentRunService.getAgentRun("RUN-001").stepLogs()).isEmpty();
    }

    // 오류 우선순위 (DESIGN-021 Implementation Guide 4-1)

    @Test
    void advanceStatus_없는_run과_null_nextStatus는_run없음이_우선() {
        assertThatThrownBy(() -> agentRunService.advanceStatus("RUN-404", null))
                .isInstanceOf(AgentRunNotFoundException.class);
    }

    @Test
    void advanceStatus_없는_run과_건너뛰는_전이는_run없음이_우선() {
        assertThatThrownBy(() -> agentRunService.advanceStatus("RUN-404", AgentRunStatus.TESTING))
                .isInstanceOf(AgentRunNotFoundException.class);
    }

    @Test
    void addStepLog_없는_run과_미등록_tool은_run없음이_우선() {
        assertThatThrownBy(() -> agentRunService.addStepLog("RUN-404",
                stepRequest("step", "UnknownTool", AgentStepStatus.SUCCESS)))
                .isInstanceOf(AgentRunNotFoundException.class);
    }

    @Test
    void addStepLog_없는_run이라도_입력오류가_우선() {
        assertThatThrownBy(() -> agentRunService.addStepLog("RUN-404",
                stepRequest("step", null, AgentStepStatus.SUCCESS)))
                .isInstanceOf(InvalidAgentRunRequestException.class);
        assertThatThrownBy(() -> agentRunService.addStepLog("RUN-404",
                stepRequest("step", "TaskDocumentTool", null)))
                .isInstanceOf(InvalidAgentRunRequestException.class);
    }

    @Test
    void addStepLog_입력오류가_미등록_tool보다_우선() {
        createRun("RUN-001");

        assertThatThrownBy(() -> agentRunService.addStepLog("RUN-001",
                stepRequest("   ", "UnknownTool", AgentStepStatus.SUCCESS)))
                .isInstanceOf(InvalidAgentRunRequestException.class);
    }

    @Test
    void advanceStatus_실패한_전이는_step_log와_상태를_바꾸지_않음() {
        createRun("RUN-001");
        agentRunService.addStepLog("RUN-001", stepRequest("step", "TaskDocumentTool", AgentStepStatus.SUCCESS));

        assertThatThrownBy(() -> agentRunService.advanceStatus("RUN-001", AgentRunStatus.TESTING))
                .isInstanceOf(InvalidAgentRunStatusTransitionException.class);

        AgentRunResponse stored = agentRunService.getAgentRun("RUN-001");
        assertThat(stored.status()).isEqualTo(AgentRunStatus.PLANNING);
        assertThat(stored.stepLogs()).hasSize(1);
    }

    private void createRun(String runId) {
        agentRunService.createAgentRun(new AgentRunCreateRequest(runId, "ISSUE-1", "AgentRun 추가"));
    }

    private AgentStepLogCreateRequest stepRequest(String stepName, String toolName, AgentStepStatus status) {
        return new AgentStepLogCreateRequest(stepName, "input", "output", toolName, status, false);
    }
}
