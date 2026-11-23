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
import com.arok2.dev_agent.common.exception.InvalidAgentRunRequestException;
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
        requireNotBlank(request.runId(), "runId");
        requireNotBlank(request.issueId(), "issueId");

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

        AgentRun existing = agentRunStore.putIfAbsent(agentRun.runId(), agentRun);
        if (existing != null) {
            throw new AgentRunAlreadyExistsException(request.runId());
        }

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

        // 전이 검사는 갱신 시점의 현재 값으로 한다. 예외가 나면 맵은 기존 값을 유지한다.
        AgentRun updated = agentRunStore.computeIfPresent(runId, (key, current) -> {
            if (!current.status().canTransitionTo(nextStatus)) {
                throw new InvalidAgentRunStatusTransitionException(current.status().name(), nextStatus.name());
            }
            return new AgentRun(
                    current.runId(),
                    current.issueId(),
                    current.issueTitle(),
                    nextStatus,
                    current.stepLogs(),
                    current.createdAt(),
                    Instant.now()
            );
        });
        if (updated == null) {
            throw new AgentRunNotFoundException(runId);
        }

        return toResponse(updated);
    }

    public AgentRunResponse addStepLog(String runId, AgentStepLogCreateRequest request) {
        requireNotBlank(request.stepName(), "stepName");
        requireNotBlank(request.toolName(), "toolName");
        if (request.status() == null) {
            throw new InvalidAgentRunRequestException("status must not be null");
        }
        findAgentRun(runId);
        toolRegistryService.getTool(request.toolName());

        AgentStepLog stepLog = new AgentStepLog(
                request.stepName(),
                request.input(),
                request.output(),
                request.toolName(),
                request.status(),
                request.retry(),
                Instant.now()
        );

        // 갱신 함수 안에서는 현재 값에 로그 1건을 더한 새 값만 만든다.
        AgentRun updated = agentRunStore.computeIfPresent(runId, (key, current) -> {
            List<AgentStepLog> stepLogs = new ArrayList<>(current.stepLogs());
            stepLogs.add(stepLog);
            return new AgentRun(
                    current.runId(),
                    current.issueId(),
                    current.issueTitle(),
                    current.status(),
                    List.copyOf(stepLogs),
                    current.createdAt(),
                    Instant.now()
            );
        });
        if (updated == null) {
            throw new AgentRunNotFoundException(runId);
        }

        return toResponse(updated);
    }

    private void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidAgentRunRequestException(fieldName + " must not be blank");
        }
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
