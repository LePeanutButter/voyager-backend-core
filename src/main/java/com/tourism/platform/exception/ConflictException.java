package com.tourism.platform.exception;

/**
 * Exception for 409 conflict scenarios.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
