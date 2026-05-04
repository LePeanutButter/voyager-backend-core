package com.tourism.platform.service.impl;

import com.tourism.platform.dto.SharedActivityDecisionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import com.tourism.platform.exception.BadRequestException;
import com.tourism.platform.exception.ConflictException;
import com.tourism.platform.exception.ResourceNotFoundException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import com.tourism.platform.model.*;
import com.tourism.platform.repository.*;
import com.tourism.platform.service.SharedActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SharedActivityServiceImpl implements SharedActivityService {
    private static final Logger log = LoggerFactory.getLogger(SharedActivityServiceImpl.class);
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final TravelPlanActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final SharedActivityRepository sharedActivityRepository;
    private final UserConnectionRepository connectionRepository;
    private final TravelPlanParticipantRepository participantRepository;
    private final MeterRegistry meterRegistry;

    public SharedActivityServiceImpl(
            TravelPlanActivityRepository activityRepository,
            UserRepository userRepository,
            SharedActivityRepository sharedActivityRepository,
            UserConnectionRepository connectionRepository,
            TravelPlanParticipantRepository participantRepository,
            MeterRegistry meterRegistry) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.sharedActivityRepository = sharedActivityRepository;
        this.connectionRepository = connectionRepository;
        this.participantRepository = participantRepository;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public SharedActivityResponse shareActivity(@NonNull Long activityId, @NonNull Long receiverId, String senderUsername) {
        log.info("event=shared_activity_share_start activityId={} receiverId={}", activityId, receiverId);
        User sender = userRepository.findByUsername(senderUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));

        TravelPlanActivity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found"));

        if (sender.getId().equals(receiver.getId())) {
            throw new BadRequestException("Sender and receiver must be different users");
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

        var latestShare = sharedActivityRepository.findTopByActivityIdAndReceiverIdOrderByCreatedAtDesc(
                activity.getId(),
                receiver.getId()
        );
        latestShare.map(SharedActivity::getStatus).ifPresent(this::validateLatestShareStatus);

        SharedActivity sharedActivity = new SharedActivity();
        sharedActivity.setActivity(activity);
        sharedActivity.setSender(sender);
        sharedActivity.setReceiver(receiver);
        sharedActivity.setStatus(SharedActivityStatus.PENDING);
        sharedActivity.setSharedPlan(false);

        SharedActivityResponse response = toResponse(sharedActivityRepository.save(sharedActivity));
        Counter.builder("shared_activity_requests_total")
                .description("Total shared activity requests")
                .register(meterRegistry)
                .increment();
        log.info("event=shared_activity_share_end sharedActivityId={} status={}", response.getId(), STATUS_SUCCESS);
        return response;
    }

    @Override
    public SharedActivityResponse resolveSharedActivity(@NonNull Long sharedActivityId, SharedActivityDecisionRequest request, String receiverUsername) {
        log.info("event=shared_activity_resolve_start sharedActivityId={}", sharedActivityId);
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

        SharedActivityResponse response = toResponse(sharedActivityRepository.save(sharedActivity));
        log.info("event=shared_activity_resolve_end sharedActivityId={} resultStatus={} status={}",
                response.getId(), response.getStatus(), STATUS_SUCCESS);
        return response;
    }

    private void validateLatestShareStatus(SharedActivityStatus status) {
        if (status == SharedActivityStatus.PENDING) {
            throw new ConflictException("A pending share already exists for this receiver");
        }
        if (status == SharedActivityStatus.ACCEPTED) {
            throw new ConflictException("This activity is already accepted by the receiver");
        }
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
