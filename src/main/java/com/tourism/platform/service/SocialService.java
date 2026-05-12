package com.tourism.platform.service;

import com.tourism.platform.dto.ConnectionRequestDto;
import com.tourism.platform.dto.SendConnectionRequestDto;
import com.tourism.platform.dto.TravelConnectionDto;
import com.tourism.platform.dto.TravelerSummaryDto;
import com.tourism.platform.model.Message;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SocialService {

    /**
     * Retrieve a summary for a traveler including basic stats and profile info.
     *
     * @param travelerId id of the traveler
     * @return TravelerSummaryDto with aggregated traveler information
     */
    TravelerSummaryDto getTravelerSummary(Long travelerId);

    /**
     * Get accepted connections associated with a specific travel plan.
     *
     * @param travelPlanId id of the travel plan
     * @return list of accepted TravelConnectionDto instances
     */
    List<TravelConnectionDto> getAcceptedConnectionsByTravelPlan(Long travelPlanId);

    /**
     * Delete a connection record.
     *
     * @param connectionId     id of the connection to delete
     * @param requestingUserId id of the user requesting deletion
     */
    void deleteConnection(Long connectionId, Long requestingUserId);

    /**
     * Send a message over an existing connection.
     *
     * @param connectionId id of the connection
     * @param senderId     id of the message sender
     * @param content      message text content
     * @return persisted Message entity
     */
    Message sendMessage(Long connectionId, Long senderId, String content);

    /**
     * Retrieve conversation messages for a connection.
     *
     * @param connectionId id of the connection
     * @param userId       id of the requesting user
     * @return list of Message entities in conversation order
     */
    List<Message> getConversationMessages(Long connectionId, Long userId);

    /**
     * Mark a message as read for a user.
     *
     * @param messageId id of the message to mark read
     * @param userId    id of the user marking the message
     */
    void markMessageAsRead(Long messageId, Long userId);

    /**
     * Retrieve paginated conversation messages for a connection.
     *
     * @param connectionId id of the connection
     * @param userId       id of the requesting user
     * @param page         zero-based page index
     * @param size         page size
     * @return Page containing Message entities
     */
    Page<Message> getConversationMessagesPaginated(Long connectionId, Long userId, int page, int size);

    /**
     * Send a connection request from a user to another user.
     *
     * @param request     DTO containing connection request details
     * @param requesterId id of the user sending the request
     * @return ConnectionRequestDto representing the created request
     */
    ConnectionRequestDto sendConnectionRequest(SendConnectionRequestDto request, Long requesterId);

    /**
     * Accept a pending connection request.
     *
     * @param requestId   id of the connection request
     * @param recipientId id of the recipient accepting the request
     * @return ConnectionRequestDto representing the accepted request
     */
    ConnectionRequestDto acceptConnectionRequest(Long requestId, Long recipientId);

    /**
     * Reject a pending connection request.
     *
     * @param requestId   id of the connection request
     * @param recipientId id of the recipient rejecting the request
     * @return ConnectionRequestDto representing the rejected request
     */
    ConnectionRequestDto rejectConnectionRequest(Long requestId, Long recipientId);

    /**
     * Get pending connection requests for a user.
     *
     * @param userId id of the user for whom pending requests are retrieved
     * @return list of ConnectionRequestDto that are pending
     */
    List<ConnectionRequestDto> getPendingRequestsForUser(Long userId);

    /**
     * Get connection requests sent by a user.
     *
     * @param userId id of the user
     * @return list of ConnectionRequestDto that the user has sent
     */
    List<ConnectionRequestDto> getSentRequestsForUser(Long userId);

    /**
     * Get connections for a user.
     *
     * @param userId id of the user
     * @return list of TravelConnectionDto representing the user's connections
     */
    List<TravelConnectionDto> getUserConnections(Long userId);

}
