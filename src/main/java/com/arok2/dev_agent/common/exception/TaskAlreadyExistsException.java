package com.arok2.dev_agent.common.exception;

public class TaskAlreadyExistsException extends RuntimeException {
    public TaskAlreadyExistsException(String taskId) {
        super("Task already exists: " + taskId);
    }
}
