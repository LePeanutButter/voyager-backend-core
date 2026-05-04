package com.tourism.platform.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tourism.platform.dto.TravelPlanDto;
import com.tourism.platform.dto.TravelerMatchDto;
import com.tourism.platform.model.TravelPlan;
import com.tourism.platform.model.TravelPlanStatus;

/**
 * Service interface for TravelPlan business logic
 * 
 * This interface defines the contract for travel plan operations
 * including traveler matching and compatibility searches.
 */
public interface TravelPlanService {

    /**
     * Find travelers with compatible destinations and dates
     * 
     * @param travelPlanId the ID of the reference travel plan
     * @param requestingUserId the ID of the user making the request
     * @return List of traveler matches with compatibility information
     */
    List<TravelerMatchDto> findCompatibleTravelers(Long travelPlanId, Long requestingUserId);

    /**
     * Calculate the number of overlapping days between two date ranges
     * 
     * @param startDate1 start date of first range
     * @param endDate1 end date of first range
     * @param startDate2 start date of second range
     * @param endDate2 end date of second range
     * @return number of overlapping days
     */
    Integer calculateOverlappingDays(java.time.LocalDateTime startDate1, 
                                   java.time.LocalDateTime endDate1,
                                   java.time.LocalDateTime startDate2, 
                                   java.time.LocalDateTime endDate2);

    /**
     * Get a travel plan by ID
     * 
     * @param travelPlanId the travel plan ID
     * @return the travel plan if found
     * @throws jakarta.persistence.EntityNotFoundException if not found
     */
    TravelPlan getTravelPlanById(Long travelPlanId);

    /**
     * Get travel plans for a specific user
     * 
     * @param userId the user ID
     * @param pageable pagination information
     * @return Page of travel plans for the user
     */
    Page<TravelPlan> getTravelPlansByUser(Long userId, Pageable pageable);

    /**
     * Get active travel plans for a specific user
     * 
     * @param userId the user ID
     * @param pageable pagination information
     * @return Page of active travel plans for the user
     */
    Page<TravelPlan> getActiveTravelPlansByUser(Long userId, Pageable pageable);
    /**
     * Create a new travel plan for a user.
     *
     * @param dto    travel plan data transfer object containing plan details
     * @param userId id of the user creating the travel plan
     * @return created TravelPlanDto representing the persisted plan
     */
    TravelPlanDto createTravelPlan(TravelPlanDto dto, Long userId);

    /**
     * Retrieve all travel plan DTOs for a given user.
     *
     * @param userId id of the user whose plans should be retrieved
     * @return list of TravelPlanDto belonging to the user
     */
    List<TravelPlanDto> getTravelPlanDtosByUser(Long userId);

    /**
     * Update an existing travel plan.
     *
     * @param travelPlanId id of the travel plan to update
     * @param userId       id of the user requesting the update
     * @param dto          DTO containing updated travel plan fields
     * @return updated TravelPlanDto after persistence
     */
    TravelPlanDto updateTravelPlan(Long travelPlanId, Long userId, TravelPlanDto dto);

    /**
     * Delete a travel plan.
     *
     * @param travelPlanId id of the travel plan to delete
     * @param userId       id of the user requesting deletion
     */
    void deleteTravelPlan(Long travelPlanId, Long userId);

    /**
     * Update only the status field of an existing travel plan and persist it.
     *
     * @param travelPlanId id of the travel plan to update
     * @param userId       id of the user performing the update
     * @param status       new TravelPlanStatus to apply
     * @return TravelPlanDto representing the travel plan after the status change
     */
    TravelPlanDto updateTravelPlanStatus(Long travelPlanId, Long userId, TravelPlanStatus status);
}
