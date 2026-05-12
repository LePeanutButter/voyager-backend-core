package com.tourism.platform.exception;

/**
 * Exception thrown when a requested resource is not found
 * 
 * This exception is used to indicate that a specific resource
 * (user, destination, reservation, etc.) could not be found in the system.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Create a new ResourceNotFoundException with a message.
     *
     * @param message detail message explaining which resource was not found
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Create a new ResourceNotFoundException with a message and cause.
     *
     * @param message detail message
     * @param cause   underlying cause of the exception
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
