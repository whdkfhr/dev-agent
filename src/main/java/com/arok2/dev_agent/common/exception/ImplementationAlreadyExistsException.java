package com.arok2.dev_agent.common.exception;

public class ImplementationAlreadyExistsException extends RuntimeException {
    public ImplementationAlreadyExistsException(String implId) {
        super("Implementation already exists: " + implId);
    }
}
