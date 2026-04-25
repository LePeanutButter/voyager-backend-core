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
import com.tourism.platform.service.CompatibilityMatchingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CompatibilityMatchingServiceImpl implements CompatibilityMatchingService {

    private static final double DESTINATION_WEIGHT = 50.0;
    private static final double DATE_WEIGHT = 30.0;
    private static final double INTEREST_WEIGHT = 20.0;

    private final UserRepository userRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final UserInterestRepository userInterestRepository;

    public CompatibilityMatchingServiceImpl(
            UserRepository userRepository,
            TravelPlanRepository travelPlanRepository,
            UserInterestRepository userInterestRepository) {
        this.userRepository = userRepository;
        this.travelPlanRepository = travelPlanRepository;
        this.userInterestRepository = userInterestRepository;
    }

    @Override
    public List<CompatibilityMatchResponse> findMatches(CompatibilityMatchRequest request, String requesterUsername) {
        validateRequest(request);

        User requester = userRepository.findByUsername(requesterUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Requester not found"));

        List<User> candidates = userRepository.findAll().stream()
                .filter(user -> !user.getId().equals(requester.getId()))
                .toList();
        Set<Long> candidateIds = candidates.stream().map(User::getId).collect(Collectors.toSet());

        Set<String> normalizedInputInterests = normalizeInterests(request.getInterests());
        Map<Long, List<TravelPlan>> travelPlansByUserId = travelPlanRepository.findByUserIdIn(candidateIds)
                .stream()
                .collect(Collectors.groupingBy(plan -> plan.getUser().getId()));
        Map<Long, Set<String>> interestsByUserId = buildInterestsByUserId(candidateIds);

        List<CompatibilityMatchResponse> matches = new ArrayList<>();
        for (User candidate : candidates) {
            CompatibilityMatchResponse score = computeScore(
                    candidate,
                    request,
                    normalizedInputInterests,
                    travelPlansByUserId.getOrDefault(candidate.getId(), List.of()),
                    interestsByUserId.getOrDefault(candidate.getId(), Set.of())
            );

            if (!normalizedInputInterests.isEmpty() && score.getMatchedInterests().isEmpty()) {
                continue;
            }

            if (score.getTotalScore() > 0.0) {
                matches.add(score);
            }
        }

        matches.sort(Comparator
                .comparingDouble(CompatibilityMatchResponse::getTotalScore).reversed()
                .thenComparing(CompatibilityMatchResponse::getUserId));

        return matches;
    }

    private CompatibilityMatchResponse computeScore(
            User candidate,
            CompatibilityMatchRequest request,
            Set<String> normalizedInputInterests,
            List<TravelPlan> plans,
            Set<String> candidateInterests) {

        double destinationScore = hasDestinationMatch(plans, request.getDestination()) ? DESTINATION_WEIGHT : 0.0;
        double dateScore = computeDateProximityScore(plans, request.getStartDate(), request.getEndDate());

        List<String> matchedInterests = candidateInterests.stream()
                .filter(normalizedInputInterests::contains)
                .sorted()
                .toList();

        double interestScore = normalizedInputInterests.isEmpty()
                ? 0.0
                : INTEREST_WEIGHT * jaccardSimilarity(candidateInterests, normalizedInputInterests);
        interestScore = round(Math.min(INTEREST_WEIGHT, interestScore));

        CompatibilityMatchResponse response = new CompatibilityMatchResponse();
        response.setUserId(candidate.getId());
        response.setDestinationScore(destinationScore);
        response.setDateProximityScore(dateScore);
        response.setInterestScore(interestScore);
        response.setMatchedInterests(matchedInterests);
        response.setTotalScore(round(destinationScore + dateScore + interestScore));
        return response;
    }

    private boolean hasDestinationMatch(List<TravelPlan> plans, String destination) {
        return plans.stream().anyMatch(plan ->
                plan.getDestinationLocation() != null
                        && plan.getDestinationLocation().equalsIgnoreCase(destination)
        );
    }

    private double computeDateProximityScore(List<TravelPlan> plans, LocalDate requestStart, LocalDate requestEnd) {
        long requestedSpan = Math.max(1L, requestEnd.toEpochDay() - requestStart.toEpochDay() + 1L);
        double bestScore = 0.0;

        for (TravelPlan plan : plans) {
            if (plan.getStartDate() == null || plan.getEndDate() == null) {
                continue;
            }
            LocalDate candidateStart = plan.getStartDate().toLocalDate();
            LocalDate candidateEnd = plan.getEndDate().toLocalDate();

            LocalDate overlapStart = candidateStart.isAfter(requestStart) ? candidateStart : requestStart;
            LocalDate overlapEnd = candidateEnd.isBefore(requestEnd) ? candidateEnd : requestEnd;
            if (overlapStart.isAfter(overlapEnd)) {
                continue;
            }

            long overlapDays = overlapEnd.toEpochDay() - overlapStart.toEpochDay() + 1L;
            double overlapRatio = overlapDays / (double) requestedSpan;
            bestScore = Math.max(bestScore, DATE_WEIGHT * overlapRatio);
        }

        return round(bestScore);
    }

    private void validateRequest(CompatibilityMatchRequest request) {
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("startDate must be before or equal to endDate");
        }
    }

    private Map<Long, Set<String>> buildInterestsByUserId(Set<Long> candidateIds) {
        if (candidateIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<String>> result = new HashMap<>();
        for (Object[] row : userInterestRepository.findUserInterestsByUserIds(candidateIds)) {
            Long userId = (Long) row[0];
            String interest = row[1] != null ? row[1].toString() : null;
            if (interest == null || interest.isBlank()) {
                continue;
            }
            result.computeIfAbsent(userId, ignored -> new TreeSet<>()).add(interest.trim().toLowerCase());
        }
        return result;
    }

    private double jaccardSimilarity(Set<String> first, Set<String> second) {
        if (first.isEmpty() && second.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(first);
        intersection.retainAll(second);

        Set<String> union = new HashSet<>(first);
        union.addAll(second);
        if (union.isEmpty()) {
            return 0.0;
        }
        return intersection.size() / (double) union.size();
    }

    private Set<String> normalizeInterests(List<String> interests) {
        if (interests == null) {
            return Set.of();
        }
        return interests.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
