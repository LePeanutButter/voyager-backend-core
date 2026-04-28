package com.tourism.platform.exception;

/**
 * Exception for HTTP 400 request validation/business input issues.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
