package com.tourism.platform.service.impl;

import com.tourism.platform.dto.CompatibilityMatchRequest;
import com.tourism.platform.dto.CompatibilityMatchResponse;
import com.tourism.platform.exception.BadRequestException;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.TravelPlan;
import com.tourism.platform.model.User;
import com.tourism.platform.repository.TravelPlanRepository;
import com.tourism.platform.repository.UserInterestRepository;
import com.tourism.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompatibilityMatchingServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TravelPlanRepository travelPlanRepository;
    @Mock
    private UserInterestRepository userInterestRepository;

    @InjectMocks
    private CompatibilityMatchingServiceImpl service;

    private CompatibilityMatchRequest request;
    private User requester;

    @BeforeEach
    void setUp() {
        requester = new User();
        requester.setId(1L);
        requester.setUsername("requester");

        request = new CompatibilityMatchRequest();
        request.setDestination("Paris");
        request.setStartDate(LocalDate.of(2026, 6, 1));
        request.setEndDate(LocalDate.of(2026, 6, 10));
        request.setInterests(List.of("food", "museums"));
    }

    @Test
    void shouldComputeExpectedScoreBreakdown() {
        User candidate = user(2L, "candidate");
        TravelPlan exactPlan = plan(2L, "Paris",
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 10, 23, 59));

        when(userRepository.findByUsername("requester")).thenReturn(Optional.of(requester));
        when(userRepository.findAll()).thenReturn(List.of(requester, candidate));
        when(travelPlanRepository.findByUserIdIn(Set.of(2L))).thenReturn(List.of(exactPlan));
        when(userInterestRepository.findUserInterestsByUserIds(Set.of(2L)))
                .thenReturn(List.<Object[]>of(new Object[]{2L, "food"}, new Object[]{2L, "hiking"}));

        List<CompatibilityMatchResponse> result = service.findMatches(request, "requester");
        assertEquals(1, result.size());
        CompatibilityMatchResponse match = result.get(0);
        assertEquals(50.0, match.getDestinationScore());
        assertEquals(30.0, match.getDateProximityScore());
        assertEquals(6.67, match.getInterestScore());
        assertEquals(86.67, match.getTotalScore());
    }

    @Test
    void shouldSortDeterministicallyByScoreThenUserId() {
        User candidateA = user(2L, "a");
        User candidateB = user(3L, "b");

        TravelPlan planA = plan(2L, "Paris",
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 10, 0, 0));
        TravelPlan planB = plan(3L, "Paris",
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 10, 0, 0));

        when(userRepository.findByUsername("requester")).thenReturn(Optional.of(requester));
        when(userRepository.findAll()).thenReturn(List.of(requester, candidateB, candidateA));
        when(travelPlanRepository.findByUserIdIn(Set.of(2L, 3L))).thenReturn(List.of(planA, planB));
        when(userInterestRepository.findUserInterestsByUserIds(Set.of(2L, 3L)))
                .thenReturn(List.<Object[]>of(new Object[]{2L, "food"}, new Object[]{3L, "food"}));

        List<CompatibilityMatchResponse> result = service.findMatches(request, "requester");
        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getUserId());
        assertEquals(3L, result.get(1).getUserId());
    }

    @Test
    void shouldFilterWhenNoInterestMatches() {
        User candidate = user(2L, "candidate");
        TravelPlan exactPlan = plan(2L, "Paris",
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 10, 0, 0));

        when(userRepository.findByUsername("requester")).thenReturn(Optional.of(requester));
        when(userRepository.findAll()).thenReturn(List.of(requester, candidate));
        when(travelPlanRepository.findByUserIdIn(Set.of(2L))).thenReturn(List.of(exactPlan));
        when(userInterestRepository.findUserInterestsByUserIds(Set.of(2L)))
                .thenReturn(List.<Object[]>of(new Object[]{2L, "beaches"}));

        List<CompatibilityMatchResponse> result = service.findMatches(request, "requester");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenNoCandidatesMatchAnyRule() {
        User candidate = user(2L, "candidate");
        when(userRepository.findByUsername("requester")).thenReturn(Optional.of(requester));
        when(userRepository.findAll()).thenReturn(List.of(requester, candidate));
        when(travelPlanRepository.findByUserIdIn(Set.of(2L))).thenReturn(Collections.emptyList());
        when(userInterestRepository.findUserInterestsByUserIds(Set.of(2L))).thenReturn(List.<Object[]>of());

        CompatibilityMatchRequest noInterestFilterRequest = new CompatibilityMatchRequest();
        noInterestFilterRequest.setDestination("Tokyo");
        noInterestFilterRequest.setStartDate(LocalDate.of(2026, 7, 1));
        noInterestFilterRequest.setEndDate(LocalDate.of(2026, 7, 5));
        noInterestFilterRequest.setInterests(null);

        List<CompatibilityMatchResponse> result = service.findMatches(noInterestFilterRequest, "requester");
        assertTrue(result.isEmpty());
    }

    @Test
    void throwsBadRequestWhenStartAfterEnd() {
        CompatibilityMatchRequest bad = new CompatibilityMatchRequest();
        bad.setStartDate(LocalDate.of(2026, 8, 10));
        bad.setEndDate(LocalDate.of(2026, 8, 1));

        assertThrows(BadRequestException.class, () -> service.findMatches(bad, "requester"));
    }

    @Test
    void throwsWhenRequesterNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findMatches(request, "ghost"));
    }

    @Test
    void returnsEmptyWhenNoOtherUsers() {
        when(userRepository.findByUsername("requester")).thenReturn(Optional.of(requester));
        when(userRepository.findAll()).thenReturn(List.of(requester));
        when(travelPlanRepository.findByUserIdIn(Collections.emptySet())).thenReturn(List.of());

        assertTrue(service.findMatches(request, "requester").isEmpty());
    }

    @Test
    void includesMatchWhenInterestsEmptyAndDestinationMatches() {
        User candidate = user(2L, "c");
        TravelPlan plan = plan(2L, "Paris",
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 5, 0, 0));

        CompatibilityMatchRequest wide = new CompatibilityMatchRequest();
        wide.setDestination("Paris");
        wide.setStartDate(LocalDate.of(2026, 6, 1));
        wide.setEndDate(LocalDate.of(2026, 6, 30));
        wide.setInterests(null);

        when(userRepository.findByUsername("requester")).thenReturn(Optional.of(requester));
        when(userRepository.findAll()).thenReturn(List.of(requester, candidate));
        when(travelPlanRepository.findByUserIdIn(Set.of(2L))).thenReturn(List.of(plan));
        when(userInterestRepository.findUserInterestsByUserIds(Set.of(2L))).thenReturn(List.of());

        List<CompatibilityMatchResponse> result = service.findMatches(wide, "requester");
        assertEquals(1, result.size());
        assertTrue(result.get(0).getTotalScore() > 0);
    }

    @Test
    void skipsInterestRowsWithNullOrBlank() {
        User candidate = user(2L, "c");
        TravelPlan plan = plan(2L, "Paris",
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 10, 0, 0));

        when(userRepository.findByUsername("requester")).thenReturn(Optional.of(requester));
        when(userRepository.findAll()).thenReturn(List.of(requester, candidate));
        when(travelPlanRepository.findByUserIdIn(Set.of(2L))).thenReturn(List.of(plan));
        when(userInterestRepository.findUserInterestsByUserIds(Set.of(2L)))
                .thenReturn(List.<Object[]>of(new Object[]{2L, null}, new Object[]{2L, "  "}, new Object[]{2L, "Food"}));

        CompatibilityMatchRequest r = new CompatibilityMatchRequest();
        r.setDestination("Paris");
        r.setStartDate(LocalDate.of(2026, 6, 1));
        r.setEndDate(LocalDate.of(2026, 6, 10));
        r.setInterests(List.of("food"));

        List<CompatibilityMatchResponse> result = service.findMatches(r, "requester");
        assertEquals(1, result.size());
        assertEquals(List.of("food"), result.get(0).getMatchedInterests());
    }

    @Test
    void planWithNullDatesGetsZeroDateScore() {
        User candidate = user(2L, "c");
        TravelPlan p = plan(2L, "Paris", null, null);

        when(userRepository.findByUsername("requester")).thenReturn(Optional.of(requester));
        when(userRepository.findAll()).thenReturn(List.of(requester, candidate));
        when(travelPlanRepository.findByUserIdIn(Set.of(2L))).thenReturn(List.of(p));
        when(userInterestRepository.findUserInterestsByUserIds(Set.of(2L)))
                .thenReturn(List.<Object[]>of(new Object[]{2L, "food"}));

        List<CompatibilityMatchResponse> result = service.findMatches(request, "requester");
        assertEquals(1, result.size());
        assertEquals(0.0, result.get(0).getDateProximityScore());
    }

    @Test
    void picksBestOverlapAmongMultiplePlans() {
        User candidate = user(2L, "c");
        TravelPlan weak = plan(2L, "Paris",
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 1, 0, 0));
        TravelPlan strong = plan(2L, "Paris",
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 10, 0, 0));

        when(userRepository.findByUsername("requester")).thenReturn(Optional.of(requester));
        when(userRepository.findAll()).thenReturn(List.of(requester, candidate));
        when(travelPlanRepository.findByUserIdIn(Set.of(2L))).thenReturn(List.of(weak, strong));
        when(userInterestRepository.findUserInterestsByUserIds(Set.of(2L)))
                .thenReturn(List.<Object[]>of(new Object[]{2L, "food"}));

        List<CompatibilityMatchResponse> result = service.findMatches(request, "requester");
        assertEquals(1, result.size());
        assertEquals(30.0, result.get(0).getDateProximityScore());
    }

    @Test
    void noDestinationMatchWhenPlanDestinationNull() {
        User candidate = user(2L, "c");
        TravelPlan p = plan(2L, null,
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 6, 10, 0, 0));

        when(userRepository.findByUsername("requester")).thenReturn(Optional.of(requester));
        when(userRepository.findAll()).thenReturn(List.of(requester, candidate));
        when(travelPlanRepository.findByUserIdIn(Set.of(2L))).thenReturn(List.of(p));
        when(userInterestRepository.findUserInterestsByUserIds(Set.of(2L)))
                .thenReturn(List.<Object[]>of(new Object[]{2L, "food"}));

        List<CompatibilityMatchResponse> result = service.findMatches(request, "requester");
        assertEquals(1, result.size());
        assertEquals(0.0, result.get(0).getDestinationScore());
    }

    private User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    private TravelPlan plan(Long userId, String destination, LocalDateTime start, LocalDateTime end) {
        TravelPlan travelPlan = new TravelPlan();
        User owner = new User();
        owner.setId(userId);
        travelPlan.setUser(owner);
        travelPlan.setDestinationLocation(destination);
        travelPlan.setStartDate(start);
        travelPlan.setEndDate(end);
        return travelPlan;
    }
}
