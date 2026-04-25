package com.tourism.platform.service.impl;

import com.tourism.platform.dto.MatchResponseDto;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.model.TravelPlan;
import com.tourism.platform.model.TravelPlanStatus;
import com.tourism.platform.repository.TravelPlanRepository;
import com.tourism.platform.repository.UserInterestRepository;
import com.tourism.platform.service.MatchingService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(MatchingServiceImpl.class);
    private static final String STATUS_SUCCESS = "SUCCESS";

    private static final int DESTINATION_WEIGHT = 50;
    private static final int DATE_WEIGHT = 30;
    private static final int INTEREST_WEIGHT = 20;

    private final TravelPlanRepository travelPlanRepository;
    private final UserInterestRepository userInterestRepository;
    private final MeterRegistry meterRegistry;

    @Override
    public List<MatchResponseDto> getMatches(String destination,
                                             LocalDate startDate,
                                             LocalDate endDate,
                                             List<String> interests) {
        Timer.Sample sample = Timer.start(meterRegistry);
        log.info("event=matching_start");
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("startDate must be before or equal to endDate");
        }

        List<TravelPlan> candidates = travelPlanRepository.findAll().stream()
                .filter(plan -> plan.getStatus() == TravelPlanStatus.ACTIVE)
                .filter(plan -> plan.getStartDate() != null && plan.getEndDate() != null)
                .filter(plan -> !plan.getStartDate().isAfter(endDate.atTime(23, 59, 59)))
                .filter(plan -> !plan.getEndDate().isBefore(startDate.atStartOfDay()))
                .toList();

        List<String> normalizedInterests = interests == null ? List.of() : interests.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(String::toLowerCase)
                .toList();

        Set<Long> userIds = candidates.stream().map(plan -> plan.getUser().getId()).collect(Collectors.toSet());
        Map<Long, Set<String>> userInterestsMap = buildInterestsByUserId(userIds);
        Map<Long, MatchResponseDto> bestPerUser = buildBestMatches(candidates, destination, startDate, endDate, normalizedInterests, userInterestsMap);

        Collection<MatchResponseDto> filteredMatches = bestPerUser.values();
        if (!normalizedInterests.isEmpty()) {
            // Filtering mode is ANY: users are kept when at least one requested interest matches.
            filteredMatches = filteredMatches.stream()
                    .filter(match -> match.getInterestPoints() > 0)
                    .toList();
        }

        List<MatchResponseDto> result = filteredMatches.stream()
                .sorted(Comparator
                        .comparing(MatchResponseDto::getScore).reversed()
                        .thenComparing(MatchResponseDto::getUserId))
                .toList();
        sample.stop(Timer.builder("matching_execution_time")
                .description("Matching execution time")
                .register(meterRegistry));
        log.info("event=matching_end resultCount={} status={}", result.size(), STATUS_SUCCESS);
        return result;
    }

    private MatchResponseDto buildMatch(TravelPlan plan,
                                        String requestedDestination,
                                        LocalDate requestedStartDate,
                                        LocalDate requestedEndDate,
                                        List<String> normalizedInterests,
                                        Set<String> userInterests) {
        int destinationPoints = computeDestinationPoints(requestedDestination, plan.getDestinationLocation());
        int datePoints = computeDatePoints(
                requestedStartDate.atStartOfDay(),
                requestedEndDate.atTime(23, 59, 59),
                plan.getStartDate(),
                plan.getEndDate()
        );
        int interestPoints = computeInterestPoints(normalizedInterests, userInterests);

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

    private Map<Long, Set<String>> buildInterestsByUserId(Set<Long> userIds) {
        Map<Long, Set<String>> interestsByUserId = new HashMap<>();
        if (userIds.isEmpty()) {
            return interestsByUserId;
        }
        for (Object[] row : userInterestRepository.findUserInterestsByUserIds(userIds)) {
            Long userId = (Long) row[0];
            String interest = row[1] != null ? row[1].toString() : null;
            if (interest != null && !interest.isBlank()) {
                interestsByUserId.computeIfAbsent(userId, key -> new HashSet<>()).add(interest.trim());
            }
        }
        return interestsByUserId;
    }

    private Map<Long, MatchResponseDto> buildBestMatches(List<TravelPlan> candidates,
                                                         String destination,
                                                         LocalDate startDate,
                                                         LocalDate endDate,
                                                         List<String> normalizedInterests,
                                                         Map<Long, Set<String>> userInterestsMap) {
        Map<Long, MatchResponseDto> bestPerUser = new HashMap<>();
        for (TravelPlan plan : candidates) {
            MatchResponseDto candidate = buildMatch(
                    plan, destination, startDate, endDate, normalizedInterests,
                    userInterestsMap.getOrDefault(plan.getUser().getId(), Set.of()));
            updateBestPerUser(bestPerUser, candidate);
        }
        return bestPerUser;
    }

    private void updateBestPerUser(Map<Long, MatchResponseDto> bestPerUser, MatchResponseDto candidate) {
        if (candidate.getScore() > 0.0d) {
            MatchResponseDto currentBest = bestPerUser.get(candidate.getUserId());
            if (currentBest == null || candidate.getScore() > currentBest.getScore()) {
                bestPerUser.put(candidate.getUserId(), candidate);
            }
        }
    }
}
