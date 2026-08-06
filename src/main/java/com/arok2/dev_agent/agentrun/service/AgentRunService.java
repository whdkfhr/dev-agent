package com.arok2.dev_agent.agentrun.service;

import com.arok2.dev_agent.agentrun.domain.AgentRun;
import com.arok2.dev_agent.agentrun.domain.AgentRunStatus;
import com.arok2.dev_agent.agentrun.domain.AgentStepLog;
import com.arok2.dev_agent.agentrun.dto.AgentRunCreateRequest;
import com.arok2.dev_agent.agentrun.dto.AgentRunResponse;
import com.arok2.dev_agent.agentrun.dto.AgentStepLogCreateRequest;
import com.arok2.dev_agent.agentrun.dto.AgentStepLogResponse;
import com.arok2.dev_agent.common.exception.AgentRunAlreadyExistsException;
import com.arok2.dev_agent.common.exception.AgentRunNotFoundException;
import com.arok2.dev_agent.common.exception.InvalidAgentRunStatusTransitionException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgentRunService {

    private final Map<String, AgentRun> agentRunStore = new ConcurrentHashMap<>();
    private final ToolRegistryService toolRegistryService;

    public AgentRunService(ToolRegistryService toolRegistryService) {
        this.toolRegistryService = toolRegistryService;
    }

    public AgentRunResponse createAgentRun(AgentRunCreateRequest request) {
        if (agentRunStore.containsKey(request.runId())) {
            throw new AgentRunAlreadyExistsException(request.runId());
        }

        Instant now = Instant.now();
        AgentRun agentRun = new AgentRun(
                request.runId(),
                request.issueId(),
                request.issueTitle(),
                AgentRunStatus.PLANNING,
                List.of(),
                now,
                now
        );
        agentRunStore.put(agentRun.runId(), agentRun);

        return toResponse(agentRun);
    }

    public List<AgentRunResponse> getAllAgentRuns() {
        return agentRunStore.values().stream()
                .map(this::toResponse)
                .toList();
    }

    public AgentRunResponse getAgentRun(String runId) {
        return toResponse(findAgentRun(runId));
    }

    public AgentRunResponse advanceStatus(String runId, AgentRunStatus nextStatus) {
        AgentRun agentRun = findAgentRun(runId);
        if (nextStatus == null) {
            throw new InvalidAgentRunStatusTransitionException(agentRun.status().name(), "null");
        }
        if (!agentRun.status().canTransitionTo(nextStatus)) {
            throw new InvalidAgentRunStatusTransitionException(agentRun.status().name(), nextStatus.name());
        }

        AgentRun updated = new AgentRun(
                agentRun.runId(),
                agentRun.issueId(),
                agentRun.issueTitle(),
                nextStatus,
                agentRun.stepLogs(),
                agentRun.createdAt(),
                Instant.now()
        );
        agentRunStore.put(updated.runId(), updated);

        return toResponse(updated);
    }

    public AgentRunResponse addStepLog(String runId, AgentStepLogCreateRequest request) {
        AgentRun agentRun = findAgentRun(runId);
        toolRegistryService.getTool(request.toolName());

        List<AgentStepLog> stepLogs = new ArrayList<>(agentRun.stepLogs());
        stepLogs.add(new AgentStepLog(
                request.stepName(),
                request.input(),
                request.output(),
                request.toolName(),
                request.status(),
                request.retry(),
                Instant.now()
        ));

        AgentRun updated = new AgentRun(
                agentRun.runId(),
                agentRun.issueId(),
                agentRun.issueTitle(),
                agentRun.status(),
                List.copyOf(stepLogs),
                agentRun.createdAt(),
                Instant.now()
        );
        agentRunStore.put(updated.runId(), updated);

        return toResponse(updated);
    }

    private AgentRun findAgentRun(String runId) {
        AgentRun agentRun = agentRunStore.get(runId);
        if (agentRun == null) {
            throw new AgentRunNotFoundException(runId);
        }
        return agentRun;
    }

    private AgentRunResponse toResponse(AgentRun agentRun) {
        return new AgentRunResponse(
                agentRun.runId(),
                agentRun.issueId(),
                agentRun.issueTitle(),
                agentRun.status(),
                agentRun.stepLogs().stream()
                        .map(this::toStepLogResponse)
                        .toList(),
                agentRun.createdAt(),
                agentRun.updatedAt()
        );
    }

    private AgentStepLogResponse toStepLogResponse(AgentStepLog stepLog) {
        return new AgentStepLogResponse(
                stepLog.stepName(),
                stepLog.input(),
                stepLog.output(),
                stepLog.toolName(),
                stepLog.status(),
                stepLog.retry(),
                stepLog.createdAt()
        );
    }
}
