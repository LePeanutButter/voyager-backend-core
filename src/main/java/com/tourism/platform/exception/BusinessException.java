package com.tourism.platform.exception;

/**
 * Exception thrown for business logic violations
 * 
 * This exception is used to indicate that a business rule or constraint
 * has been violated during the execution of business logic.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
