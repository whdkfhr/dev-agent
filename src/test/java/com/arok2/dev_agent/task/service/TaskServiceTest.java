package com.arok2.dev_agent.task.service;

import com.arok2.dev_agent.common.exception.TaskAlreadyExistsException;
import com.arok2.dev_agent.common.exception.TaskNotFoundException;
import com.arok2.dev_agent.task.dto.TaskCreateRequest;
import com.arok2.dev_agent.task.dto.TaskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskServiceTest {

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService();
    }

    @Test
    void createTask_저장_후_응답_반환() {
        TaskCreateRequest request = new TaskCreateRequest("TASK-001", "Webhook 구현", "내용", "TODO");

        TaskResponse response = taskService.createTask(request);

        assertThat(response.taskId()).isEqualTo("TASK-001");
        assertThat(response.title()).isEqualTo("Webhook 구현");
        assertThat(response.status()).isEqualTo("TODO");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void createTask_중복_taskId_예외() {
        TaskCreateRequest request = new TaskCreateRequest("TASK-001", "제목", "내용", "TODO");
        taskService.createTask(request);

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(TaskAlreadyExistsException.class)
                .hasMessageContaining("TASK-001");
    }

    @Test
    void getAllTasks_전체_목록_반환() {
        taskService.createTask(new TaskCreateRequest("TASK-001", "제목1", "내용1", "TODO"));
        taskService.createTask(new TaskCreateRequest("TASK-002", "제목2", "내용2", "IN_PROGRESS"));

        List<TaskResponse> tasks = taskService.getAllTasks();

        assertThat(tasks).hasSize(2);
    }

    @Test
    void getTask_존재하는_taskId_반환() {
        taskService.createTask(new TaskCreateRequest("TASK-001", "제목", "내용", "TODO"));

        TaskResponse response = taskService.getTask("TASK-001");

        assertThat(response.taskId()).isEqualTo("TASK-001");
    }

    @Test
    void getTask_존재하지않는_taskId_예외() {
        assertThatThrownBy(() -> taskService.getTask("TASK-999"))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("TASK-999");
    }
}
