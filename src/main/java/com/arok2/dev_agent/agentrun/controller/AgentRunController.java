package com.arok2.dev_agent.agentrun.controller;

import com.arok2.dev_agent.agentrun.dto.AgentRunCreateRequest;
import com.arok2.dev_agent.agentrun.dto.AgentRunResponse;
import com.arok2.dev_agent.agentrun.dto.AgentRunStatusUpdateRequest;
import com.arok2.dev_agent.agentrun.dto.AgentStepLogCreateRequest;
import com.arok2.dev_agent.agentrun.service.AgentRunService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/agent-runs")
public class AgentRunController {

    private final AgentRunService agentRunService;

    public AgentRunController(AgentRunService agentRunService) {
        this.agentRunService = agentRunService;
    }

    @PostMapping
    public ResponseEntity<AgentRunResponse> createAgentRun(@RequestBody AgentRunCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agentRunService.createAgentRun(request));
    }

    @GetMapping
    public ResponseEntity<List<AgentRunResponse>> getAllAgentRuns() {
        return ResponseEntity.ok(agentRunService.getAllAgentRuns());
    }

    @GetMapping("/{runId}")
    public ResponseEntity<AgentRunResponse> getAgentRun(@PathVariable String runId) {
        return ResponseEntity.ok(agentRunService.getAgentRun(runId));
    }

    @PostMapping("/{runId}/advance")
    public ResponseEntity<AgentRunResponse> advanceStatus(
            @PathVariable String runId,
            @RequestBody AgentRunStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(agentRunService.advanceStatus(runId, request.nextStatus()));
    }

    @PostMapping("/{runId}/steps")
    public ResponseEntity<AgentRunResponse> addStepLog(
            @PathVariable String runId,
            @RequestBody AgentStepLogCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(agentRunService.addStepLog(runId, request));
    }
}
