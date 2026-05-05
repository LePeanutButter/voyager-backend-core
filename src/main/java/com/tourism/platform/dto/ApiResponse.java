package com.tourism.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard API response wrapper for consistent REST responses across controllers
 * (status, message, payload, path, validation errors when applicable).
 *
 * @param <T> type of the response {@code data} payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Response timestamp")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code")
    private int status;

    @Schema(description = "Response message")
    private String message;

    @Schema(description = "Response data payload")
    private T data;

    @Schema(description = "Path of the endpoint")
    private String path;

    @Schema(description = "List of validation errors if any")
    private List<ValidationError> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Validation error details")
    public static class ValidationError {
        @Schema(description = "Field name with validation error")
        private String field;

        @Schema(description = "Error message for the field")
        private String message;
    }

    /**
     * Create a successful ApiResponse with a payload.
     *
     * @param status  HTTP status code representing the result
     * @param message human-readable message describing the result
     * @param data    payload data to include in the response
     * @param path    endpoint path that produced the response
     * @param <T>     type of the payload
     * @return ApiResponse containing the provided payload and metadata
     */
    public static <T> ApiResponse<T> success(int status, String message, T data, String path) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .message(message)
                .data(data)
                .path(path)
                .build();
    }

    /**
     * Create a successful ApiResponse without a payload.
     *
     * @param status  HTTP status code representing the result
     * @param message human-readable message describing the result
     * @param path    endpoint path that produced the response
     * @return ApiResponse without payload
     */
    public static ApiResponse<Void> success(int status, String message, String path) {
        return ApiResponse.<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .message(message)
                .path(path)
                .build();
    }

    /**
     * Create an error ApiResponse including validation errors.
     *
     * @param status  HTTP status code representing the error
     * @param message human-readable error message
     * @param path    endpoint path that produced the error
     * @param errors  list of validation errors to include
     * @param <T>     type parameter for completeness (usually Void)
     * @return ApiResponse containing error details
     */
    public static <T> ApiResponse<T> error(int status, String message, String path, List<ValidationError> errors) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .message(message)
                .path(path)
                .errors(errors)
                .build();
    }

    /**
     * Create an error ApiResponse without validation details.
     *
     * @param status  HTTP status code representing the error
     * @param message human-readable error message
     * @param path    endpoint path that produced the error
     * @param <T>     type parameter for completeness (usually Void)
     * @return ApiResponse containing high-level error information
     */
    public static <T> ApiResponse<T> error(int status, String message, String path) {
        return ApiResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .message(message)
                .path(path)
                .build();
    }
}
