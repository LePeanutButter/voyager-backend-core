package com.tourism.platform.service.impl;

import com.tourism.platform.dto.CreateTravelPlanActivityRequestDto;
import com.tourism.platform.dto.TravelPlanActivityDto;
import com.tourism.platform.dto.UpdateTravelPlanActivityRequestDto;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.ActivityType;
import com.tourism.platform.model.TravelPlan;
import com.tourism.platform.model.TravelPlanActivity;
import com.tourism.platform.repository.TravelPlanActivityRepository;
import com.tourism.platform.repository.TravelPlanRepository;
import com.tourism.platform.service.TravelPlanActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelPlanActivityServiceImpl implements TravelPlanActivityService {

    private final TravelPlanRepository travelPlanRepository;
    private final TravelPlanActivityRepository activityRepository;

    @Override
    @Transactional
    public TravelPlanActivityDto createActivity(Long travelPlanId, CreateTravelPlanActivityRequestDto request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());
        TravelPlan travelPlan = getTravelPlanOrThrow(travelPlanId);

        TravelPlanActivity activity = new TravelPlanActivity();
        activity.setTravelPlan(travelPlan);
        activity.setName(request.getName());
        activity.setDescription(request.getDescription());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setLocation(request.getLocation());
        activity.setType(ActivityType.SIGHTSEEING);
        activity.setIsConfirmed(false);

        TravelPlanActivity saved = activityRepository.save(activity);
        return toDto(saved);
    }

    @Override
    @Transactional
    public TravelPlanActivityDto updateActivity(Long travelPlanId, Long activityId, UpdateTravelPlanActivityRequestDto request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());
        getTravelPlanOrThrow(travelPlanId);

        TravelPlanActivity activity = activityRepository.findByIdAndTravelPlanId(activityId, travelPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with ID: " + activityId));

        activity.setName(request.getName());
        activity.setDescription(request.getDescription());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setLocation(request.getLocation());

        TravelPlanActivity saved = activityRepository.save(activity);
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TravelPlanActivityDto> getActivities(Long travelPlanId) {
        getTravelPlanOrThrow(travelPlanId);
        return activityRepository.findByTravelPlanIdOrderByStartTimeAsc(travelPlanId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteActivity(Long travelPlanId, Long activityId) {
        getTravelPlanOrThrow(travelPlanId);

        TravelPlanActivity activity = activityRepository.findByIdAndTravelPlanId(activityId, travelPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with ID: " + activityId));

        activityRepository.delete(activity);
    }

    private void validateTimeRange(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must be greater than or equal to startTime");
        }
    }

    private TravelPlan getTravelPlanOrThrow(Long travelPlanId) {
        return travelPlanRepository.findById(travelPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Travel plan not found with ID: " + travelPlanId));
    }

    private TravelPlanActivityDto toDto(TravelPlanActivity activity) {
        return TravelPlanActivityDto.builder()
                .id(activity.getId())
                .name(activity.getName())
                .description(activity.getDescription())
                .type(activity.getType())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .location(activity.getLocation())
                .estimatedCost(activity.getEstimatedCost())
                .actualCost(activity.getActualCost())
                .bookingReference(activity.getBookingReference())
                .isConfirmed(activity.getIsConfirmed())
                .notes(activity.getNotes())
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }
}
