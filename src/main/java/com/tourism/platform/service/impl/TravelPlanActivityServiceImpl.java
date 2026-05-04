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
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TravelPlanActivityServiceImpl implements TravelPlanActivityService {

    private final TravelPlanRepository travelPlanRepository;
    private final TravelPlanActivityRepository activityRepository;

    @Override
    @Transactional
    /**
     * Create an activity associated with a travel plan.
     *
     * @param travelPlanId id of the travel plan
     * @param request      DTO containing activity details
     * @return TravelPlanActivityDto representing the created activity
     * @throws IllegalArgumentException if the provided time range is invalid
     * @throws ResourceNotFoundException if the travel plan does not exist
     */
    public TravelPlanActivityDto createActivity(Long travelPlanId, CreateTravelPlanActivityRequestDto request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());
        TravelPlan travelPlan = getTravelPlanOrThrow(Objects.requireNonNull(travelPlanId));

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
    /**
     * Update an existing travel plan activity.
     *
     * @param travelPlanId id of the travel plan that owns the activity
     * @param activityId   id of the activity to update
     * @param request      DTO with updated activity fields
     * @return TravelPlanActivityDto representing the updated activity
     * @throws IllegalArgumentException if the provided time range is invalid
     * @throws ResourceNotFoundException if the activity or travel plan does not exist
     */
    public TravelPlanActivityDto updateActivity(Long travelPlanId, Long activityId, UpdateTravelPlanActivityRequestDto request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());
        getTravelPlanOrThrow(Objects.requireNonNull(travelPlanId));

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
    /**
     * Retrieve activities for a travel plan ordered by start time.
     *
     * @param travelPlanId id of the travel plan
     * @return list of TravelPlanActivityDto ordered by start time
     * @throws ResourceNotFoundException if the travel plan does not exist
     */
    public List<TravelPlanActivityDto> getActivities(Long travelPlanId) {
        getTravelPlanOrThrow(Objects.requireNonNull(travelPlanId));
        return activityRepository.findByTravelPlanIdOrderByStartTimeAsc(travelPlanId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    /**
     * Delete an activity belonging to a travel plan.
     *
     * @param travelPlanId id of the travel plan
     * @param activityId   id of the activity to delete
     * @throws ResourceNotFoundException if the travel plan or activity does not exist
     */
    public void deleteActivity(Long travelPlanId, Long activityId) {
        getTravelPlanOrThrow(Objects.requireNonNull(travelPlanId));

        TravelPlanActivity activity = activityRepository.findByIdAndTravelPlanId(activityId, travelPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with ID: " + activityId));

        activityRepository.delete(Objects.requireNonNull(activity));
    }

    private void validateTimeRange(java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must be greater than or equal to startTime");
        }
    }

    private TravelPlan getTravelPlanOrThrow(@NonNull Long travelPlanId) {
        return travelPlanRepository.findById(travelPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("Travel plan not found with ID: " + travelPlanId));
    }

    private TravelPlanActivityDto toDto(TravelPlanActivity activity) {
        /**
         * Map a TravelPlanActivity entity to its DTO representation.
         *
         * @param activity entity to map
         * @return TravelPlanActivityDto containing the activity data
         */
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
