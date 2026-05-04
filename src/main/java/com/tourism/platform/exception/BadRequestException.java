package com.tourism.platform.exception;

/**
 * Exception for HTTP 400 request validation/business input issues.
 */
public class BadRequestException extends RuntimeException {

    /**
     * Create a new BadRequestException with a descriptive message.
     *
     * @param message description of the bad request or validation problem
     */
    public BadRequestException(String message) {
        super(message);
    }
}
