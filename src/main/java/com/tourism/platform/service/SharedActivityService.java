package com.tourism.platform.service;

import com.tourism.platform.dto.SharedActivityDecisionRequest;
import com.tourism.platform.dto.SharedActivityResponse;

public interface SharedActivityService {

    SharedActivityResponse shareActivity(Long activityId, Long receiverId, String senderUsername);

    SharedActivityResponse resolveSharedActivity(Long sharedActivityId, SharedActivityDecisionRequest request, String receiverUsername);
}
