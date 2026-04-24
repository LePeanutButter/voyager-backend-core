package com.tourism.platform.service;

import com.tourism.platform.dto.TravelerMatchDto;
import com.tourism.platform.dto.TravelPlanDto;
import com.tourism.platform.model.TravelPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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

    TravelPlanDto createTravelPlan(TravelPlanDto dto, Long userId);

    List<TravelPlanDto> getTravelPlanDtosByUser(Long userId);
}
