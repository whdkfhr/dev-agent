package com.arok2.dev_agent.task.service;

import com.arok2.dev_agent.common.exception.TaskAlreadyExistsException;
import com.arok2.dev_agent.common.exception.TaskNotFoundException;
import com.arok2.dev_agent.task.domain.Task;
import com.arok2.dev_agent.task.dto.TaskCreateRequest;
import com.arok2.dev_agent.task.dto.TaskResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskService {

    private final Map<String, Task> taskStore = new ConcurrentHashMap<>();

    public TaskResponse createTask(TaskCreateRequest request) {
        if (taskStore.containsKey(request.taskId())) {
            throw new TaskAlreadyExistsException(request.taskId());
        }

        Task task = new Task(
                request.taskId(),
                request.title(),
                request.content(),
                request.status(),
                Instant.now()
        );
        taskStore.put(task.taskId(), task);

        return toResponse(task);
    }

    public List<TaskResponse> getAllTasks() {
        return taskStore.values().stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse getTask(String taskId) {
        Task task = taskStore.get(taskId);
        if (task == null) {
            throw new TaskNotFoundException(taskId);
        }
        return toResponse(task);
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(task.taskId(), task.title(), task.status(), task.createdAt());
    }
}
