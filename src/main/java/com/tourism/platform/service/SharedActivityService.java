package com.tourism.platform.service;

import com.tourism.platform.dto.SharedActivityActionRequest;
import com.tourism.platform.dto.SharedActivityResponse;

public interface SharedActivityService {
    SharedActivityResponse shareActivity(Long activityId, Long receiverUserId, Long currentUserId);

    SharedActivityResponse updateSharedActivity(Long sharedActivityId,
                                                SharedActivityActionRequest.SharedActivityAction action,
                                                Long currentUserId);
}
