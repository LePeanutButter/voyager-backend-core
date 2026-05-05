package com.tourism.platform.exception;

/**
 * Exception thrown when external service calls fail
 * 
 * This exception is used to indicate that an external service
 * (AI service, payment gateway, notification service, etc.) has failed.
 */
public class ExternalServiceException extends RuntimeException {

    /**
     * Create a new ExternalServiceException with a message.
     *
     * @param message human-readable message describing the external failure
     */
    public ExternalServiceException(String message) {
        super(message);
    }

    /**
     * Create a new ExternalServiceException with a message and underlying cause.
     *
     * @param message human-readable message
     * @param cause   underlying exception thrown by the external integration
     */
    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
