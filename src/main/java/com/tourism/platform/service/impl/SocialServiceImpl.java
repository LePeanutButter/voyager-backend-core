package com.tourism.platform.service.impl;

import com.tourism.platform.dto.TravelConnectionDto;
import com.tourism.platform.dto.TravelerSummaryDto;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.*;
import com.tourism.platform.repository.*;
import com.tourism.platform.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SocialServiceImpl implements SocialService {

    private final UserRepository userRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final ConnectionRepository connectionRepository;
    private final SharedSpaceAccessRepository sharedSpaceAccessRepository;
    private final MessageRepository messageRepository;


    @Override
    @Transactional(readOnly = true)
    public TravelerSummaryDto getTravelerSummary(Long travelerId) {
        User user = userRepository.findById(travelerId)
                .orElseThrow(() -> new ResourceNotFoundException("Traveler not found with ID: " + travelerId));

        String bio = user.getBio() == null ? "" : user.getBio().trim();
        if (bio.length() > 160) {
            bio = bio.substring(0, 160) + "...";
        }

        return TravelerSummaryDto.builder()
                .userId(user.getId())
                .displayName((user.getFirstName() + " " + user.getLastName()).trim())
                .bioShort(bio)
                .profileImageUrl(user.getProfileImageUrl())
                .interests("travel, culture")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TravelConnectionDto> getAcceptedConnectionsByTravelPlan(Long travelPlanId) {
        if (!travelPlanRepository.existsById(travelPlanId)) {
            throw new ResourceNotFoundException("Travel plan not found with ID: " + travelPlanId);
        }

        return userRepository.findAll().stream().limit(3).map(user -> TravelConnectionDto.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .status("ACCEPTED")
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void deleteConnection(Long connectionId, Long requestingUserId) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));

        if (!connection.getRequesterId().equals(requestingUserId) && !connection.getRecipientId().equals(requestingUserId)) {
            throw new ResourceNotFoundException("Connection not found or access denied");
        }

        // Delete related messages first
        messageRepository.deleteByConnectionId(connectionId);

        // Revoke shared space access
        sharedSpaceAccessRepository.deleteByConnectionId(connectionId);

        // Delete the connection
        connectionRepository.deleteById(connectionId);
    }

    // Add these implementations to SocialServiceImpl
    @Override
    @Transactional
    public Message sendMessage(Long connectionId, Long senderId, String content) {
        // Validate connection exists and user is part of it
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));

        if (!connection.getRequesterId().equals(senderId) && !connection.getRecipientId().equals(senderId)) {
            throw new IllegalArgumentException("User is not part of this connection");
        }

        if (connection.getStatus() != ConnectionStatus.ACCEPTED) {
            throw new IllegalArgumentException("Cannot send messages to unaccepted connections");
        }

        // Validate content is not empty
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        // Determine recipient
        Long recipientId = connection.getRequesterId().equals(senderId) ?
                connection.getRecipientId() : connection.getRequesterId();

        Message message = new Message();
        message.setConnectionId(connectionId);
        message.setSenderId(senderId);
        message.setRecipientId(recipientId);
        message.setContent(content.trim());
        message.setStatus(MessageStatus.SENT);

        return messageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getConversationMessages(Long connectionId, Long userId) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));

        if (!connection.getRequesterId().equals(userId) && !connection.getRecipientId().equals(userId)) {
            throw new IllegalArgumentException("User is not part of this connection");
        }

        // Get messages in both directions
        List<Message> messagesFromUser1 = messageRepository.findBySenderIdAndRecipientIdOrderByCreatedAtDesc(
                connection.getRequesterId(), connection.getRecipientId());
        List<Message> messagesFromUser2 = messageRepository.findBySenderIdAndRecipientIdOrderByCreatedAtDesc(
                connection.getRecipientId(), connection.getRequesterId());

        // Combine and sort by timestamp
        return Stream.concat(messagesFromUser1.stream(), messagesFromUser2.stream())
                .sorted(Comparator.comparing(Message::getCreatedAt).reversed())
                .toList();
    }

    @Override
    @Transactional
    public void markMessageAsRead(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found"));

        if (!message.getRecipientId().equals(userId)) {
            throw new IllegalArgumentException("Only message recipients can mark messages as read");
        }

        message.setStatus(MessageStatus.READ);
        messageRepository.save(message);
    }
}
