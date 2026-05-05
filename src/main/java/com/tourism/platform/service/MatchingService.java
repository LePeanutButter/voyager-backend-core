package com.tourism.platform.service;

import com.tourism.platform.dto.MatchResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface MatchingService {
    /**
     * Retrieve potential matches for travelers based on destination, dates and interests.
     *
     * @param destination destination location to match on
     * @param startDate   desired travel start date
     * @param endDate     desired travel end date
     * @param interests   list of traveler interests to consider for compatibility
     * @return list of MatchResponseDto describing compatible travelers
     */
    List<MatchResponseDto> getMatches(String destination,
                                      LocalDate startDate,
                                      LocalDate endDate,
                                      List<String> interests);
}
