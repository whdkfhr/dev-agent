package com.arok2.dev_agent.common.exception;

public record ErrorResponse(
        String code,
        String message
) {}
