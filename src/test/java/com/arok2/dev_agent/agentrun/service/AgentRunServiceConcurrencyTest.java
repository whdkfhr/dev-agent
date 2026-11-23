package com.arok2.dev_agent.agentrun.service;

import com.arok2.dev_agent.agentrun.domain.AgentRunStatus;
import com.arok2.dev_agent.agentrun.domain.AgentStepStatus;
import com.arok2.dev_agent.agentrun.dto.AgentRunCreateRequest;
import com.arok2.dev_agent.agentrun.dto.AgentRunResponse;
import com.arok2.dev_agent.agentrun.dto.AgentStepLogCreateRequest;
import com.arok2.dev_agent.agentrun.dto.AgentStepLogResponse;
import com.arok2.dev_agent.common.exception.AgentRunAlreadyExistsException;
import com.arok2.dev_agent.common.exception.InvalidAgentRunStatusTransitionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 같은 runId에 대한 동시 요청에서 성공한 요청의 결과가 정확히 한 번씩 보존되는지 검증한다.
 * 시작 배리어로 동시에 출발시키고, 새 서비스로 시나리오를 반복한다.
 * 기존 공개 API와 기존 예외만 사용하므로 수정 전 코드에서도 그대로 실행할 수 있다.
 */
class AgentRunServiceConcurrencyTest {

    private static final int REPETITIONS = 200;
    private static final int THREADS = 8;
    private static final long WAIT_SECONDS = 10;
    private static final String RUN_ID = "RUN-CONCURRENCY";

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(THREADS);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @Timeout(120)
    void 같은_runId_동시_생성은_정확히_한_건만_성공() throws Exception {
        int violations = 0;
        for (int round = 0; round < REPETITIONS; round++) {
            AgentRunService service = newService();
            List<Outcome<AgentRunResponse>> outcomes = runConcurrently(THREADS, i ->
                    () -> service.createAgentRun(new AgentRunCreateRequest(RUN_ID, "ISSUE-" + i, "동시 생성")));

            List<AgentRunResponse> successes = successes(outcomes);
            boolean othersAreDuplicates = failures(outcomes).stream()
                    .allMatch(AgentRunAlreadyExistsException.class::isInstance);
            boolean ok = successes.size() == 1
                    && othersAreDuplicates
                    && service.getAgentRun(RUN_ID).issueId().equals(successes.get(0).issueId());
            if (!ok) {
                violations++;
            }
        }
        report("duplicate-create", violations);
        assertThat(violations).isZero();
    }

    @Test
    @Timeout(120)
    void 서로_다른_step_log_동시_추가는_모두_정확히_한_번씩_저장() throws Exception {
        int violations = 0;
        for (int round = 0; round < REPETITIONS; round++) {
            AgentRunService service = newServiceWithRun();
            List<Outcome<AgentRunResponse>> outcomes = runConcurrently(THREADS, i ->
                    () -> service.addStepLog(RUN_ID, stepRequest("step-" + i)));

            List<String> expected = IntStream.range(0, THREADS).mapToObj(i -> "step-" + i).toList();
            boolean ok = failures(outcomes).isEmpty()
                    && hasExactlyOnce(storedStepNames(service), expected);
            if (!ok) {
                violations++;
            }
        }
        report("concurrent-step-logs", violations);
        assertThat(violations).isZero();
    }

    @Test
    @Timeout(120)
    void 상태_전이와_step_log가_겹쳐도_모두_보존() throws Exception {
        int stepCount = THREADS - 1;
        int violations = 0;
        for (int round = 0; round < REPETITIONS; round++) {
            AgentRunService service = newServiceWithRun();
            List<Outcome<AgentRunResponse>> outcomes = runConcurrently(THREADS, i -> i == 0
                    ? () -> service.advanceStatus(RUN_ID, AgentRunStatus.DESIGNING)
                    : () -> service.addStepLog(RUN_ID, stepRequest("step-" + i)));

            List<String> expected = IntStream.rangeClosed(1, stepCount).mapToObj(i -> "step-" + i).toList();
            AgentRunResponse stored = service.getAgentRun(RUN_ID);
            boolean ok = failures(outcomes).isEmpty()
                    && stored.status() == AgentRunStatus.DESIGNING
                    && hasExactlyOnce(storedStepNames(service), expected);
            if (!ok) {
                violations++;
            }
        }
        report("advance-with-step-logs", violations);
        assertThat(violations).isZero();
    }

