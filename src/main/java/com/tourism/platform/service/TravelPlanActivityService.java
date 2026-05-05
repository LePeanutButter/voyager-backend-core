package com.tourism.platform.service;

import java.util.List;

import com.tourism.platform.dto.CreateTravelPlanActivityRequestDto;
import com.tourism.platform.dto.TravelPlanActivityDto;
import com.tourism.platform.dto.UpdateTravelPlanActivityRequestDto;

public interface TravelPlanActivityService {

    /**
     * Create an activity for a travel plan.
     *
     * @param travelPlanId id of the travel plan to which the activity belongs
     * @param request      DTO containing activity creation data
     * @return TravelPlanActivityDto representing the created activity
     */
    TravelPlanActivityDto createActivity(Long travelPlanId, CreateTravelPlanActivityRequestDto request);

    /**
     * Update an existing activity for a travel plan.
     *
     * @param travelPlanId id of the travel plan that owns the activity
     * @param activityId   id of the activity to update
     * @param request      DTO containing the fields to update
     * @return TravelPlanActivityDto representing the updated activity
     */
    TravelPlanActivityDto updateActivity(Long travelPlanId, Long activityId, UpdateTravelPlanActivityRequestDto request);

    /**
     * Retrieve all activities associated with a travel plan.
     *
     * @param travelPlanId id of the travel plan
     * @return list of TravelPlanActivityDto for the travel plan
     */
    List<TravelPlanActivityDto> getActivities(Long travelPlanId);

    /**
     * Delete an activity from a travel plan.
     *
     * @param travelPlanId id of the travel plan
     * @param activityId   id of the activity to delete
     */
    void deleteActivity(Long travelPlanId, Long activityId);
}
