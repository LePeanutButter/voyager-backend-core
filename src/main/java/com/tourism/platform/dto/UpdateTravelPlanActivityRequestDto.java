package com.tourism.platform.dto;

/**
 * Request DTO used to update an existing travel plan activity. Carries fields
 * that can be modified by the client and validated in the controller layer.
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTravelPlanActivityRequestDto {

    @NotBlank(message = "Activity title is required")
    private String name;

    private String description;

    @NotNull(message = "startTime is required")
    private LocalDateTime startTime;

    @NotNull(message = "endTime is required")
    private LocalDateTime endTime;

    private String location;
}
