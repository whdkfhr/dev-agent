package com.arok2.dev_agent.pullrequest.service;

import com.arok2.dev_agent.common.exception.PullRequestAlreadyExistsException;
import com.arok2.dev_agent.common.exception.PullRequestNotFoundException;
import com.arok2.dev_agent.pullrequest.dto.PullRequestCreateRequest;
import com.arok2.dev_agent.pullrequest.dto.PullRequestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PullRequestServiceTest {

    private PullRequestService pullRequestService;

    @BeforeEach
    void setUp() {
        pullRequestService = new PullRequestService();
    }

    @Test
    void createPullRequest_저장_후_응답_반환() {
        PullRequestCreateRequest request = new PullRequestCreateRequest(
                "PR-001", "TASK-001", "IMPL-001", "REVIEW-001",
                "feat: TASK-001 구현", "PR 본문",
                "https://github.com/whdkfhr/dev-agent/pull/1", "OPEN"
        );

        PullRequestResponse response = pullRequestService.createPullRequest(request);

        assertThat(response.prId()).isEqualTo("PR-001");
        assertThat(response.taskId()).isEqualTo("TASK-001");
        assertThat(response.implId()).isEqualTo("IMPL-001");
        assertThat(response.reviewId()).isEqualTo("REVIEW-001");
        assertThat(response.title()).isEqualTo("feat: TASK-001 구현");
        assertThat(response.prUrl()).isEqualTo("https://github.com/whdkfhr/dev-agent/pull/1");
        assertThat(response.status()).isEqualTo("OPEN");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void createPullRequest_중복_prId_예외() {
        PullRequestCreateRequest request = new PullRequestCreateRequest(
                "PR-001", "TASK-001", "IMPL-001", "REVIEW-001",
                "제목", "본문", "https://github.com/whdkfhr/dev-agent/pull/1", "OPEN"
        );
        pullRequestService.createPullRequest(request);

        assertThatThrownBy(() -> pullRequestService.createPullRequest(request))
                .isInstanceOf(PullRequestAlreadyExistsException.class)
                .hasMessageContaining("PR-001");
    }

    @Test
    void getAllPullRequests_전체_목록_반환() {
        pullRequestService.createPullRequest(new PullRequestCreateRequest(
                "PR-001", "TASK-001", "IMPL-001", "REVIEW-001",
                "제목1", "본문1", "https://github.com/whdkfhr/dev-agent/pull/1", "OPEN"
        ));
        pullRequestService.createPullRequest(new PullRequestCreateRequest(
                "PR-002", "TASK-002", "IMPL-002", "REVIEW-002",
                "제목2", "본문2", "https://github.com/whdkfhr/dev-agent/pull/2", "MERGED"
        ));

        List<PullRequestResponse> pullRequests = pullRequestService.getAllPullRequests();

        assertThat(pullRequests).hasSize(2);
    }

    @Test
    void getPullRequest_존재하는_prId_반환() {
        pullRequestService.createPullRequest(new PullRequestCreateRequest(
                "PR-001", "TASK-001", "IMPL-001", "REVIEW-001",
                "제목", "본문", "https://github.com/whdkfhr/dev-agent/pull/1", "OPEN"
        ));

        PullRequestResponse response = pullRequestService.getPullRequest("PR-001");

        assertThat(response.prId()).isEqualTo("PR-001");
    }

    @Test
    void getPullRequest_존재하지않는_prId_예외() {
        assertThatThrownBy(() -> pullRequestService.getPullRequest("PR-999"))
                .isInstanceOf(PullRequestNotFoundException.class)
                .hasMessageContaining("PR-999");
    }

    @Test
    void getPullRequestByTaskId_연관_PullRequest_반환() {
        pullRequestService.createPullRequest(new PullRequestCreateRequest(
                "PR-001", "TASK-001", "IMPL-001", "REVIEW-001",
                "제목", "본문", "https://github.com/whdkfhr/dev-agent/pull/1", "OPEN"
        ));

        PullRequestResponse response = pullRequestService.getPullRequestByTaskId("TASK-001");

        assertThat(response.taskId()).isEqualTo("TASK-001");
        assertThat(response.prId()).isEqualTo("PR-001");
    }

    @Test
    void getPullRequestByTaskId_존재하지않는_taskId_예외() {
        assertThatThrownBy(() -> pullRequestService.getPullRequestByTaskId("TASK-999"))
                .isInstanceOf(PullRequestNotFoundException.class)
                .hasMessageContaining("TASK-999");
    }
}
