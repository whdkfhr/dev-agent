package com.arok2.dev_agent.common.exception;

public class PullRequestNotFoundException extends RuntimeException {
    public PullRequestNotFoundException(String message) {
        super(message);
    }
}
