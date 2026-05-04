package com.tourism.platform.dto;

/**
 * DTO representing a travel plan. Contains plan metadata and lists of
 * associated activities, participants and other related information used by
 * the API to transfer travel plan state.
 */

import com.tourism.platform.model.TravelPlanStatus;
import com.tourism.platform.model.TravelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Travel Plan responses
 * 
 * This DTO is used to transfer travel plan data between layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Travel plan response DTO")
public class TravelPlanDto {

    @Schema(description = "Unique travel plan identifier")
    private Long id;

    @Schema(description = "Travel plan title")
    private String title;

    @Schema(description = "Travel plan description")
    private String description;

    @Schema(description = "Travel plan status")
    private TravelPlanStatus status;

    @Schema(description = "Travel type")
    private TravelType travelType;

    @Schema(description = "Start date and time")
    private LocalDateTime startDate;

    @Schema(description = "End date and time")
    private LocalDateTime endDate;

    @Schema(description = "Estimated budget")
    private BigDecimal estimatedBudget;

    @Schema(description = "Actual cost")
    private BigDecimal actualCost;

    @Schema(description = "Number of travelers")
    private Integer numberOfTravelers;

    @Schema(description = "Origin location")
    private String originLocation;

    @Schema(description = "Destination location")
    private String destinationLocation;

    @Schema(description = "Whether the plan is public")
    private Boolean isPublic;

    @Schema(description = "Share token for public access")
    private String shareToken;

    @Schema(description = "Travel plan activities")
    private List<TravelPlanActivityDto> activities;

    @Schema(description = "Travel plan reservations")
    private List<ReservationDto> reservations;

    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
