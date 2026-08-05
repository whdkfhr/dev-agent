package com.arok2.dev_agent.agentrun.service;

import com.arok2.dev_agent.agentrun.domain.AgentRunStatus;
import com.arok2.dev_agent.agentrun.domain.AgentStepStatus;
import com.arok2.dev_agent.agentrun.dto.AgentRunCreateRequest;
import com.arok2.dev_agent.agentrun.dto.AgentRunResponse;
import com.arok2.dev_agent.agentrun.dto.AgentStepLogCreateRequest;
import com.arok2.dev_agent.common.exception.AgentRunAlreadyExistsException;
import com.arok2.dev_agent.common.exception.AgentRunNotFoundException;
import com.arok2.dev_agent.common.exception.InvalidAgentRunStatusTransitionException;
import com.arok2.dev_agent.common.exception.ToolNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
