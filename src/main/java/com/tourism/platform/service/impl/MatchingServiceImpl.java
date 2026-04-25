package com.tourism.platform.service.impl;

import com.tourism.platform.dto.MatchResponseDto;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.model.TravelPlan;
import com.tourism.platform.model.TravelPlanStatus;
import com.tourism.platform.repository.TravelPlanRepository;
import com.tourism.platform.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchingServiceImpl implements MatchingService {

    private static final int DESTINATION_WEIGHT = 50;
    private static final int DATE_WEIGHT = 30;
    private static final int INTEREST_WEIGHT = 20;

    private final TravelPlanRepository travelPlanRepository;

    @Override
    public List<MatchResponseDto> getMatches(String destination,
                                             LocalDate startDate,
                                             LocalDate endDate,
                                             List<String> interests) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("startDate must be before or equal to endDate");
        }

        List<TravelPlan> candidates = travelPlanRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        TravelPlanStatus.ACTIVE,
                        endDate.atTime(23, 59, 59),
                        startDate.atStartOfDay()
                );

        List<String> normalizedInterests = interests == null ? List.of() : interests.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(String::toLowerCase)
                .toList();

        Map<Long, MatchResponseDto> bestPerUser = new HashMap<>();
        for (TravelPlan plan : candidates) {
            MatchResponseDto candidate = buildMatch(plan, destination, startDate, endDate, normalizedInterests);
            if (candidate.getScore() <= 0.0d) {
                continue;
            }

            MatchResponseDto currentBest = bestPerUser.get(candidate.getUserId());
            if (currentBest == null || candidate.getScore() > currentBest.getScore()) {
                bestPerUser.put(candidate.getUserId(), candidate);
            }
        }

        Collection<MatchResponseDto> filteredMatches = bestPerUser.values();
        if (!normalizedInterests.isEmpty()) {
            // Filtering mode is ANY: users are kept when at least one requested interest matches.
            filteredMatches = filteredMatches.stream()
                    .filter(match -> match.getInterestPoints() > 0)
                    .collect(Collectors.toList());
        }

        return filteredMatches.stream()
                .sorted(Comparator
                        .comparing(MatchResponseDto::getScore).reversed()
                        .thenComparing(MatchResponseDto::getUserId))
                .toList();
    }

    private MatchResponseDto buildMatch(TravelPlan plan,
                                        String requestedDestination,
                                        LocalDate requestedStartDate,
                                        LocalDate requestedEndDate,
                                        List<String> normalizedInterests) {
        int destinationPoints = computeDestinationPoints(requestedDestination, plan.getDestinationLocation());
        int datePoints = computeDatePoints(
                requestedStartDate.atStartOfDay(),
                requestedEndDate.atTime(23, 59, 59),
                plan.getStartDate(),
                plan.getEndDate()
        );
        int interestPoints = computeInterestPoints(normalizedInterests, plan.getUser().getInterests());

        double totalScore = destinationPoints + datePoints + interestPoints;

        return MatchResponseDto.builder()
                .userId(plan.getUser().getId())
                .username(plan.getUser().getUsername())
                .destination(plan.getDestinationLocation())
                .score(totalScore)
                .destinationPoints(destinationPoints)
                .datePoints(datePoints)
                .interestPoints(interestPoints)
                .build();
    }

    private int computeDestinationPoints(String requestedDestination, String planDestination) {
        if (requestedDestination == null || planDestination == null) {
            return 0;
        }
        return requestedDestination.trim().equalsIgnoreCase(planDestination.trim()) ? DESTINATION_WEIGHT : 0;
    }

    private int computeDatePoints(LocalDateTime requestedStart,
                                  LocalDateTime requestedEnd,
                                  LocalDateTime candidateStart,
                                  LocalDateTime candidateEnd) {
        LocalDateTime overlapStart = requestedStart.isAfter(candidateStart) ? requestedStart : candidateStart;
        LocalDateTime overlapEnd = requestedEnd.isBefore(candidateEnd) ? requestedEnd : candidateEnd;
        if (overlapStart.isAfter(overlapEnd)) {
            return 0;
        }

        long overlapDays = ChronoUnit.DAYS.between(overlapStart.toLocalDate(), overlapEnd.toLocalDate()) + 1;
        long requestedDays = ChronoUnit.DAYS.between(requestedStart.toLocalDate(), requestedEnd.toLocalDate()) + 1;
        double overlapRatio = requestedDays <= 0 ? 0.0d : (double) overlapDays / requestedDays;

        return (int) Math.round(Math.min(1.0d, overlapRatio) * DATE_WEIGHT);
    }

    private int computeInterestPoints(List<String> normalizedInterests, Set<String> userInterests) {
        if (normalizedInterests.isEmpty() || userInterests == null || userInterests.isEmpty()) {
            return 0;
        }

        Set<String> normalizedUserInterests = userInterests.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        long matches = normalizedInterests.stream()
                .filter(normalizedUserInterests::contains)
                .count();

        if (matches == 0) {
            return 0;
        }

        double ratio = (double) matches / normalizedInterests.size();
        return (int) Math.round(ratio * INTEREST_WEIGHT);
    }
}
