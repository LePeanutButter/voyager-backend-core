package com.tourism.platform.service.impl;

import com.tourism.platform.dto.MatchResponseDto;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.model.TravelPlan;
import com.tourism.platform.model.TravelPlanStatus;
import com.tourism.platform.model.User;
import com.tourism.platform.repository.TravelPlanRepository;
import com.tourism.platform.repository.UserInterestRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceImplTest {

    @Mock
    private TravelPlanRepository travelPlanRepository;
    @Mock
    private UserInterestRepository userInterestRepository;

    private MatchingServiceImpl matchingService;

    @BeforeEach
    void setUp() {
        matchingService = new MatchingServiceImpl(travelPlanRepository, userInterestRepository, new SimpleMeterRegistry());
    }

    @Test
    void getMatchesShouldScoreAndSortDeterministically() {
        TravelPlan topCandidate = createPlan(2L, "alice", "Paris",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 5, 10, 0));

        TravelPlan lowCandidate = createPlan(1L, "bob", "Lima",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 3, 10, 0));

        when(travelPlanRepository.findAll())
                .thenReturn(List.of(lowCandidate, topCandidate));
        when(userInterestRepository.findUserInterestsByUserIds(any()))
                .thenReturn(List.of(
                        new Object[]{2L, "food"},
                        new Object[]{2L, "hiking"},
                        new Object[]{1L, "food"}
                ));

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
    void whenStartAfterEnd_throwsBusinessException() {
        String destination = "Paris";
        LocalDate start = LocalDate.of(2026, 6, 10);
        LocalDate end = LocalDate.of(2026, 6, 1);
        List<String> interests = List.of();
        assertThrows(BusinessException.class, () -> matchingService.getMatches(
                destination,
                start,
                end,
                interests
        ));
    }

    @Test
    void filtersInactivePlansAndNullDates() {
        TravelPlan active = createPlan(1L, "a", "X",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 3, 10, 0));

        TravelPlan draft = createPlan(2L, "b", "X",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 3, 10, 0));
        draft.setStatus(TravelPlanStatus.DRAFT);

        TravelPlan nullStart = createPlan(3L, "c", "X", null, LocalDateTime.of(2026, 5, 3, 10, 0));
        TravelPlan nullEnd = createPlan(4L, "d", "X", LocalDateTime.of(2026, 5, 2, 10, 0), null);

        when(travelPlanRepository.findAll()).thenReturn(List.of(draft, nullStart, nullEnd, active));
        when(userInterestRepository.findUserInterestsByUserIds(any())).thenReturn(List.of());

        List<MatchResponseDto> result = matchingService.getMatches(
                "X",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 5),
                List.of()
        );

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUserId());
    }

    @Test
    void filtersPlansOutsideRequestedWindow() {
        TravelPlan tooEarly = createPlan(1L, "a", "X",
                LocalDateTime.of(2026, 4, 1, 10, 0),
                LocalDateTime.of(2026, 4, 2, 10, 0));
        TravelPlan tooLate = createPlan(2L, "b", "X",
                LocalDateTime.of(2026, 6, 1, 10, 0),
                LocalDateTime.of(2026, 6, 2, 10, 0));
        TravelPlan ok = createPlan(3L, "c", "X",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 3, 10, 0));

        when(travelPlanRepository.findAll()).thenReturn(List.of(tooEarly, tooLate, ok));
        when(userInterestRepository.findUserInterestsByUserIds(any())).thenReturn(List.of());

        List<MatchResponseDto> result = matchingService.getMatches(
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 5),
                null
        );

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getUserId());
    }

    @Test
    void whenNoCandidates_doesNotQueryInterests() {
        when(travelPlanRepository.findAll()).thenReturn(List.of());

        List<MatchResponseDto> result = matchingService.getMatches(
                "Paris",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 5),
                List.of("x")
        );

        assertEquals(0, result.size());
        verifyNoInteractions(userInterestRepository);
    }

    @Test
    void normalizesInterestsAndTrimsDestinationMatch() {
        TravelPlan plan = createPlan(9L, "u", " Paris ",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 3, 10, 0));

        when(travelPlanRepository.findAll()).thenReturn(List.of(plan));
        when(userInterestRepository.findUserInterestsByUserIds(any()))
                .thenReturn(List.<Object[]>of(new Object[]{9L, " Food "}));

        List<MatchResponseDto> result = matchingService.getMatches(
                "paris",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 5),
                Arrays.asList("  FOOD  ", null, "  ", "food")
        );

        assertEquals(1, result.size());
        assertEquals(50, result.get(0).getDestinationPoints());
        assertTrue(result.get(0).getInterestPoints() > 0);
    }

    @Test
    void interestRowsWithNullOrBlank_areSkipped() {
        TravelPlan plan = createPlan(11L, "u", "Tokyo",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 3, 10, 0));

        when(travelPlanRepository.findAll()).thenReturn(List.of(plan));
        when(userInterestRepository.findUserInterestsByUserIds(any()))
                .thenReturn(List.of(
                        new Object[]{11L, null},
                        new Object[]{11L, "  "},
                        new Object[]{11L, "tea"}
                ));

        List<MatchResponseDto> result = matchingService.getMatches(
                "Tokyo",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 5),
                List.of("tea")
        );

        assertEquals(1, result.size());
        assertTrue(result.get(0).getInterestPoints() > 0);
    }

    @Test
    void twoPlansSameUser_keepsHigherScore() {
        TravelPlan low = createPlan(5L, "same", "Lima",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 2, 11, 0));
        TravelPlan high = createPlan(5L, "same", "Paris",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 5, 10, 0));

        when(travelPlanRepository.findAll()).thenReturn(List.of(low, high));
        when(userInterestRepository.findUserInterestsByUserIds(any())).thenReturn(List.of());

        List<MatchResponseDto> result = matchingService.getMatches(
                "Paris",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 5),
                List.of()
        );

        assertEquals(1, result.size());
        assertEquals("Paris", result.get(0).getDestination());
    }

    @Test
    void secondPlanLowerScore_doesNotReplaceBest() {
        TravelPlan high = createPlan(7L, "u", "Paris",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 5, 10, 0));
        TravelPlan low = createPlan(7L, "u", "Lima",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 3, 10, 0));

        when(travelPlanRepository.findAll()).thenReturn(List.of(high, low));
        when(userInterestRepository.findUserInterestsByUserIds(any())).thenReturn(List.of());

        List<MatchResponseDto> result = matchingService.getMatches(
                "Paris",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 5),
                List.of()
        );

        assertEquals(1, result.size());
        assertEquals("Paris", result.get(0).getDestination());
    }

    @Test
    void tieBreakSortsByUserIdAscending() {
        TravelPlan b = createPlan(2L, "b", "X",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 3, 10, 0));
        TravelPlan a = createPlan(1L, "a", "X",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 3, 10, 0));

        when(travelPlanRepository.findAll()).thenReturn(List.of(b, a));
        when(userInterestRepository.findUserInterestsByUserIds(any())).thenReturn(List.of());

        List<MatchResponseDto> result = matchingService.getMatches(
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 5),
                List.of()
        );

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getUserId());
        assertEquals(2L, result.get(1).getUserId());
        assertEquals(result.get(0).getScore(), result.get(1).getScore());
    }

    @Test
    void scoreZeroPlan_isExcludedFromResults() {
        TravelPlan tinyOverlap = createPlan(99L, "z", "Far",
                LocalDateTime.of(2026, 5, 1, 10, 0),
                LocalDateTime.of(2026, 5, 1, 11, 0));

        when(travelPlanRepository.findAll()).thenReturn(List.of(tinyOverlap));
        when(userInterestRepository.findUserInterestsByUserIds(any())).thenReturn(List.of());

        List<MatchResponseDto> result = matchingService.getMatches(
                null,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                List.of()
        );

        assertEquals(0, result.size());
    }

    @Test
    void invertedCandidateDates_yieldZeroDatePoints() {
        TravelPlan badDates = createPlan(88L, "x", "Somewhere",
                LocalDateTime.of(2026, 5, 5, 10, 0),
                LocalDateTime.of(2026, 5, 2, 10, 0));

        when(travelPlanRepository.findAll()).thenReturn(List.of(badDates));
        when(userInterestRepository.findUserInterestsByUserIds(any())).thenReturn(List.of());

        List<MatchResponseDto> result = matchingService.getMatches(
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 10),
                List.of()
        );

        assertEquals(0, result.size());
    }

    @Test
    void getMatchesShouldApplyAnyInterestFilteringAndReturnEmptyWhenNoMatch() {
        TravelPlan candidate = createPlan(10L, "carol", "Paris",
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 5, 10, 0));

        when(travelPlanRepository.findAll())
                .thenReturn(List.of(candidate));
        when(userInterestRepository.findUserInterestsByUserIds(any()))
                .thenReturn(List.<Object[]>of(new Object[]{10L, "museum"}));

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
                                  LocalDateTime endDate) {
        User user = User.builder()
                .id(userId)
                .username(username)
                .email(username + "@test.com")
                .password("pass")
                .firstName("First")
                .lastName("Last")
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
