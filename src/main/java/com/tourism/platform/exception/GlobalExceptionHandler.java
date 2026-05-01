package com.tourism.platform.exception;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for the Tourism Platform
 * 
 * This class handles exceptions globally across the application,
 * providing consistent error responses and proper HTTP status codes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle JPA entity not found (e.g. invalid id for getReference / findById workflows).
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex, WebRequest request) {

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, ex, false);
    }

    /**
     * Handle resource not found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ApiResponse(responseCode = "404", description = "Resource not found", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, ex, false);
    }

    /**
     * Handle validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ApiResponse(responseCode = "400", description = "Validation failed", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, message.isBlank() ? "Validation failed" : message, request, ex, false);
    }

    /**
     * Handle illegal argument exceptions
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ApiResponse(responseCode = "400", description = "Invalid argument", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex, false);
    }

    /**
     * Handle domain bad request exceptions.
     */
    @ExceptionHandler(BadRequestException.class)
    @ApiResponse(responseCode = "400", description = "Bad request",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            BadRequestException ex, WebRequest request) {

        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex, false);
    }

    /**
     * Handle authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    @ApiResponse(responseCode = "401", description = "Authentication failed", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication failed", request, ex, false);
    }

    /**
     * Handle access denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ApiResponse(responseCode = "403", description = "Access denied", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied", request, ex, false);
    }

    /**
     * Handle business logic exceptions
     */
    @ExceptionHandler(BusinessException.class)
    @ApiResponse(responseCode = "400", description = "Business logic error", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {
        
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex, false);
    }

    /**
     * Handle conflict exceptions.
     */
    @ExceptionHandler(ConflictException.class)
    @ApiResponse(responseCode = "409", description = "Conflict",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleConflictException(
            ConflictException ex, WebRequest request) {

        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request, ex, false);
    }

    /**
     * Handle optimistic locking conflicts.
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ApiResponse(responseCode = "409", description = "Optimistic locking conflict",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleOptimisticLockException(
            ObjectOptimisticLockingFailureException ex, WebRequest request) {

        return buildResponse(HttpStatus.CONFLICT, "Concurrent update detected. Please retry your request.", request, ex, false);
    }

    /**
     * Handle external service exceptions
     */
    @ExceptionHandler(ExternalServiceException.class)
    @ApiResponse(responseCode = "502", description = "External service error", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleExternalServiceException(
            ExternalServiceException ex, WebRequest request) {
        
        return buildResponse(HttpStatus.BAD_GATEWAY, "External service error", request, ex, false);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    @ApiResponse(responseCode = "500", description = "Internal server error", 
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, ex, true);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status,
                                                        String message,
                                                        WebRequest request,
                                                        Exception ex,
                                                        boolean withStacktrace) {
        String path = request instanceof ServletWebRequest servletWebRequest
                ? servletWebRequest.getRequest().getRequestURI()
                : request.getDescription(false);
        String traceId = MDC.get("traceId");

        if (withStacktrace) {
            log.error("event=exception_handled status={} exceptionType={} traceId={}",
                    status.value(), ex.getClass().getSimpleName(), traceId, ex);
        } else {
            log.warn("event=exception_handled status={} exceptionType={} traceId={}",
                    status.value(), ex.getClass().getSimpleName(), traceId);
        }

        ErrorResponse errorResponse = new ErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                traceId
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Error response class for consistent API error responses
     */
    @Schema(description = "Standard error response format")
    public static class ErrorResponse {
        @Schema(description = "Timestamp of the error")
        private OffsetDateTime timestamp;

        @Schema(description = "HTTP status code")
        private int status;

        @Schema(description = "Reason phrase")
        private String error;

        @Schema(description = "Error message")
        private String message;

        @Schema(description = "Request path")
        private String path;

        @Schema(description = "Trace id")
        private String traceId;

        public ErrorResponse() {}

        public ErrorResponse(OffsetDateTime timestamp, int status, String error, String message, String path, String traceId) {
            this.timestamp = timestamp;
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
            this.traceId = traceId;
        }

        // Getters and Setters
        public OffsetDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(OffsetDateTime timestamp) {
            this.timestamp = timestamp;
        }

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

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }
    }
}
