package com.tourism.platform.dto;

/**
 * DTO representing a reservation associated with a travel plan activity.
 * Encapsulates reservation identifiers and relevant booking metadata.
 */

import com.tourism.platform.model.ReservationStatus;
import com.tourism.platform.model.ReservationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Reservation responses
 * 
 * This DTO is used to transfer reservation data between layers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reservation response DTO")
public class ReservationDto {

    @Schema(description = "Unique reservation identifier")
    private Long id;

    @Schema(description = "Reservation name")
    private String name;

    @Schema(description = "Reservation description")
    private String description;

    @Schema(description = "Reservation type")
    private ReservationType type;

    @Schema(description = "Reservation status")
    private ReservationStatus status;

    @Schema(description = "Confirmation number")
    private String confirmationNumber;

    @Schema(description = "Start date and time")
    private LocalDateTime startDate;

    @Schema(description = "End date and time")
    private LocalDateTime endDate;

    @Schema(description = "Reservation location")
    private String location;

    @Schema(description = "Service provider")
    private String serviceProvider;

    @Schema(description = "Total cost")
    private BigDecimal totalCost;

    @Schema(description = "Deposit paid")
    private BigDecimal depositPaid;

    @Schema(description = "Whether reservation is fully paid")
    private Boolean isPaid;

    @Schema(description = "Payment method")
    private String paymentMethod;

    @Schema(description = "Cancellation policy")
    private String cancellationPolicy;

    @Schema(description = "Special requests")
    private String specialRequests;

    @Schema(description = "Contact information")
    private String contactInfo;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