    @Test
    @Timeout(120)
    void 같은_상태로의_동시_전이는_정확히_한_건만_성공() throws Exception {
        int violations = 0;
        for (int round = 0; round < REPETITIONS; round++) {
            AgentRunService service = newServiceWithRun();
            List<Outcome<AgentRunResponse>> outcomes = runConcurrently(THREADS, i ->
                    () -> service.advanceStatus(RUN_ID, AgentRunStatus.DESIGNING));

            boolean othersAreInvalidTransitions = failures(outcomes).stream()
                    .allMatch(InvalidAgentRunStatusTransitionException.class::isInstance);
            boolean ok = successes(outcomes).size() == 1
                    && othersAreInvalidTransitions
                    && service.getAgentRun(RUN_ID).status() == AgentRunStatus.DESIGNING;
            if (!ok) {
                violations++;
            }
        }
        report("duplicate-advance", violations);
        assertThat(violations).isZero();
    }

    private AgentRunService newService() {
        return new AgentRunService(new ToolRegistryService());
    }

    private AgentRunService newServiceWithRun() {
        AgentRunService service = newService();
        service.createAgentRun(new AgentRunCreateRequest(RUN_ID, "ISSUE-1", "동시성 테스트"));
        return service;
    }

    private AgentStepLogCreateRequest stepRequest(String stepName) {
        return new AgentStepLogCreateRequest(stepName, "input", "output", "TaskDocumentTool",
                AgentStepStatus.SUCCESS, false);
    }

    private List<String> storedStepNames(AgentRunService service) {
        return service.getAgentRun(RUN_ID).stepLogs().stream()
                .map(AgentStepLogResponse::stepName)
                .toList();
    }

    /** 저장된 이름이 기대 이름 집합과 같고, 각 이름이 정확히 한 번씩 나타나는지 확인한다. */
    private boolean hasExactlyOnce(List<String> stored, List<String> expected) {
        Set<String> distinct = Set.copyOf(stored);
        return stored.size() == expected.size()
                && distinct.size() == stored.size()
                && distinct.equals(Set.copyOf(expected));
    }

    private <T> List<Outcome<T>> runConcurrently(int taskCount, IntFunction<Callable<T>> taskFactory)
            throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            Callable<T> task = taskFactory.apply(i);
            futures.add(executor.submit(() -> {
                ready.countDown();
                if (!start.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("start barrier timed out");
                }
                return task.call();
            }));
        }
        assertThat(ready.await(WAIT_SECONDS, TimeUnit.SECONDS)).as("all tasks ready").isTrue();
        start.countDown();

        List<Outcome<T>> outcomes = new ArrayList<>();
        for (Future<T> future : futures) {
            try {
                outcomes.add(Outcome.success(future.get(WAIT_SECONDS, TimeUnit.SECONDS)));
            } catch (ExecutionException e) {
                outcomes.add(Outcome.failure(e.getCause()));
            } catch (java.util.concurrent.TimeoutException e) {
                throw new AssertionError("concurrent task timed out", e);
            }
        }
        return outcomes;
    }

    private <T> List<T> successes(List<Outcome<T>> outcomes) {
        return outcomes.stream().filter(o -> o.error() == null).map(Outcome::value).toList();
    }

    private <T> List<Throwable> failures(List<Outcome<T>> outcomes) {
        return outcomes.stream().map(Outcome::error).filter(e -> e != null).collect(Collectors.toList());
    }

    private void report(String scenario, int violations) {
        System.out.printf("[concurrency] scenario=%s repetitions=%d threads=%d violations=%d%n",
                scenario, REPETITIONS, THREADS, violations);
    }

    private record Outcome<T>(T value, Throwable error) {
        static <T> Outcome<T> success(T value) {
            return new Outcome<>(value, null);
        }

        static <T> Outcome<T> failure(Throwable error) {
            return new Outcome<>(null, error);
        }
    }
}
