package com.tourism.platform.exception;

/**
 * Exception thrown when a requested resource is not found
 * 
 * This exception is used to indicate that a specific resource
 * (user, destination, reservation, etc.) could not be found in the system.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
