package com.tourism.platform.dto;

/**
 * DTO representing a traveler match result with identifying information and
 * travel date/location details. Used by matching endpoints to return concise
 * match summaries.
 */

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
        
        private TravelerMatchData(Builder builder) {
            this.userId = builder.userId;
            this.username = builder.username;
            this.firstName = builder.firstName;
            this.lastName = builder.lastName;
            this.destinationLocation = builder.destinationLocation;
            this.travelStartDate = builder.travelStartDate;
            this.travelEndDate = builder.travelEndDate;
            this.daysOverlap = builder.daysOverlap;
        }
        
        public static class Builder {
            private Long userId;
            private String username;
            private String firstName;
            private String lastName;
            private String destinationLocation;
            private LocalDateTime travelStartDate;
            private LocalDateTime travelEndDate;
            private Integer daysOverlap;
            
            public Builder userId(Long userId) {
                this.userId = userId;
                return this;
            }
            
            public Builder username(String username) {
                this.username = username;
                return this;
            }
            
            public Builder firstName(String firstName) {
                this.firstName = firstName;
                return this;
            }
            
            public Builder lastName(String lastName) {
                this.lastName = lastName;
                return this;
            }
            
            public Builder destinationLocation(String destinationLocation) {
                this.destinationLocation = destinationLocation;
                return this;
            }
            
            public Builder travelStartDate(LocalDateTime travelStartDate) {
                this.travelStartDate = travelStartDate;
                return this;
            }
            
            public Builder travelEndDate(LocalDateTime travelEndDate) {
                this.travelEndDate = travelEndDate;
                return this;
            }
            
            public Builder daysOverlap(Integer daysOverlap) {
                this.daysOverlap = daysOverlap;
                return this;
            }
            
            public TravelerMatchData build() {
                return new TravelerMatchData(this);
            }
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
