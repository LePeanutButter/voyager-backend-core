package com.tourism.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for representing traveler compatibility matches
 * 
 * This DTO contains information about travelers who have similar
 * destinations and compatible travel dates for social matching.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelerMatchDto {
    
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String bio;
    private Long travelPlanId;
    private String travelPlanTitle;
    private String destinationLocation;
    private LocalDateTime travelStartDate;
    private LocalDateTime travelEndDate;
    private Integer numberOfTravelers;
    private Integer daysOverlap;
    private Double compatibilityScore;
    
    /**
     * Creates a simplified traveler match for display purposes
     * 
     * @param userId the user ID
     * @param username the username
     * @param firstName the first name
     * @param lastName the last name
     * @param destinationLocation the destination location
     * @param travelStartDate the travel start date
     * @param travelEndDate the travel end date
     * @param daysOverlap number of overlapping days
     * @return TravelerMatchDto instance
     */
    public static TravelerMatchDto createSimpleMatch(Long userId, String username, 
                                                   String firstName, String lastName,
                                                   String destinationLocation,
                                                   LocalDateTime travelStartDate,
                                                   LocalDateTime travelEndDate,
                                                   Integer daysOverlap) {
        return TravelerMatchDto.builder()
                .userId(userId)
                .username(username)
                .firstName(firstName)
                .lastName(lastName)
                .destinationLocation(destinationLocation)
                .travelStartDate(travelStartDate)
                .travelEndDate(travelEndDate)
                .daysOverlap(daysOverlap)
                .build();
    }
}
