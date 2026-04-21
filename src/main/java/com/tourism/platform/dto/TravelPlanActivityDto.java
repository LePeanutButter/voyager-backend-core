package com.tourism.platform.dto;

import com.tourism.platform.model.ActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Travel Plan Activity responses
 * 
 * This DTO is used to transfer activity data between layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Travel plan activity response DTO")
public class TravelPlanActivityDto {

    @Schema(description = "Unique activity identifier")
    private Long id;

    @Schema(description = "Activity name")
    private String name;

    @Schema(description = "Activity description")
    private String description;

    @Schema(description = "Activity type")
    private ActivityType type;

    @Schema(description = "Start time")
    private LocalDateTime startTime;

    @Schema(description = "End time")
    private LocalDateTime endTime;

    @Schema(description = "Activity location")
    private String location;

    @Schema(description = "Estimated cost")
    private BigDecimal estimatedCost;

    @Schema(description = "Actual cost")
    private BigDecimal actualCost;

    @Schema(description = "Booking reference")
    private String bookingReference;

    @Schema(description = "Whether activity is confirmed")
    private Boolean isConfirmed;

    @Schema(description = "Activity notes")
    private String notes;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
