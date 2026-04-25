package com.tourism.platform.service.impl;

import com.tourism.platform.dto.SharedActivityActionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.*;
import com.tourism.platform.repository.ActivityRepository;
import com.tourism.platform.repository.SharedActivityRepository;
import com.tourism.platform.repository.UserConnectionRepository;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.service.SharedActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SharedActivityServiceImpl implements SharedActivityService {

    private final ActivityRepository activityRepository;
    private final SharedActivityRepository sharedActivityRepository;
    private final UserRepository userRepository;
    private final UserConnectionRepository userConnectionRepository;

    @Override
    @Transactional
    public SharedActivityResponse shareActivity(Long activityId, Long receiverUserId, Long currentUserId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found with ID: " + activityId));

        if (!activity.getOwnerUser().getId().equals(currentUserId)) {
            throw new BusinessException("Only the activity owner can share this activity");
        }

        if (currentUserId.equals(receiverUserId)) {
            throw new BusinessException("You cannot share an activity with yourself");
        }

        User senderUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender user not found with ID: " + currentUserId));

        User receiverUser = userRepository.findById(receiverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver user not found with ID: " + receiverUserId));

        if (!isValidConnectionInTripContext(currentUserId, receiverUserId, activity.getTripContextId())) {
            throw new BusinessException("Receiver is not a valid accepted connection in this trip context");
        }

        if (!activityRepository.existsByOwnerUserIdAndTripContextId(receiverUserId, activity.getTripContextId())) {
            throw new BusinessException("Sender and receiver do not belong to the same trip context");
        }

        SharedActivity sharedActivity = SharedActivity.builder()
                .activity(activity)
                .senderUser(senderUser)
                .receiverUser(receiverUser)
                .status(SharedActivityStatus.PENDING)
                .build();

        return toResponse(sharedActivityRepository.save(sharedActivity));
    }

    @Override
    @Transactional
    public SharedActivityResponse updateSharedActivity(Long sharedActivityId,
                                                       SharedActivityActionRequest.SharedActivityAction action,
                                                       Long currentUserId) {
        SharedActivity sharedActivity = sharedActivityRepository.findById(sharedActivityId)
                .orElseThrow(() -> new ResourceNotFoundException("Shared activity not found with ID: " + sharedActivityId));

        if (!sharedActivity.getReceiverUser().getId().equals(currentUserId)) {
            throw new BusinessException("Only the receiver can accept or reject this shared activity");
        }

        if (sharedActivity.getStatus() != SharedActivityStatus.PENDING) {
            throw new BusinessException("Shared activity has already been processed");
        }

        if (action == SharedActivityActionRequest.SharedActivityAction.ACCEPT) {
            sharedActivity.setStatus(SharedActivityStatus.ACCEPTED);
            sharedActivity.setSharedPlan(true);
        } else {
            sharedActivity.setStatus(SharedActivityStatus.REJECTED);
            sharedActivity.setSharedPlan(false);
        }

        return toResponse(sharedActivityRepository.save(sharedActivity));
    }

    private boolean isValidConnectionInTripContext(Long senderUserId, Long receiverUserId, Long tripContextId) {
        return userConnectionRepository.existsByRequesterUserIdAndReceiverUserIdAndTripContextIdAndStatus(
                senderUserId, receiverUserId, tripContextId, UserConnectionStatus.ACCEPTED
        ) || userConnectionRepository.existsByReceiverUserIdAndRequesterUserIdAndTripContextIdAndStatus(
                senderUserId, receiverUserId, tripContextId, UserConnectionStatus.ACCEPTED
        );
    }

    private SharedActivityResponse toResponse(SharedActivity sharedActivity) {
        return SharedActivityResponse.builder()
                .id(sharedActivity.getId())
                .activityId(sharedActivity.getActivity().getId())
                .senderUserId(sharedActivity.getSenderUser().getId())
                .receiverUserId(sharedActivity.getReceiverUser().getId())
                .status(sharedActivity.getStatus())
                .sharedPlan(sharedActivity.isSharedPlan())
                .createdAt(sharedActivity.getCreatedAt())
                .build();
    }
}
