package com.tourism.platform.service;

import com.tourism.platform.dto.SharedActivityDecisionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import org.springframework.lang.NonNull;

public interface SharedActivityService {

    SharedActivityResponse shareActivity(@NonNull Long activityId, @NonNull Long receiverId, String senderUsername);

    SharedActivityResponse resolveSharedActivity(@NonNull Long sharedActivityId, SharedActivityDecisionRequest request, String receiverUsername);
}
