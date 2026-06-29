package com.arok2.dev_agent.common.exception;

public class DesignAlreadyExistsException extends RuntimeException {
    public DesignAlreadyExistsException(String designId) {
        super("Design already exists: " + designId);
    }
}
