package com.arok2.dev_agent.common.exception;

public class AgentRunNotFoundException extends RuntimeException {

    public AgentRunNotFoundException(String runId) {
        super("AgentRun not found: " + runId);
    }
}
