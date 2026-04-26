package com.tourism.platform.service;

import com.tourism.platform.dto.CompatibilityMatchRequest;
import com.tourism.platform.dto.CompatibilityMatchResponse;

import java.util.List;

public interface CompatibilityMatchingService {

    List<CompatibilityMatchResponse> findMatches(CompatibilityMatchRequest request, String requesterUsername);
}
