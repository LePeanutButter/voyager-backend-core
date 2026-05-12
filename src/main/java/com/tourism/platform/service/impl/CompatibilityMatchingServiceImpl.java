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
        /**
         * Find compatible users for the requester based on destination, dates and interests.
         *
         * @param request           compatibility match request containing criteria
         * @param requesterUsername username of the requesting user (used to exclude self)
         * @return list of CompatibilityMatchResponse ordered by total score descending
         * @throws BadRequestException if provided date range is invalid
         * @throws ResourceNotFoundException if the requester user cannot be found
         */
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

        List<CompatibilityMatchResponse> matches = new ArrayList<>(candidates.stream()
                .map(candidate -> computeScore(
                        candidate,
                        request,
                        normalizedInputInterests,
                        travelPlansByUserId.getOrDefault(candidate.getId(), List.of()),
                        interestsByUserId.getOrDefault(candidate.getId(), Set.of())
                ))
                .filter(score -> shouldIncludeScore(score, normalizedInputInterests))
                .toList());

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
        /**
         * Compute a composite compatibility score for a single candidate user based
         * on destination match, date proximity and interest similarity.
         */

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
        /**
         * Check whether any of the candidate's travel plans match the requested destination.
         */
        return plans.stream().anyMatch(plan ->
                plan.getDestinationLocation() != null
                        && plan.getDestinationLocation().equalsIgnoreCase(destination)
        );
    }

    private double computeDateProximityScore(List<TravelPlan> plans, LocalDate requestStart, LocalDate requestEnd) {
        /**
         * Compute a date proximity score by finding the best overlap between the
         * request range and any of the candidate's travel plans.
         */
        long requestedSpan = Math.max(1L, requestEnd.toEpochDay() - requestStart.toEpochDay() + 1L);
        double bestScore = 0.0;

        for (TravelPlan plan : plans) {
            double planScore = scorePlanOverlap(plan, requestStart, requestEnd, requestedSpan);
            bestScore = Math.max(bestScore, planScore);
        }

        return round(bestScore);
    }

    private void validateRequest(CompatibilityMatchRequest request) {
        /**
         * Validate the incoming request payload for basic consistency.
         */
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("startDate must be before or equal to endDate");
        }
    }

    private Map<Long, Set<String>> buildInterestsByUserId(Set<Long> candidateIds) {
        /**
         * Load user interests for the provided candidate ids and normalize them.
         */
        if (candidateIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Set<String>> result = new HashMap<>();
        for (Object[] row : userInterestRepository.findUserInterestsByUserIds(candidateIds)) {
            Long userId = (Long) row[0];
            String interest = row[1] != null ? row[1].toString() : null;
            if (interest != null && !interest.isBlank()) {
                result.computeIfAbsent(userId, ignored -> new TreeSet<>()).add(interest.trim().toLowerCase());
            }
        }
        return result;
    }

    private double jaccardSimilarity(Set<String> first, Set<String> second) {
        /**
         * Compute Jaccard similarity between two sets of interests.
         */
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
        /**
         * Normalize a list of interest strings to a lower-case, trimmed set.
         */
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
        /**
         * Round a double to two decimal places.
         */
        return Math.round(value * 100.0) / 100.0;
    }

    private boolean shouldIncludeScore(CompatibilityMatchResponse score, Set<String> normalizedInputInterests) {
        /**
         * Decide whether a computed score should be included in the final results
         * based on totalScore and whether interest filtering is requested.
         */
        if (score.getTotalScore() <= 0.0) {
            return false;
        }
        return normalizedInputInterests.isEmpty() || !score.getMatchedInterests().isEmpty();
    }

    private double scorePlanOverlap(TravelPlan plan, LocalDate requestStart, LocalDate requestEnd, long requestedSpan) {
        /**
         * Score the overlap of a candidate plan with the requested date range.
         */
        if (plan.getStartDate() == null || plan.getEndDate() == null) {
            return 0.0;
        }
        LocalDate candidateStart = plan.getStartDate().toLocalDate();
        LocalDate candidateEnd = plan.getEndDate().toLocalDate();

        LocalDate overlapStart = candidateStart.isAfter(requestStart) ? candidateStart : requestStart;
        LocalDate overlapEnd = candidateEnd.isBefore(requestEnd) ? candidateEnd : requestEnd;
        if (overlapStart.isAfter(overlapEnd)) {
            return 0.0;
        }

        long overlapDays = overlapEnd.toEpochDay() - overlapStart.toEpochDay() + 1L;
        double overlapRatio = overlapDays / (double) requestedSpan;
        return DATE_WEIGHT * overlapRatio;
    }
}
