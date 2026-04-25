package com.tourism.platform.service;

import com.tourism.platform.dto.CreateTravelPlanActivityRequestDto;
import com.tourism.platform.dto.TravelPlanActivityDto;
import com.tourism.platform.dto.UpdateTravelPlanActivityRequestDto;

import java.util.List;

public interface TravelPlanActivityService {
    TravelPlanActivityDto createActivity(Long travelPlanId, CreateTravelPlanActivityRequestDto request);
    TravelPlanActivityDto updateActivity(Long travelPlanId, Long activityId, UpdateTravelPlanActivityRequestDto request);
    List<TravelPlanActivityDto> getActivities(Long travelPlanId);
}
