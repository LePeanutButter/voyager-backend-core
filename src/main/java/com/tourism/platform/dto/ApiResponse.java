package com.tourism.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standard API Response wrapper for consistent REST responses
 * 
 * This class provides a standardized response format for all API endpoints,
 * following REST best practices and providing consistent structure.
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
     * Success response factory method
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
     * Success response without data
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
     * Error response factory method
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
     * Error response without validation errors
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
