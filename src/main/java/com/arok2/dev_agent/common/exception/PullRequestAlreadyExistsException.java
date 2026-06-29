package com.arok2.dev_agent.common.exception;

public class PullRequestAlreadyExistsException extends RuntimeException {
    public PullRequestAlreadyExistsException(String prId) {
        super("PullRequest already exists: " + prId);
    }
}
