package com.tourism.platform.service.impl;

import com.tourism.platform.dto.SharedActivityActionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.model.*;
import com.tourism.platform.repository.ActivityRepository;
import com.tourism.platform.repository.SharedActivityRepository;
import com.tourism.platform.repository.UserConnectionRepository;
import com.tourism.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedActivityServiceImplTest {

    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private SharedActivityRepository sharedActivityRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserConnectionRepository userConnectionRepository;

    @InjectMocks
    private SharedActivityServiceImpl sharedActivityService;

    private User sender;
    private User receiver;
    private Activity activity;

    @BeforeEach
    void setUp() {
        sender = User.builder().id(1L).username("sender").email("sender@test.com").password("p").firstName("S").lastName("U").build();
        receiver = User.builder().id(2L).username("receiver").email("receiver@test.com").password("p").firstName("R").lastName("U").build();

        activity = Activity.builder()
                .id(10L)
                .ownerUser(sender)
                .tripContextId(100L)
                .title("Museum")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shareActivity_shouldFailWhenCurrentUserIsNotOwner() {
        User anotherUser = User.builder().id(3L).username("other").email("o@test.com").password("p").firstName("O").lastName("U").build();
        activity.setOwnerUser(anotherUser);
        when(activityRepository.findById(10L)).thenReturn(Optional.of(activity));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> sharedActivityService.shareActivity(10L, 2L, 1L));

        assertEquals("Only the activity owner can share this activity", ex.getMessage());
    }

    @Test
    void updateSharedActivity_shouldAcceptAndMarkSharedPlan() {
        SharedActivity sharedActivity = SharedActivity.builder()
                .id(500L)
                .activity(activity)
                .senderUser(sender)
                .receiverUser(receiver)
                .status(SharedActivityStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(sharedActivityRepository.findById(500L)).thenReturn(Optional.of(sharedActivity));
        when(sharedActivityRepository.save(any(SharedActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SharedActivityResponse result = sharedActivityService.updateSharedActivity(
                500L,
                SharedActivityActionRequest.SharedActivityAction.ACCEPT,
                2L
        );

        assertEquals(SharedActivityStatus.ACCEPTED, result.getStatus());
        assertTrue(result.isSharedPlan());
    }

    @Test
    void updateSharedActivity_shouldRejectAndKeepOriginalActivityUntouched() {
        SharedActivity sharedActivity = SharedActivity.builder()
                .id(501L)
                .activity(activity)
                .senderUser(sender)
                .receiverUser(receiver)
                .status(SharedActivityStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(sharedActivityRepository.findById(501L)).thenReturn(Optional.of(sharedActivity));
        when(sharedActivityRepository.save(any(SharedActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SharedActivityResponse result = sharedActivityService.updateSharedActivity(
                501L,
                SharedActivityActionRequest.SharedActivityAction.REJECT,
                2L
        );

        assertEquals(SharedActivityStatus.REJECTED, result.getStatus());
        assertFalse(result.isSharedPlan());
        assertEquals(10L, activity.getId());
    }
}
