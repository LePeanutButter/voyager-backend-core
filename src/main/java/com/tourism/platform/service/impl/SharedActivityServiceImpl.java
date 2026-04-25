package com.tourism.platform.service.impl;

import com.tourism.platform.dto.SharedActivityDecisionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import com.tourism.platform.exception.ConflictException;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.*;
import com.tourism.platform.repository.*;
import com.tourism.platform.service.SharedActivityService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SharedActivityServiceImpl implements SharedActivityService {

    private final TravelPlanActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final SharedActivityRepository sharedActivityRepository;
    private final UserConnectionRepository connectionRepository;
    private final TravelPlanParticipantRepository participantRepository;

    public SharedActivityServiceImpl(
            TravelPlanActivityRepository activityRepository,
            UserRepository userRepository,
            SharedActivityRepository sharedActivityRepository,
            UserConnectionRepository connectionRepository,
            TravelPlanParticipantRepository participantRepository) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.sharedActivityRepository = sharedActivityRepository;
        this.connectionRepository = connectionRepository;
        this.participantRepository = participantRepository;
    }

    @Override
    public SharedActivityResponse shareActivity(Long activityId, Long receiverId, String senderUsername) {
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        TravelPlanActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        if (sender.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Sender and receiver must be different users");
        }

        if (activity.getTravelPlan() == null || activity.getTravelPlan().getId() == null) {
            throw new ConflictException("Activity does not belong to a valid trip context");
        }

        Long tripId = activity.getTravelPlan().getId();
        Long ownerId = activity.getTravelPlan().getUser() != null ? activity.getTravelPlan().getUser().getId() : null;

        if (ownerId == null || !ownerId.equals(sender.getId())) {
            throw new AccessDeniedException("Only the activity owner can share this activity");
        }

        boolean senderInTrip = ownerId.equals(sender.getId()) || participantRepository.existsByTravelPlanIdAndUserId(tripId, sender.getId());
        boolean receiverInTrip = ownerId.equals(receiver.getId()) || participantRepository.existsByTravelPlanIdAndUserId(tripId, receiver.getId());
        if (!senderInTrip || !receiverInTrip) {
            throw new ConflictException("Both users must belong to the same trip");
        }

        boolean connected = connectionRepository.existsConnectionBetweenUsersWithStatus(
                sender.getId(), receiver.getId(), ConnectionStatus.ACCEPTED
        );
        if (!connected) {
            throw new AccessDeniedException("Users must have an accepted connection");
        }

        if (sharedActivityRepository.existsByActivityIdAndReceiverIdAndStatus(
                activity.getId(), receiver.getId(), SharedActivityStatus.PENDING)) {
            throw new ConflictException("A pending share already exists for this receiver");
        }

        SharedActivity sharedActivity = new SharedActivity();
        sharedActivity.setActivity(activity);
        sharedActivity.setSender(sender);
        sharedActivity.setReceiver(receiver);
        sharedActivity.setStatus(SharedActivityStatus.PENDING);
        sharedActivity.setSharedPlan(false);

        return toResponse(sharedActivityRepository.save(sharedActivity));
    }

    @Override
    public SharedActivityResponse resolveSharedActivity(Long sharedActivityId, SharedActivityDecisionRequest request, String receiverUsername) {
        User receiver = userRepository.findByUsername(receiverUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        SharedActivity sharedActivity = sharedActivityRepository.findById(sharedActivityId)
                .orElseThrow(() -> new ResourceNotFoundException("Shared activity not found"));

        if (!sharedActivity.getReceiver().getId().equals(receiver.getId())) {
            throw new AccessDeniedException("Only the receiver can update shared activity status");
        }

        if (sharedActivity.getStatus() != SharedActivityStatus.PENDING) {
            throw new ConflictException("Only pending shared activities can be updated");
        }

        if (request.getAction() == SharedActivityDecisionAction.ACCEPT) {
            sharedActivity.setStatus(SharedActivityStatus.ACCEPTED);
            sharedActivity.setSharedPlan(true);
        } else {
            sharedActivity.setStatus(SharedActivityStatus.REJECTED);
            sharedActivity.setSharedPlan(false);
        }

        return toResponse(sharedActivityRepository.save(sharedActivity));
    }

    private SharedActivityResponse toResponse(SharedActivity sharedActivity) {
        SharedActivityResponse response = new SharedActivityResponse();
        response.setId(sharedActivity.getId());
        response.setActivityId(sharedActivity.getActivity().getId());
        response.setSenderId(sharedActivity.getSender().getId());
        response.setReceiverId(sharedActivity.getReceiver().getId());
        response.setStatus(sharedActivity.getStatus());
        response.setSharedPlan(sharedActivity.isSharedPlan());
        return response;
    }
}
