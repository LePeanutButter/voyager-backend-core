package com.tourism.platform.service.impl;

import com.tourism.platform.dto.ConnectionRequestDto;
import com.tourism.platform.dto.SendConnectionRequestDto;
import com.tourism.platform.dto.TravelConnectionDto;
import com.tourism.platform.dto.TravelerSummaryDto;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.*;
import com.tourism.platform.repository.*;
import com.tourism.platform.service.SocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SocialServiceImpl implements SocialService {

    private static final String CONNECTION_NOT_FOUND = "Connection not found";
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final ConnectionRepository connectionRepository;
    private final SharedSpaceAccessRepository sharedSpaceAccessRepository;


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
                .orElseThrow(() -> new ResourceNotFoundException(CONNECTION_NOT_FOUND));

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
                .orElseThrow(() -> new ResourceNotFoundException(CONNECTION_NOT_FOUND));

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
                .orElseThrow(() -> new ResourceNotFoundException(CONNECTION_NOT_FOUND));

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

    // Connection request management methods
    @Override
    @Transactional
    public ConnectionRequestDto sendConnectionRequest(SendConnectionRequestDto request, Long requesterId) {
        // Validate recipient exists
        if (!userRepository.existsById(request.getRecipientId())) {
            throw new ResourceNotFoundException("Recipient user not found");
        }

        // Check if user is trying to send request to themselves
        if (request.getRecipientId().equals(requesterId)) {
            throw new IllegalArgumentException("Cannot send connection request to yourself");
        }

        // Check if a connection already exists between these users
        Optional<Connection> existingConnection = connectionRepository
                .findByRequesterIdAndRecipientId(requesterId, request.getRecipientId());
        if (existingConnection.isPresent()) {
            Connection connection = existingConnection.get();
            if (connection.getStatus() == ConnectionStatus.PENDING) {
                throw new IllegalArgumentException("A pending connection request already exists");
            } else if (connection.getStatus() == ConnectionStatus.ACCEPTED) {
                throw new IllegalArgumentException("Users are already connected");
            } else {
                // If rejected or blocked, create a new request
                connection.setStatus(ConnectionStatus.PENDING);
                connection = connectionRepository.save(connection);
                return convertToConnectionRequestDto(connection);
            }
        }

        // Also check reverse direction (in case recipient already sent a request)
        Optional<Connection> reverseConnection = connectionRepository
                .findByRequesterIdAndRecipientId(request.getRecipientId(), requesterId);
        if (reverseConnection.isPresent()) {
            Connection connection = reverseConnection.get();
            if (connection.getStatus() == ConnectionStatus.PENDING) {
                throw new IllegalArgumentException("A pending connection request already exists");
            } else if (connection.getStatus() == ConnectionStatus.ACCEPTED) {
                throw new IllegalArgumentException("Users are already connected");
            }
        }

        // Create new connection request
        Connection connection = new Connection();
        connection.setRequesterId(requesterId);
        connection.setRecipientId(request.getRecipientId());
        connection.setStatus(ConnectionStatus.PENDING);
        connection.setMessage(request.getMessage());
        connection = connectionRepository.save(connection);

        return convertToConnectionRequestDto(connection);
    }

    @Override
    @Transactional
    public ConnectionRequestDto acceptConnectionRequest(Long requestId, Long recipientId) {
        Connection connection = connectionRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));

        if (!connection.getRecipientId().equals(recipientId)) {
            throw new IllegalArgumentException("Only the recipient can accept this request");
        }

        if (connection.getStatus() != ConnectionStatus.PENDING) {
            throw new IllegalArgumentException("This connection request cannot be accepted");
        }

        connection.setStatus(ConnectionStatus.ACCEPTED);
        connection = connectionRepository.save(connection);

        return convertToConnectionRequestDto(connection);
    }

    @Override
    @Transactional
    public ConnectionRequestDto rejectConnectionRequest(Long requestId, Long recipientId) {
        Connection connection = connectionRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));

        if (!connection.getRecipientId().equals(recipientId)) {
            throw new IllegalArgumentException("Only the recipient can reject this request");
        }

        if (connection.getStatus() != ConnectionStatus.PENDING) {
            throw new IllegalArgumentException("This connection request cannot be rejected");
        }

        connection.setStatus(ConnectionStatus.REJECTED);
        connection = connectionRepository.save(connection);

        return convertToConnectionRequestDto(connection);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectionRequestDto> getPendingRequestsForUser(Long userId) {
        List<Connection> pendingRequests = connectionRepository
                .findByRecipientIdAndStatus(userId, ConnectionStatus.PENDING);
        
        return pendingRequests.stream()
                .map(this::convertToConnectionRequestDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectionRequestDto> getSentRequestsForUser(Long userId) {
        List<Connection> sentRequests = connectionRepository
                .findByRequesterIdAndStatus(userId, ConnectionStatus.PENDING);
        
        return sentRequests.stream()
                .map(this::convertToConnectionRequestDto)
                .toList();
    }

    private ConnectionRequestDto convertToConnectionRequestDto(Connection connection) {
        ConnectionRequestDto dto = new ConnectionRequestDto();
        dto.setId(connection.getId());
        dto.setRequesterId(connection.getRequesterId());
        dto.setRecipientId(connection.getRecipientId());
        dto.setStatus(connection.getStatus().toString());
        dto.setMessage(connection.getMessage());
        dto.setCreatedAt(connection.getCreatedAt());
        dto.setUpdatedAt(connection.getUpdatedAt());

        // Load user information for display
        User requester = userRepository.findById(connection.getRequesterId()).orElse(null);
        User recipient = userRepository.findById(connection.getRecipientId()).orElse(null);

        if (requester != null) {
            dto.setRequesterName(requester.getFirstName() + " " + requester.getLastName());
            dto.setRequesterProfileImage(requester.getProfileImageUrl());
        }

        if (recipient != null) {
            dto.setRecipientName(recipient.getFirstName() + " " + recipient.getLastName());
            dto.setRecipientProfileImage(recipient.getProfileImageUrl());
        }

        return dto;
    }
    @Override
    @Transactional(readOnly = true)
    public List<TravelConnectionDto> getUserConnections(Long userId) {
        // Get all connections where user is either requester or recipient and status is ACCEPTED
        List<Connection> connections = connectionRepository.findByRequesterIdAndStatus(userId, ConnectionStatus.ACCEPTED);
        connections.addAll(connectionRepository.findByRecipientIdAndStatus(userId, ConnectionStatus.ACCEPTED));

        return connections.stream()
                .map(connection -> {
                    Long otherUserId = connection.getRequesterId().equals(userId) ?
                            connection.getRecipientId() : connection.getRequesterId();
                    User otherUser = userRepository.findById(otherUserId).orElse(null);

                    if (otherUser != null) {
                        return TravelConnectionDto.builder()
                                .userId(otherUser.getId())
                                .username(otherUser.getUsername())
                                .firstName(otherUser.getFirstName())
                                .lastName(otherUser.getLastName())
                                .status(connection.getStatus().toString())
                                .build();
                    }
                    return null;
                })
                .filter(dto -> dto != null)
                .toList();
    }
    @Override
    @Transactional(readOnly = true)
    public Page<Message> getConversationMessagesPaginated(Long connectionId, Long userId, int page, int size) {
        Connection connection = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException(CONNECTION_NOT_FOUND));

        if (!connection.getRequesterId().equals(userId) && !connection.getRecipientId().equals(userId)) {
            throw new IllegalArgumentException("User is not part of this connection");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return messageRepository.findConversationMessages(connectionId, pageable);
    }
}
