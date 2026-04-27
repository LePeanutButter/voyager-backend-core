package com.tourism.platform.dto;

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
