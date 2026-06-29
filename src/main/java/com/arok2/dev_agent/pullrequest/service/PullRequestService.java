package com.arok2.dev_agent.pullrequest.service;

import com.arok2.dev_agent.common.exception.PullRequestAlreadyExistsException;
import com.arok2.dev_agent.common.exception.PullRequestNotFoundException;
import com.arok2.dev_agent.pullrequest.domain.PullRequest;
import com.arok2.dev_agent.pullrequest.dto.PullRequestCreateRequest;
import com.arok2.dev_agent.pullrequest.dto.PullRequestResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PullRequestService {

    private final Map<String, PullRequest> prById = new ConcurrentHashMap<>();
    private final Map<String, PullRequest> prByTaskId = new ConcurrentHashMap<>();

    public PullRequestResponse createPullRequest(PullRequestCreateRequest request) {
        if (prById.containsKey(request.prId())) {
            throw new PullRequestAlreadyExistsException(request.prId());
        }
        PullRequest pr = new PullRequest(
                request.prId(),
                request.taskId(),
                request.implId(),
                request.reviewId(),
                request.title(),
                request.body(),
                request.prUrl(),
                request.status(),
                Instant.now()
        );
        prById.put(pr.prId(), pr);
        prByTaskId.put(pr.taskId(), pr);
        return toResponse(pr);
    }

    public List<PullRequestResponse> getAllPullRequests() {
        return prById.values().stream()
                .map(this::toResponse)
                .toList();
    }

    public PullRequestResponse getPullRequest(String prId) {
        PullRequest pr = prById.get(prId);
        if (pr == null) {
            throw new PullRequestNotFoundException("PullRequest not found: " + prId);
        }
        return toResponse(pr);
    }

    public PullRequestResponse getPullRequestByTaskId(String taskId) {
        PullRequest pr = prByTaskId.get(taskId);
        if (pr == null) {
            throw new PullRequestNotFoundException("PullRequest not found for task: " + taskId);
        }
        return toResponse(pr);
    }

    private PullRequestResponse toResponse(PullRequest pr) {
        return new PullRequestResponse(
                pr.prId(),
                pr.taskId(),
                pr.implId(),
                pr.reviewId(),
                pr.title(),
                pr.body(),
                pr.prUrl(),
                pr.status(),
                pr.createdAt()
        );
    }
}
