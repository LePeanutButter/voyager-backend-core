package com.tourism.platform.exception;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
    /**
     * Handle a ResourceNotFoundException and build a 404 response.
     *
     * @param ex      the ResourceNotFoundException thrown by controllers or services
     * @param request the current web request
     * @return ResponseEntity containing a populated ErrorResponse and HTTP 404 status
     */
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, ex, false);
    }

    /**
     * Handle MethodArgumentNotValidException produced by validation failures.
     *
     * @param ex      the validation exception containing binding results
     * @param request the current web request
     * @return ResponseEntity with validation error details and HTTP 400 status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(HttpStatus.BAD_REQUEST, message.isBlank() ? "Validation failed" : message, request,
                ex, false);
    }

    /**
     * Malformed JSON, unknown enum values, or other body deserialization failures.
     * Handles {@link HttpMessageNotReadableException} (Jackson / message conversion).
     *
     * @param ex      deserialization exception carrying the underlying cause detail
     * @param request the current web request
     * @return ResponseEntity with HTTP 400 and a descriptive message
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ApiResponse(responseCode = "400", description = "Invalid request body",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ErrorResponse> handleRequestBodyNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {
        Throwable cause = ex.getMostSpecificCause();
        String detail = cause.getMessage();
        String message = (detail != null && !detail.isBlank())
                ? detail
                : "Malformed or unreadable JSON request body";
        return buildResponse(HttpStatus.BAD_REQUEST, message, request, ex, false);
    }

    /**
     * Handle IllegalArgumentException and return HTTP 400.
     *
     * @param ex      the illegal argument exception
     * @param request the current web request
     * @return ResponseEntity with error details and HTTP 400
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
    /**
     * Handle BadRequestException thrown by domain logic and return HTTP 400.
     *
     * @param ex      the BadRequestException instance
     * @param request the current web request
     * @return ResponseEntity with error details and HTTP 400 status
     */
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
    /**
     * Handle authentication failures and return HTTP 401.
     *
     * @param ex      the AuthenticationException thrown during auth
     * @param request the current web request
     * @return ResponseEntity with generic authentication failure message and HTTP 401
     */
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
    /**
     * Handle access denied errors and return HTTP 403.
     *
     * @param ex      the AccessDeniedException indicating insufficient permissions
     * @param request the current web request
     * @return ResponseEntity with access denied message and HTTP 403
     */
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
    /**
     * Handle application BusinessException and return HTTP 400.
     *
     * @param ex      the business exception thrown from service layer
     * @param request the current web request
     * @return ResponseEntity describing the business error and HTTP 400
     */
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
    /**
     * Handle ConflictException and return HTTP 409.
     *
     * @param ex      the conflict exception describing the resource conflict
     * @param request the current web request
     * @return ResponseEntity with conflict details and HTTP 409
     */
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
    /**
     * Handle optimistic locking failures (concurrent update) and return HTTP 409.
     *
     * @param ex      the optimistic locking failure exception
     * @param request the current web request
     * @return ResponseEntity advising the client to retry the operation
     */
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
    /**
     * Handle failures from external integrations and return HTTP 502.
     *
     * @param ex      the ExternalServiceException containing integration error details
     * @param request the current web request
     * @return ResponseEntity with a generic external service error message and HTTP 502
     */
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
    /**
     * Catch-all exception handler that returns a 500 Internal Server Error.
     *
     * @param ex      uncaught exception
     * @param request the current web request
     * @return ResponseEntity with a generic internal server error message and HTTP 500
     */
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, ex, true);
    }

    /**
     * Helper to build a standard ErrorResponse payload and ResponseEntity.
     *
     * @param status         HTTP status to return
     * @param message        human-readable error message
     * @param request        current web request for extracting the path
     * @param ex             the exception that was thrown
     * @param withStacktrace whether to include the stacktrace in server logs
     * @return ResponseEntity containing the ErrorResponse and the provided status
     */
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

        /**
         * Default constructor used by frameworks.
         */
        public ErrorResponse() {}

        /**
         * Construct an ErrorResponse with full details.
         *
         * @param timestamp timestamp when the error occurred (UTC)
         * @param status    HTTP status code
         * @param error     reason phrase
         * @param message   human-readable message describing the error
         * @param path      endpoint path that produced the error
         * @param traceId   optional trace id for correlation
         */
        public ErrorResponse(OffsetDateTime timestamp, int status, String error, String message, String path, String traceId) {
            this.timestamp = timestamp;
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
            this.traceId = traceId;
        }

        // Getters and Setters
        /**
         * Get the timestamp for the error occurrence.
         *
         * @return error timestamp (UTC)
         */
        public OffsetDateTime getTimestamp() {
            return timestamp;
        }

        /**
         * Set the timestamp for the error occurrence.
         *
         * @param timestamp timestamp to set (UTC)
         */
        public void setTimestamp(OffsetDateTime timestamp) {
            this.timestamp = timestamp;
        }

        /**
         * Get the HTTP status code associated with this error.
         *
         * @return HTTP status code
         */
        public int getStatus() {
            return status;
        }

        /**
         * Set the HTTP status code for this error response.
         *
         * @param status HTTP status code
         */
        public void setStatus(int status) {
            this.status = status;
        }

        /**
         * Get the human-readable error message.
         *
         * @return message describing the error
         */
        public String getMessage() {
            return message;
        }

        /**
         * Set the human-readable error message.
         *
         * @param message descriptive error message
         */
        public void setMessage(String message) {
            this.message = message;
        }

        /**
         * Get the reason phrase associated with the HTTP status.
         *
         * @return reason phrase
         */
        public String getError() {
            return error;
        }

        /**
         * Set the reason phrase associated with the HTTP status.
         *
         * @param error reason phrase to set
         */
        public void setError(String error) {
            this.error = error;
        }

        /**
         * Get the request path where the error occurred.
         *
         * @return request path
         */
        public String getPath() {
            return path;
        }

        /**
         * Set the request path where the error occurred.
         *
         * @param path request path to set
         */
        public void setPath(String path) {
            this.path = path;
        }

        /**
         * Get the trace id used for correlation.
         *
         * @return trace id string or null if not present
         */
        public String getTraceId() {
            return traceId;
        }

        /**
         * Set the trace id for correlation.
         *
         * @param traceId trace identifier to set
         */
        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }
    }
}
