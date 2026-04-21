package com.tourism.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Paginated API Response wrapper for consistent paginated responses
 * 
 * This class provides a standardized response format for paginated endpoints,
 * following REST best practices and providing consistent pagination structure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated API response wrapper")
public class PagedResponse<T> {

    @Schema(description = "Response timestamp")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code")
    private int status;

    @Schema(description = "Response message")
    private String message;

    @Schema(description = "Paginated data payload")
    private List<T> data;

    @Schema(description = "Current page number (0-based)")
    private int currentPage;

    @Schema(description = "Total number of pages")
    private int totalPages;

    @Schema(description = "Total number of elements")
    private long totalElements;

    @Schema(description = "Number of elements per page")
    private int pageSize;

    @Schema(description = "Whether this is the first page")
    private boolean first;

    @Schema(description = "Whether this is the last page")
    private boolean last;

    @Schema(description = "Path of the endpoint")
    private String path;

    /**
     * Success response factory method
     */
    public static <T> PagedResponse<T> of(
            int status, 
            String message, 
            List<T> data, 
            int currentPage, 
            int totalPages, 
            long totalElements, 
            int pageSize, 
            boolean first, 
            boolean last, 
            String path) {
        
        return PagedResponse.<T>builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .message(message)
                .data(data)
                .currentPage(currentPage)
                .totalPages(totalPages)
                .totalElements(totalElements)
                .pageSize(pageSize)
                .first(first)
                .last(last)
                .path(path)
                .build();
    }

    /**
     * Create from Spring Data Page
     */
    public static <T> PagedResponse<T> fromPage(
            org.springframework.data.domain.Page<T> page, 
            String message, 
            int status, 
            String path) {
        
        return PagedResponse.<T>of(
                status,
                message,
                page.getContent(),
                page.getNumber(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.isFirst(),
                page.isLast(),
                path
        );
    }
}
