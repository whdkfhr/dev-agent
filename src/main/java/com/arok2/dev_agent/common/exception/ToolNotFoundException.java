package com.arok2.dev_agent.common.exception;

public class ToolNotFoundException extends RuntimeException {

    public ToolNotFoundException(String toolName) {
        super("Tool not found: " + toolName);
    }
}
