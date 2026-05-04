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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelPlanActivityServiceImplTest {

    @Mock
    private TravelPlanRepository travelPlanRepository;

    @Mock
    private TravelPlanActivityRepository activityRepository;

    @InjectMocks
    private TravelPlanActivityServiceImpl service;

    private TravelPlan travelPlan;
    private CreateTravelPlanActivityRequestDto createRequest;
    private UpdateTravelPlanActivityRequestDto updateRequest;
    private TravelPlanActivity activity;

    @BeforeEach
    void setUp() {
        travelPlan = new TravelPlan();
        travelPlan.setId(1L);

        createRequest = new CreateTravelPlanActivityRequestDto();
        createRequest.setName("Visit Eiffel Tower");
        createRequest.setDescription("Tour the iconic landmark");
        createRequest.setStartTime(LocalDateTime.of(2024, 6, 1, 10, 0));
        createRequest.setEndTime(LocalDateTime.of(2024, 6, 1, 12, 0));
        createRequest.setLocation("Paris, France");

        updateRequest = new UpdateTravelPlanActivityRequestDto();
        updateRequest.setName("Updated Activity");
        updateRequest.setDescription("Updated description");
        updateRequest.setStartTime(LocalDateTime.of(2024, 6, 1, 14, 0));
        updateRequest.setEndTime(LocalDateTime.of(2024, 6, 1, 16, 0));
        updateRequest.setLocation("Updated Location");

        activity = new TravelPlanActivity();
        activity.setId(1L);
        activity.setTravelPlan(travelPlan);
        activity.setName("Visit Eiffel Tower");
        activity.setDescription("Tour the iconic landmark");
        activity.setStartTime(LocalDateTime.of(2024, 6, 1, 10, 0));
        activity.setEndTime(LocalDateTime.of(2024, 6, 1, 12, 0));
        activity.setLocation("Paris, France");
        activity.setType(ActivityType.SIGHTSEEING);
        activity.setIsConfirmed(false);
    }

    @Test
    void createActivity_ShouldReturnActivityDto_WhenValidRequest() {
        when(travelPlanRepository.findById(1L)).thenReturn(Optional.of(travelPlan));
        when(activityRepository.save(Objects.requireNonNull(any(TravelPlanActivity.class)))).thenReturn(activity);

        TravelPlanActivityDto result = service.createActivity(1L, createRequest);

        assertNotNull(result);
        assertEquals("Visit Eiffel Tower", result.getName());
        assertEquals("Tour the iconic landmark", result.getDescription());
        assertEquals(ActivityType.SIGHTSEEING, result.getType());
        assertFalse(result.getIsConfirmed());

        verify(travelPlanRepository).findById(Objects.requireNonNull(1L));
        verify(activityRepository).save(Objects.requireNonNull(any(TravelPlanActivity.class)));
    }

    @Test
    void createActivity_ShouldThrowException_WhenTravelPlanNotFound() {
        when(travelPlanRepository.findById(Objects.requireNonNull(1L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createActivity(1L, createRequest));

        verify(travelPlanRepository).findById(Objects.requireNonNull(1L));
        verify(activityRepository, never()).save(Objects.requireNonNull(any(TravelPlanActivity.class)));
    }

    @Test
    void createActivity_ShouldThrowException_WhenEndTimeBeforeStartTime() {
        createRequest.setEndTime(LocalDateTime.of(2024, 6, 1, 8, 0)); // Before start time

        assertThrows(IllegalArgumentException.class, () -> service.createActivity(1L, createRequest));

        verify(travelPlanRepository, never()).findById(Objects.requireNonNull(any()));
        verify(activityRepository, never()).save(Objects.requireNonNull(any(TravelPlanActivity.class)));
    }

    @Test
    void updateActivity_ShouldReturnUpdatedActivityDto_WhenValidRequest() {
        when(travelPlanRepository.findById(Objects.requireNonNull(1L))).thenReturn(Optional.of(travelPlan));
        when(activityRepository.findByIdAndTravelPlanId(1L, 1L)).thenReturn(Optional.of(activity));
        when(activityRepository.save(Objects.requireNonNull(any(TravelPlanActivity.class)))).thenReturn(activity);

        TravelPlanActivityDto result = service.updateActivity(1L, 1L, updateRequest);

        assertNotNull(result);
        verify(travelPlanRepository).findById(Objects.requireNonNull(1L));
        verify(activityRepository).findByIdAndTravelPlanId(Objects.requireNonNull(1L), Objects.requireNonNull(1L));
        verify(activityRepository).save(Objects.requireNonNull(activity));
    }

    @Test
    void updateActivity_ShouldThrowException_WhenActivityNotFound() {
        when(travelPlanRepository.findById(Objects.requireNonNull(1L))).thenReturn(Optional.of(travelPlan));
        when(activityRepository.findByIdAndTravelPlanId(Objects.requireNonNull(1L), Objects.requireNonNull(1L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateActivity(1L, 1L, updateRequest));

        verify(travelPlanRepository).findById(Objects.requireNonNull(1L));
        verify(activityRepository).findByIdAndTravelPlanId(Objects.requireNonNull(1L), Objects.requireNonNull(1L));
        verify(activityRepository, never()).save(Objects.requireNonNull(any(TravelPlanActivity.class)));
    }

    @Test
    void updateActivity_ShouldThrowException_WhenTravelPlanNotFound() {
        when(travelPlanRepository.findById(Objects.requireNonNull(1L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateActivity(1L, 1L, updateRequest));

        verify(travelPlanRepository).findById(Objects.requireNonNull(1L));
        verify(activityRepository, never()).findByIdAndTravelPlanId(Objects.requireNonNull(any()), Objects.requireNonNull(any()));
        verify(activityRepository, never()).save(Objects.requireNonNull(any(TravelPlanActivity.class)));
    }

    @Test
    void updateActivity_ShouldThrowException_WhenEndTimeBeforeStartTime() {
        updateRequest.setEndTime(LocalDateTime.of(2024, 6, 1, 12, 0)); // Before start time

        assertThrows(IllegalArgumentException.class, () -> service.updateActivity(1L, 1L, updateRequest));

        verify(travelPlanRepository, never()).findById(Objects.requireNonNull(any()));
        verify(activityRepository, never()).findByIdAndTravelPlanId(Objects.requireNonNull(any()), Objects.requireNonNull(any()));
        verify(activityRepository, never()).save(Objects.requireNonNull(any(TravelPlanActivity.class)));
    }

    @Test
    void getActivities_ShouldReturnActivityDtos_WhenTravelPlanExists() {
        List<TravelPlanActivity> activities = List.of(activity);
        when(travelPlanRepository.findById(Objects.requireNonNull(1L))).thenReturn(Optional.of(travelPlan));
        when(activityRepository.findByTravelPlanIdOrderByStartTimeAsc(1L)).thenReturn(activities);

        List<TravelPlanActivityDto> result = service.getActivities(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Visit Eiffel Tower", result.get(0).getName());

        verify(travelPlanRepository).findById(Objects.requireNonNull(1L));
        verify(activityRepository).findByTravelPlanIdOrderByStartTimeAsc(1L);
    }

    @Test
    void getActivities_ShouldThrowException_WhenTravelPlanNotFound() {
        when(travelPlanRepository.findById(Objects.requireNonNull(1L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getActivities(1L));

        verify(travelPlanRepository).findById(Objects.requireNonNull(1L));
        verify(activityRepository, never()).findByTravelPlanIdOrderByStartTimeAsc(any());
    }

    @Test
    void deleteActivity_ShouldDeleteActivity_WhenActivityExists() {
        when(travelPlanRepository.findById(Objects.requireNonNull(1L))).thenReturn(Optional.of(travelPlan));
        when(activityRepository.findByIdAndTravelPlanId(Objects.requireNonNull(1L), Objects.requireNonNull(1L))).thenReturn(Optional.of(activity));

        service.deleteActivity(1L, 1L);

        verify(travelPlanRepository).findById(Objects.requireNonNull(1L));
        verify(activityRepository).findByIdAndTravelPlanId(Objects.requireNonNull(1L), Objects.requireNonNull(1L));
        verify(activityRepository).delete(Objects.requireNonNull(activity));
    }

    @Test
    void deleteActivity_ShouldThrowException_WhenActivityNotFound() {
        when(travelPlanRepository.findById(Objects.requireNonNull(1L))).thenReturn(Optional.of(travelPlan));
        when(activityRepository.findByIdAndTravelPlanId(Objects.requireNonNull(1L), Objects.requireNonNull(1L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteActivity(1L, 1L));

        verify(travelPlanRepository).findById(Objects.requireNonNull(1L));
        verify(activityRepository).findByIdAndTravelPlanId(Objects.requireNonNull(1L), Objects.requireNonNull(1L));
        verify(activityRepository, never()).delete(Objects.requireNonNull(any(TravelPlanActivity.class)));
    }

    @Test
    void deleteActivity_ShouldThrowException_WhenTravelPlanNotFound() {
        when(travelPlanRepository.findById(Objects.requireNonNull(1L))).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteActivity(1L, 1L));

        verify(travelPlanRepository).findById(Objects.requireNonNull(1L));
        verify(activityRepository, never()).findByIdAndTravelPlanId(Objects.requireNonNull(any()), Objects.requireNonNull(any()));
        verify(activityRepository, never()).delete(Objects.requireNonNull(any(TravelPlanActivity.class)));
    }
}
