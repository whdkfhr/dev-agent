package com.arok2.dev_agent.common.exception;

public class InvalidAgentRunStatusTransitionException extends RuntimeException {

    public InvalidAgentRunStatusTransitionException(String currentStatus, String nextStatus) {
        super("Invalid AgentRun status transition: " + currentStatus + " -> " + nextStatus);
    }
}
