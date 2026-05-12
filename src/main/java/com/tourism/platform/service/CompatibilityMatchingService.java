package com.tourism.platform.service;

import com.tourism.platform.dto.CompatibilityMatchRequest;
import com.tourism.platform.dto.CompatibilityMatchResponse;

import java.util.List;

public interface CompatibilityMatchingService {

    /**
     * Find compatibility matches based on a detailed request object.
     *
     * @param request           compatibility matching criteria
     * @param requesterUsername username of the user requesting matches (for personalization/audit)
     * @return list of CompatibilityMatchResponse containing match scores and metadata
     */
    List<CompatibilityMatchResponse> findMatches(CompatibilityMatchRequest request, String requesterUsername);
}
