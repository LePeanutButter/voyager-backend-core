package com.tourism.platform.service.impl;

import com.tourism.platform.dto.MatchResponseDto;
import com.tourism.platform.model.TravelPlan;
import com.tourism.platform.model.TravelPlanStatus;
import com.tourism.platform.model.User;
import com.tourism.platform.repository.TravelPlanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceImplTest {

    @Mock
    private TravelPlanRepository travelPlanRepository;

    @InjectMocks
    private MatchingServiceImpl matchingService;

    @Test
    void getMatches_shouldScoreAndSortDeterministically() {
        TravelPlan topCandidate = createPlan(2L, "alice", "Paris",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 5, 10, 0),
                Set.of("food", "hiking"));

        TravelPlan lowCandidate = createPlan(1L, "bob", "Lima",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 3, 10, 0),
                Set.of("food"));

        when(travelPlanRepository.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(TravelPlanStatus.ACTIVE), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(lowCandidate, topCandidate));

        List<MatchResponseDto> result = matchingService.getMatches(
                "Paris",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 5),
                List.of("food", "hiking")
        );

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getUserId());
        assertEquals(94.0, result.get(0).getScore());
        assertEquals(1L, result.get(1).getUserId());
    }

    @Test
    void getMatches_shouldApplyAnyInterestFilteringAndReturnEmptyWhenNoMatch() {
        TravelPlan candidate = createPlan(10L, "carol", "Paris",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 5, 10, 0),
                Set.of("museum"));

        when(travelPlanRepository.findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(TravelPlanStatus.ACTIVE), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(candidate));

        List<MatchResponseDto> result = matchingService.getMatches(
                "Paris",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 5),
                List.of("beach", "food")
        );

        assertEquals(0, result.size());
    }

    private TravelPlan createPlan(Long userId,
                                  String username,
                                  String destination,
                                  LocalDateTime startDate,
                                  LocalDateTime endDate,
                                  Set<String> interests) {
        User user = User.builder()
                .id(userId)
                .username(username)
                .email(username + "@test.com")
                .password("pass")
                .firstName("First")
                .lastName("Last")
                .interests(interests)
                .build();

        TravelPlan plan = new TravelPlan();
        plan.setUser(user);
        plan.setStatus(TravelPlanStatus.ACTIVE);
        plan.setDestinationLocation(destination);
        plan.setStartDate(startDate);
        plan.setEndDate(endDate);
        return plan;
    }
}
