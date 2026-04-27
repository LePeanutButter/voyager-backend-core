package com.tourism.platform.service;

import com.tourism.platform.dto.MatchResponseDto;

import java.time.LocalDate;
import java.util.List;

public interface MatchingService {
    List<MatchResponseDto> getMatches(String destination,
                                      LocalDate startDate,
                                      LocalDate endDate,
                                      List<String> interests);
}
