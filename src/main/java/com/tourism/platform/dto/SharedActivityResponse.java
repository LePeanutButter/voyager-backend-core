package com.tourism.platform.dto;

import com.tourism.platform.model.SharedActivityStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class SharedActivityResponse {
    Long id;
    Long activityId;
    Long senderUserId;
    Long receiverUserId;
    SharedActivityStatus status;
    boolean sharedPlan;
    LocalDateTime createdAt;
}
