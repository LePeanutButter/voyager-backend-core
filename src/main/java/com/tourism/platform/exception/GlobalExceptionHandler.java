package com.tourism.platform.exception;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for the Tourism Platform
 * 
 * This class handles exceptions globally across the application,
 * providing consistent error responses and proper HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ApiResponse(responseCode = "404", description = "Resource not found", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ApiResponse(responseCode = "400", description = "Validation failed", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                LocalDateTime.now(),
                request.getDescription(false),
                errors
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ApiResponse(responseCode = "400", description = "Invalid argument", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    @ApiResponse(responseCode = "401", description = "Authentication failed", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication failed: " + ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ApiResponse(responseCode = "403", description = "Access denied", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Access denied: " + ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle business logic exceptions
     */
    @ExceptionHandler(BusinessException.class)
    @ApiResponse(responseCode = "400", description = "Business logic error", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle external service exceptions
     */
    @ExceptionHandler(ExternalServiceException.class)
    @ApiResponse(responseCode = "502", description = "External service error", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleExternalServiceException(
            ExternalServiceException ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_GATEWAY.value(),
                "External service error: " + ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_GATEWAY);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    @ApiResponse(responseCode = "500", description = "Internal server error", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred: " + ex.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Error response class for consistent API error responses
     */
    @Schema(description = "Standard error response format")
    public static class ErrorResponse {
        @Schema(description = "HTTP status code")
        private int status;
        
        @Schema(description = "Error message")
        private String message;
        
        @Schema(description = "Timestamp of the error")
        private LocalDateTime timestamp;
        
        @Schema(description = "Request description")
        private String path;
        
        @Schema(description = "Validation errors (if any)")
        private Map<String, String> validationErrors;

        public ErrorResponse() {}

        public ErrorResponse(int status, String message, LocalDateTime timestamp, String path) {
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
            this.path = path;
        }

        public ErrorResponse(int status, String message, LocalDateTime timestamp, String path, 
                           Map<String, String> validationErrors) {
            this(status, message, timestamp, path);
            this.validationErrors = validationErrors;
        }

        // Getters and Setters
        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public Map<String, String> getValidationErrors() {
            return validationErrors;
        }

        public void setValidationErrors(Map<String, String> validationErrors) {
            this.validationErrors = validationErrors;
        }
    }
}
