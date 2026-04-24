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
    
    /**
     * Creates a simplified traveler match using builder pattern
     * 
     * @param matchData traveler match data object
     * @return TravelerMatchDto instance
     */
    public static TravelerMatchDto createSimpleMatch(TravelerMatchData matchData) {
        return TravelerMatchDto.builder()
                .userId(matchData.userId)
                .username(matchData.username)
                .firstName(matchData.firstName)
                .lastName(matchData.lastName)
                .destinationLocation(matchData.destinationLocation)
                .travelStartDate(matchData.travelStartDate)
                .travelEndDate(matchData.travelEndDate)
                .daysOverlap(matchData.daysOverlap)
                .build();
    }
    
    /**
     * Data class for traveler match parameters
     */
    public static class TravelerMatchData {
        private final Long userId;
        private final String username;
        private final String firstName;
        private final String lastName;
        private final String destinationLocation;
        private final LocalDateTime travelStartDate;
        private final LocalDateTime travelEndDate;
        private final Integer daysOverlap;
        
        public TravelerMatchData(Long userId, String username, String firstName, String lastName,
                               String destinationLocation, LocalDateTime travelStartDate,
                               LocalDateTime travelEndDate, Integer daysOverlap) {
            this.userId = userId;
            this.username = username;
            this.firstName = firstName;
            this.lastName = lastName;
            this.destinationLocation = destinationLocation;
            this.travelStartDate = travelStartDate;
            this.travelEndDate = travelEndDate;
            this.daysOverlap = daysOverlap;
        }
        
        // Getters
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getDestinationLocation() { return destinationLocation; }
        public LocalDateTime getTravelStartDate() { return travelStartDate; }
        public LocalDateTime getTravelEndDate() { return travelEndDate; }
        public Integer getDaysOverlap() { return daysOverlap; }
    }
}
