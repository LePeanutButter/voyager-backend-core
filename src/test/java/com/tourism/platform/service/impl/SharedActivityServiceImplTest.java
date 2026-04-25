package com.tourism.platform.service.impl;

import com.tourism.platform.dto.SharedActivityDecisionRequest;
import com.tourism.platform.exception.ConflictException;
import com.tourism.platform.model.*;
import com.tourism.platform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @InjectMocks
    private SharedActivityServiceImpl service;

    private User sender;
    private User receiver;
    private TravelPlanActivity activity;

    @BeforeEach
    void setUp() {
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

        assertThrows(IllegalArgumentException.class, () -> service.shareActivity(7L, 10L, "sender"));
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
        when(sharedActivityRepository.existsByActivityIdAndReceiverIdAndStatus(7L, 20L, SharedActivityStatus.PENDING)).thenReturn(true);

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
}
