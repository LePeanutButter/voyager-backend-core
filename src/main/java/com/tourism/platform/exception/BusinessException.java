package com.tourism.platform.exception;

/**
 * Exception thrown for business logic violations
 * 
 * This exception is used to indicate that a business rule or constraint
 * has been violated during the execution of business logic.
 */
public class BusinessException extends RuntimeException {

    /**
     * Create a new BusinessException with a message.
     *
     * @param message explanation of the business rule violation
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * Create a new BusinessException with a message and cause.
     *
     * @param message detail message
     * @param cause   underlying cause of the business exception
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
