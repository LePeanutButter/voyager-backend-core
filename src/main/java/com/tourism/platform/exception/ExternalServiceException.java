package com.tourism.platform.exception;

/**
 * Exception thrown when external service calls fail
 * 
 * This exception is used to indicate that an external service
 * (AI service, payment gateway, notification service, etc.) has failed.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
