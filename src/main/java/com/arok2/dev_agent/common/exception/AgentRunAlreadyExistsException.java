package com.arok2.dev_agent.common.exception;

public class AgentRunAlreadyExistsException extends RuntimeException {

    public AgentRunAlreadyExistsException(String runId) {
        super("AgentRun already exists: " + runId);
    }
}
