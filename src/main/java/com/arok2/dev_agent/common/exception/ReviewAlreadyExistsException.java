package com.arok2.dev_agent.common.exception;

public class ReviewAlreadyExistsException extends RuntimeException {
    public ReviewAlreadyExistsException(String reviewId) {
        super("Review already exists: " + reviewId);
    }
}
