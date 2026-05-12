package com.tourism.platform.service.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tourism.platform.dto.TravelPlanDto;
import com.tourism.platform.dto.TravelerMatchDto;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.TravelPlan;
import com.tourism.platform.model.TravelPlanStatus;
import com.tourism.platform.repository.TravelPlanRepository;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.service.TravelPlanService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of TravelPlanService
 * 
 * This service provides business logic for travel plan operations
 * including traveler matching based on destination and date compatibility.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TravelPlanServiceImpl implements TravelPlanService {

    // Constants for duplicated literals
    private static final String TRAVEL_PLAN_NOT_FOUND = "Travel plan not found with id: ";

    private final TravelPlanRepository travelPlanRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    /**
     * Find travelers whose travel plans are compatible with the given travel plan.
     *
     * Compatibility is determined by destination proximity and overlapping dates.
     * The requesting user must own the reference travel plan.
     *
     * @param travelPlanId     id of the reference travel plan
     * @param requestingUserId id of the user requesting compatible travelers
     * @return list of TravelerMatchDto containing compatibility metadata for each match
     * @throws AccessDeniedException if the requesting user does not own the travel plan
     */
    public List<TravelerMatchDto> findCompatibleTravelers(Long travelPlanId, Long requestingUserId) {
        log.info("Finding compatible travelers for travel plan ID: {} by user ID: {}", travelPlanId, requestingUserId);

        // Get the reference travel plan
        TravelPlan referenceTravelPlan = getTravelPlanById(travelPlanId);
        
        // Validate that the requesting user owns this travel plan
        if (!referenceTravelPlan.getUser().getId().equals(requestingUserId)) {
            throw new AccessDeniedException("User does not own this travel plan");
        }

        // Find compatible travel plans from other users (only ACTIVE plans)
        List<TravelPlan> compatiblePlans = travelPlanRepository.findCompatibleTravelPlans(
                referenceTravelPlan.getDestinationLocation(),
                referenceTravelPlan.getStartDate(),
                referenceTravelPlan.getEndDate(),
                requestingUserId,
                TravelPlanStatus.ACTIVE
        );

        log.info("Found {} compatible travel plans", compatiblePlans.size());

        // Convert to TravelerMatchDto
        List<TravelerMatchDto> matches = compatiblePlans.stream()
                .map(plan -> createTravelerMatch(plan, referenceTravelPlan))
                .toList();

        log.info("Returning {} traveler matches", matches.size());
        return matches;
    }

    @Override
    /**
     * Calculate the number of overlapping days between two date ranges.
     *
     * The result includes both endpoints and returns 0 when ranges do not overlap.
     *
     * @param startDate1 start of the first range
     * @param endDate1   end of the first range
     * @param startDate2 start of the second range
     * @param endDate2   end of the second range
     * @return number of overlapping days (0 if none)
     */
    public Integer calculateOverlappingDays(LocalDateTime startDate1, LocalDateTime endDate1,
                                          LocalDateTime startDate2, LocalDateTime endDate2) {
        // Find the latest start date
        LocalDateTime overlapStart = startDate1.isAfter(startDate2) ? startDate1 : startDate2;
        
        // Find the earliest end date
        LocalDateTime overlapEnd = endDate1.isBefore(endDate2) ? endDate1 : endDate2;
        
        // If there's no overlap, return 0
        if (overlapStart.isAfter(overlapEnd)) {
            return 0;
        }
        
        // Calculate the number of days (rounded up to include partial days)
        return (int) ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
    }

    @Override
    public TravelPlan getTravelPlanById(Long travelPlanId) {
        /**
         * Retrieve a travel plan by its identifier.
         *
         * @param travelPlanId id of the travel plan to retrieve
         * @return TravelPlan entity when found
         * @throws EntityNotFoundException if the travel plan is not found
         */
        return travelPlanRepository.findById(Objects.requireNonNull(travelPlanId))
                .orElseThrow(() -> new EntityNotFoundException("Travel plan not found with ID: " + travelPlanId));
    }

    @Override
    public Page<TravelPlan> getTravelPlansByUser(Long userId, Pageable pageable) {
        /**
         * Retrieve travel plans for a user with pagination.
         *
         * @param userId   id of the user
         * @param pageable pagination information
         * @return page of TravelPlan entities for the user
         */
        return travelPlanRepository.findByUserId(userId, pageable);
    }

    @Override
    public Page<TravelPlan> getActiveTravelPlansByUser(Long userId, Pageable pageable) {
        /**
         * Retrieve only active travel plans for a user with pagination.
         *
         * @param userId   id of the user
         * @param pageable pagination information
         * @return page of active TravelPlan entities for the user
         */
        return travelPlanRepository.findByUserIdAndStatus(userId, TravelPlanStatus.ACTIVE, pageable);
    }

    @Override
    @Transactional
    public TravelPlanDto createTravelPlan(TravelPlanDto dto, Long userId) {
        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessException("Invalid date range: endDate cannot be before startDate");
        }

        var user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        TravelPlan plan = new TravelPlan();
        plan.setUser(user);
        plan.setTitle(dto.getTitle());
        plan.setDescription(dto.getDescription());
        plan.setDestinationLocation(dto.getDestinationLocation());
        plan.setOriginLocation(dto.getOriginLocation());
        plan.setStartDate(dto.getStartDate());
        plan.setEndDate(dto.getEndDate());
        plan.setEstimatedBudget(dto.getEstimatedBudget());
        plan.setNumberOfTravelers(dto.getNumberOfTravelers());
        if (dto.getStatus() != null) {
            plan.setStatus(dto.getStatus());
        }
        if (dto.getTravelType() != null) {
            plan.setTravelType(dto.getTravelType());
        }
        if (dto.getIsPublic() != null) {
            plan.setIsPublic(dto.getIsPublic());
        }

        TravelPlan saved = travelPlanRepository.save(plan);
        return toDto(saved);
    }

    @Override
    public List<TravelPlanDto> getTravelPlanDtosByUser(Long userId) {
        /**
         * Retrieve all travel plans for a user and map them to DTOs.
         *
         * @param userId id of the user
         * @return list of TravelPlanDto for the user
         */
        return travelPlanRepository.findByUserId(userId, org.springframework.data.domain.Pageable.unpaged())
                .map(this::toDto)
                .getContent();
    }

    @Override
    @Transactional
    public TravelPlanDto updateTravelPlan(Long travelPlanId, Long userId, TravelPlanDto dto) {
        /**
         * Update an existing travel plan's fields.
         *
         * @param travelPlanId id of the travel plan to update
         * @param userId       id of the user performing the update
         * @param dto          DTO containing updated fields
         * @return updated TravelPlanDto after persistence
         * @throws BusinessException if provided dates are invalid
         * @throws ResourceNotFoundException if the travel plan does not exist
         * @throws AccessDeniedException if the user is not allowed to update the plan
         */
        if (dto.getStartDate() != null && dto.getEndDate() != null && dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessException("Invalid date range: endDate cannot be before startDate");
        }

        TravelPlan existing = travelPlanRepository.findById(Objects.requireNonNull(travelPlanId))
                .orElseThrow(() -> new ResourceNotFoundException(TRAVEL_PLAN_NOT_FOUND + travelPlanId));

        if (!existing.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You are not allowed to update this travel plan");
        }

        existing.setTitle(dto.getTitle());
        existing.setDescription(dto.getDescription());
        existing.setDestinationLocation(dto.getDestinationLocation());
        existing.setOriginLocation(dto.getOriginLocation());
        existing.setStartDate(dto.getStartDate());
        existing.setEndDate(dto.getEndDate());
        existing.setEstimatedBudget(dto.getEstimatedBudget());
        existing.setActualCost(dto.getActualCost());
        existing.setNumberOfTravelers(dto.getNumberOfTravelers());
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        if (dto.getTravelType() != null) {
            existing.setTravelType(dto.getTravelType());
        }
        if (dto.getIsPublic() != null) {
            existing.setIsPublic(dto.getIsPublic());
        }

        TravelPlan saved = travelPlanRepository.save(existing);
        return toDto(saved);
    }

    @Override
    @Transactional
    public void deleteTravelPlan(Long travelPlanId, Long userId) {
        /**
         * Delete a travel plan if the requesting user is the owner.
         *
         * @param travelPlanId id of the travel plan to delete
         * @param userId       id of the user requesting deletion
         * @throws ResourceNotFoundException if the travel plan does not exist
         * @throws AccessDeniedException if the user does not own the travel plan
         */
        TravelPlan existing = travelPlanRepository.findById(Objects.requireNonNull(travelPlanId))
                .orElseThrow(() -> new ResourceNotFoundException(TRAVEL_PLAN_NOT_FOUND + travelPlanId));

        if (!existing.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You are not allowed to delete this travel plan");
        }

        travelPlanRepository.delete(existing);
    }

    @Override
    @Transactional
    public TravelPlanDto updateTravelPlanStatus(Long travelPlanId, Long userId, TravelPlanStatus status) {
        /**
         * Update only the status of an existing travel plan.
         *
         * @param travelPlanId id of the travel plan to update
         * @param userId       id of the user performing the update
         * @param status       new TravelPlanStatus to set
         * @return TravelPlanDto representing the travel plan after the status update
         * @throws ResourceNotFoundException if the travel plan does not exist
         * @throws AccessDeniedException if the user is not the owner of the travel plan
         */
        TravelPlan existing = travelPlanRepository.findById(Objects.requireNonNull(travelPlanId))
                .orElseThrow(() -> new ResourceNotFoundException(TRAVEL_PLAN_NOT_FOUND + travelPlanId));
        if (!existing.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You are not allowed to update this travel plan");
        }
        existing.setStatus(status);
        return toDto(travelPlanRepository.save(existing));
    }

    private TravelPlanDto toDto(TravelPlan plan) {
        /**
         * Map a TravelPlan entity to its corresponding DTO representation.
         *
         * @param plan TravelPlan entity to map
         * @return TravelPlanDto containing selected fields from the entity
         */
        return TravelPlanDto.builder()
                .id(plan.getId())
                .title(plan.getTitle())
                .description(plan.getDescription())
                .status(plan.getStatus())
                .travelType(plan.getTravelType())
                .startDate(plan.getStartDate())
                .endDate(plan.getEndDate())
                .estimatedBudget(plan.getEstimatedBudget())
                .actualCost(plan.getActualCost())
                .numberOfTravelers(plan.getNumberOfTravelers())
                .originLocation(plan.getOriginLocation())
                .destinationLocation(plan.getDestinationLocation())
                .isPublic(plan.getIsPublic())
                .shareToken(plan.getShareToken())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }

    /**
     * Creates a TravelerMatchDto from a compatible travel plan
     * 
     * @param compatiblePlan the compatible travel plan from another user
     * @param referencePlan the reference travel plan for comparison
     * @return TravelerMatchDto with compatibility information
     */
    private TravelerMatchDto createTravelerMatch(TravelPlan compatiblePlan, TravelPlan referencePlan) {
        Integer overlappingDays = calculateOverlappingDays(
                referencePlan.getStartDate(), referencePlan.getEndDate(),
                compatiblePlan.getStartDate(), compatiblePlan.getEndDate()
        );

        // Calculate a simple compatibility score based on overlap and traveler count
        Double compatibilityScore = calculateCompatibilityScore(overlappingDays, 
                referencePlan.getNumberOfTravelers(), compatiblePlan.getNumberOfTravelers());

        return TravelerMatchDto.builder()
                .userId(compatiblePlan.getUser().getId())
                .username(compatiblePlan.getUser().getUsername())
                .firstName(compatiblePlan.getUser().getFirstName())
                .lastName(compatiblePlan.getUser().getLastName())
                .profileImageUrl(compatiblePlan.getUser().getProfileImageUrl())
                .bio(compatiblePlan.getUser().getBio())
                .travelPlanId(compatiblePlan.getId())
                .travelPlanTitle(compatiblePlan.getTitle())
                .destinationLocation(compatiblePlan.getDestinationLocation())
                .travelStartDate(compatiblePlan.getStartDate())
                .travelEndDate(compatiblePlan.getEndDate())
                .numberOfTravelers(compatiblePlan.getNumberOfTravelers())
                .daysOverlap(overlappingDays)
                .compatibilityScore(compatibilityScore)
                .build();
    }

    /**
     * Calculates a compatibility score between two travel plans
     * 
     * @param overlappingDays number of overlapping days
     * @param referenceTravelers number of travelers in reference plan
     * @param compatibleTravelers number of travelers in compatible plan
     * @return compatibility score (0.0 to 1.0)
     */
    private Double calculateCompatibilityScore(Integer overlappingDays, 
                                             Integer referenceTravelers, 
                                             Integer compatibleTravelers) {
        // Base score from overlapping days (max 30 days = 1.0)
        double daysScore = Math.min(overlappingDays / 30.0, 1.0);
        
        // Traveler group size compatibility (prefer similar group sizes)
        double groupSizeScore = 1.0 - Math.abs(referenceTravelers - compatibleTravelers) / 10.0;
        groupSizeScore = Math.max(groupSizeScore, 0.0);
        
        // Weighted average (70% days overlap, 30% group size compatibility)
        return (daysScore * 0.7) + (groupSizeScore * 0.3);
    }
}
