package com.tourism.platform.service;

import com.tourism.platform.dto.ConnectionRequestDto;
import com.tourism.platform.dto.SendConnectionRequestDto;
import com.tourism.platform.dto.TravelConnectionDto;
import com.tourism.platform.dto.TravelerSummaryDto;
import com.tourism.platform.model.Message;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SocialService {
    TravelerSummaryDto getTravelerSummary(Long travelerId);
    List<TravelConnectionDto> getAcceptedConnectionsByTravelPlan(Long travelPlanId);
    void deleteConnection(Long connectionId, Long requestingUserId);
    // Add these methods to SocialService interface
    Message sendMessage(Long connectionId, Long senderId, String content);
    List<Message> getConversationMessages(Long connectionId, Long userId);
    void markMessageAsRead(Long messageId, Long userId);
    Page<Message> getConversationMessagesPaginated(Long connectionId, Long userId, int page, int size);

    // Connection request management methods
    ConnectionRequestDto sendConnectionRequest(SendConnectionRequestDto request, Long requesterId);
    ConnectionRequestDto acceptConnectionRequest(Long requestId, Long recipientId);
    ConnectionRequestDto rejectConnectionRequest(Long requestId, Long recipientId);
    List<ConnectionRequestDto> getPendingRequestsForUser(Long userId);
    List<ConnectionRequestDto> getSentRequestsForUser(Long userId);
    List<TravelConnectionDto> getUserConnections(Long userId);

}
