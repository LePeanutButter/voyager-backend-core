package com.tourism.platform.service.impl;

import com.tourism.platform.dto.SharedActivityDecisionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import com.tourism.platform.exception.BadRequestException;
import com.tourism.platform.exception.ConflictException;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.ConnectionStatus;
import com.tourism.platform.model.SharedActivity;
import com.tourism.platform.model.SharedActivityDecisionAction;
import com.tourism.platform.model.SharedActivityStatus;
import com.tourism.platform.model.TravelPlan;
import com.tourism.platform.model.TravelPlanActivity;
import com.tourism.platform.model.User;
import com.tourism.platform.repository.SharedActivityRepository;
import com.tourism.platform.repository.TravelPlanActivityRepository;
import com.tourism.platform.repository.TravelPlanParticipantRepository;
import com.tourism.platform.repository.UserConnectionRepository;
import com.tourism.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedActivityServiceImplTest {

    @Mock
    private TravelPlanActivityRepository activityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SharedActivityRepository sharedActivityRepository;
    @Mock
    private UserConnectionRepository connectionRepository;
    @Mock
    private TravelPlanParticipantRepository participantRepository;

    private SharedActivityServiceImpl service;

    private User sender;
    private User receiver;
    private TravelPlanActivity activity;

    @BeforeEach
    void setUp() {
        service = new SharedActivityServiceImpl(
                activityRepository,
                userRepository,
                sharedActivityRepository,
                connectionRepository,
                participantRepository,
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        );

        sender = new User();
        sender.setId(10L);
        sender.setUsername("sender");

        receiver = new User();
        receiver.setId(20L);
        receiver.setUsername("receiver");

        TravelPlan trip = new TravelPlan();
        trip.setId(99L);
        trip.setUser(sender);

        activity = new TravelPlanActivity();
        activity.setId(7L);
        activity.setTravelPlan(trip);
        activity.setStartTime(LocalDateTime.now());
        activity.setEndTime(LocalDateTime.now().plusHours(2));
    }

    @Test
    void shareActivity_shouldPreventSharingToSameUser() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sender));

        assertThrows(BadRequestException.class, () -> service.shareActivity(7L, 10L, "sender"));
    }

    @Test
    void shareActivity_throwsWhenSenderNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.shareActivity(7L, 20L, "ghost"));
    }

    @Test
    void shareActivity_throwsWhenActivityNotFound() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_throwsWhenReceiverNotFound() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(20L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_throwsWhenTripContextInvalid() {
        TravelPlanActivity orphan = new TravelPlanActivity();
        orphan.setId(7L);
        orphan.setTravelPlan(null);

        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(orphan));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));

        assertThrows(ConflictException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_throwsWhenTripIdMissing() {
        TravelPlan bare = new TravelPlan();
        bare.setUser(sender);
        TravelPlanActivity a = new TravelPlanActivity();
        a.setId(7L);
        a.setTravelPlan(bare);

        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(a));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));

        assertThrows(ConflictException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_throwsWhenOwnerUnknownOnTrip() {
        TravelPlan trip = new TravelPlan();
        trip.setId(99L);
        trip.setUser(null);
        TravelPlanActivity a = new TravelPlanActivity();
        a.setId(7L);
        a.setTravelPlan(trip);

        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(a));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));

        assertThrows(AccessDeniedException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_throwsWhenReceiverNotOnTrip() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));
        when(participantRepository.existsByTravelPlanIdAndUserId(99L, 20L)).thenReturn(false);

        assertThrows(ConflictException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_succeedsWhenEligible() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));
        when(participantRepository.existsByTravelPlanIdAndUserId(99L, 20L)).thenReturn(true);
        when(connectionRepository.existsConnectionBetweenUsersWithStatus(10L, 20L, ConnectionStatus.ACCEPTED)).thenReturn(true);
        when(sharedActivityRepository.findTopByActivityIdAndReceiverIdOrderByCreatedAtDesc(7L, 20L))
                .thenReturn(Optional.empty());
        when(sharedActivityRepository.save(any(SharedActivity.class))).thenAnswer(inv -> {
            SharedActivity sa = inv.getArgument(0);
            ReflectionTestUtils.setField(sa, "id", 55L);
            return sa;
        });

        SharedActivityResponse response = service.shareActivity(7L, 20L, "sender");

        assertEquals(55L, response.getId());
        assertEquals(SharedActivityStatus.PENDING, response.getStatus());
        assertFalse(response.isSharedPlan());
    }

    @Test
    void shareActivity_shouldRejectWhenSenderIsNotOwner() {
        User nonOwner = new User();
        nonOwner.setId(11L);
        nonOwner.setUsername("nonOwner");
        when(userRepository.findByUsername("nonOwner")).thenReturn(Optional.of(nonOwner));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));

        assertThrows(AccessDeniedException.class, () -> service.shareActivity(7L, 20L, "nonOwner"));
    }

    @Test
    void shareActivity_shouldRejectDuplicatePendingShare() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));
        when(participantRepository.existsByTravelPlanIdAndUserId(99L, 20L)).thenReturn(true);
        when(connectionRepository.existsConnectionBetweenUsersWithStatus(10L, 20L, ConnectionStatus.ACCEPTED)).thenReturn(true);
        SharedActivity existing = new SharedActivity();
        existing.setStatus(SharedActivityStatus.PENDING);
        when(sharedActivityRepository.findTopByActivityIdAndReceiverIdOrderByCreatedAtDesc(7L, 20L))
                .thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_shouldRejectWhenConnectionIsNotAccepted() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));
        when(participantRepository.existsByTravelPlanIdAndUserId(99L, 20L)).thenReturn(true);
        when(connectionRepository.existsConnectionBetweenUsersWithStatus(10L, 20L, ConnectionStatus.ACCEPTED)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void shareActivity_shouldRejectWhenAlreadyAccepted() {
        when(userRepository.findByUsername("sender")).thenReturn(Optional.of(sender));
        when(activityRepository.findById(7L)).thenReturn(Optional.of(activity));
        when(userRepository.findById(20L)).thenReturn(Optional.of(receiver));
        when(participantRepository.existsByTravelPlanIdAndUserId(99L, 20L)).thenReturn(true);
        when(connectionRepository.existsConnectionBetweenUsersWithStatus(10L, 20L, ConnectionStatus.ACCEPTED)).thenReturn(true);
        SharedActivity existing = new SharedActivity();
        existing.setStatus(SharedActivityStatus.ACCEPTED);
        when(sharedActivityRepository.findTopByActivityIdAndReceiverIdOrderByCreatedAtDesc(7L, 20L))
                .thenReturn(Optional.of(existing));

        assertThrows(ConflictException.class, () -> service.shareActivity(7L, 20L, "sender"));
    }

    @Test
    void resolveSharedActivity_shouldAcceptPendingShare() {
        SharedActivity sharedActivity = new SharedActivity();
        sharedActivity.setActivity(activity);
        sharedActivity.setSender(sender);
        sharedActivity.setReceiver(receiver);
        sharedActivity.setStatus(SharedActivityStatus.PENDING);

        SharedActivityDecisionRequest request = new SharedActivityDecisionRequest();
        request.setAction(SharedActivityDecisionAction.ACCEPT);

        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(sharedActivityRepository.findById(1L)).thenReturn(Optional.of(sharedActivity));
        when(sharedActivityRepository.save(any(SharedActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.resolveSharedActivity(1L, request, "receiver");
        assertEquals(SharedActivityStatus.ACCEPTED, response.getStatus());
        assertTrue(response.isSharedPlan());
    }

    @Test
    void resolveSharedActivity_shouldRejectWhenAlreadyResolved() {
        SharedActivity sharedActivity = new SharedActivity();
        sharedActivity.setActivity(activity);
        sharedActivity.setSender(sender);
        sharedActivity.setReceiver(receiver);
        sharedActivity.setStatus(SharedActivityStatus.ACCEPTED);

        SharedActivityDecisionRequest request = new SharedActivityDecisionRequest();
        request.setAction(SharedActivityDecisionAction.REJECT);

        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(sharedActivityRepository.findById(1L)).thenReturn(Optional.of(sharedActivity));

        assertThrows(ConflictException.class, () -> service.resolveSharedActivity(1L, request, "receiver"));
    }

    @Test
    void resolveSharedActivity_shouldRejectPendingShare() {
        SharedActivity sharedActivity = new SharedActivity();
        sharedActivity.setActivity(activity);
        sharedActivity.setSender(sender);
        sharedActivity.setReceiver(receiver);
        sharedActivity.setStatus(SharedActivityStatus.PENDING);

        SharedActivityDecisionRequest request = new SharedActivityDecisionRequest();
        request.setAction(SharedActivityDecisionAction.REJECT);

        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(sharedActivityRepository.findById(1L)).thenReturn(Optional.of(sharedActivity));
        when(sharedActivityRepository.save(any(SharedActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SharedActivityResponse response = service.resolveSharedActivity(1L, request, "receiver");

        assertEquals(SharedActivityStatus.REJECTED, response.getStatus());
        assertFalse(response.isSharedPlan());
    }

    @Test
    void resolveSharedActivity_throwsWhenReceiverNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        SharedActivityDecisionRequest request = new SharedActivityDecisionRequest();
        request.setAction(SharedActivityDecisionAction.ACCEPT);
        assertThrows(ResourceNotFoundException.class, () -> service.resolveSharedActivity(1L, request, "ghost"));
    }

    @Test
    void resolveSharedActivity_throwsWhenShareNotFound() {
        when(userRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(sharedActivityRepository.findById(1L)).thenReturn(Optional.empty());
        SharedActivityDecisionRequest request = new SharedActivityDecisionRequest();
        request.setAction(SharedActivityDecisionAction.ACCEPT);
        assertThrows(ResourceNotFoundException.class, () -> service.resolveSharedActivity(1L, request, "receiver"));
    }

    @Test
    void resolveSharedActivity_throwsWhenCallerIsNotReceiver() {
        SharedActivity sharedActivity = new SharedActivity();
        sharedActivity.setActivity(activity);
        sharedActivity.setSender(sender);
        sharedActivity.setReceiver(receiver);
        sharedActivity.setStatus(SharedActivityStatus.PENDING);

        User other = new User();
        other.setId(99L);
        other.setUsername("other");

        SharedActivityDecisionRequest request = new SharedActivityDecisionRequest();
        request.setAction(SharedActivityDecisionAction.ACCEPT);

        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));
        when(sharedActivityRepository.findById(1L)).thenReturn(Optional.of(sharedActivity));

        assertThrows(AccessDeniedException.class, () -> service.resolveSharedActivity(1L, request, "other"));
    }
}
