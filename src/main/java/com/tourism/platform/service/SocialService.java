package com.tourism.platform.service;

import com.tourism.platform.dto.TravelConnectionDto;
import com.tourism.platform.dto.TravelerSummaryDto;

import java.util.List;

public interface SocialService {
    TravelerSummaryDto getTravelerSummary(Long travelerId);
    List<TravelConnectionDto> getAcceptedConnectionsByTravelPlan(Long travelPlanId);
}
