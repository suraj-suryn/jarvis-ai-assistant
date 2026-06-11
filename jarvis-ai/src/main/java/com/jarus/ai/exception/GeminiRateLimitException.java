package com.jarus.ai.exception;

public class GeminiRateLimitException extends RuntimeException {
    public GeminiRateLimitException(String message) {
        super(message);
    }
}
