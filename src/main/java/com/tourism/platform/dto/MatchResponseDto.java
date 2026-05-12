package com.tourism.platform.dto;

/**
 * DTO used to return a match result to the client. Wraps compatibility and
 * traveler identity information returned by the matching service.
 */

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MatchResponseDto {
    Long userId;
    String username;
    String destination;
    Double score;
    Integer destinationPoints;
    Integer datePoints;
    Integer interestPoints;
}
