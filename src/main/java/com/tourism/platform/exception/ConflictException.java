package com.tourism.platform.exception;

/**
 * Exception for 409 conflict scenarios.
 */
public class ConflictException extends RuntimeException {

    /**
     * Create a new ConflictException with a message describing the conflict.
     *
     * @param message description of the conflict scenario
     */
    public ConflictException(String message) {
        super(message);
    }
}
