package com.arok2.dev_agent.pullrequest.controller;

import com.arok2.dev_agent.pullrequest.dto.PullRequestCreateRequest;
import com.arok2.dev_agent.pullrequest.dto.PullRequestResponse;
import com.arok2.dev_agent.pullrequest.service.PullRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pull-requests")
public class PullRequestController {

    private final PullRequestService pullRequestService;

    public PullRequestController(PullRequestService pullRequestService) {
        this.pullRequestService = pullRequestService;
    }

    @PostMapping
    public ResponseEntity<PullRequestResponse> createPullRequest(@RequestBody PullRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pullRequestService.createPullRequest(request));
    }

    @GetMapping
    public ResponseEntity<List<PullRequestResponse>> getAllPullRequests() {
        return ResponseEntity.ok(pullRequestService.getAllPullRequests());
    }

    @GetMapping("/{prId}")
    public ResponseEntity<PullRequestResponse> getPullRequest(@PathVariable String prId) {
        return ResponseEntity.ok(pullRequestService.getPullRequest(prId));
    }

    @GetMapping("/by-task/{taskId}")
    public ResponseEntity<PullRequestResponse> getPullRequestByTaskId(@PathVariable String taskId) {
        return ResponseEntity.ok(pullRequestService.getPullRequestByTaskId(taskId));
    }
}
