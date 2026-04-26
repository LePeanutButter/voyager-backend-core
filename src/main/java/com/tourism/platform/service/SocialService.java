package com.tourism.platform.service;

import com.tourism.platform.dto.TravelConnectionDto;
import com.tourism.platform.dto.TravelerSummaryDto;
import com.tourism.platform.model.Message;

import java.util.List;

public interface SocialService {
    TravelerSummaryDto getTravelerSummary(Long travelerId);
    List<TravelConnectionDto> getAcceptedConnectionsByTravelPlan(Long travelPlanId);
    void deleteConnection(Long connectionId, Long requestingUserId);
    // Add these methods to SocialService interface
    Message sendMessage(Long connectionId, Long senderId, String content);
    List<Message> getConversationMessages(Long connectionId, Long userId);
    void markMessageAsRead(Long messageId, Long userId);
}
