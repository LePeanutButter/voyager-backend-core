package com.tourism.platform.dto;

/**
 * Response DTO returned after sharing an activity. Includes identifiers and
 * any metadata required by the client to render shared activity information.
 */

import com.tourism.platform.model.SharedActivityStatus;

public class SharedActivityResponse {
    private Long id;
    private Long activityId;
    private Long senderId;
    private Long receiverId;
    private SharedActivityStatus status;
    private boolean sharedPlan;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public SharedActivityStatus getStatus() {
        return status;
    }

    public void setStatus(SharedActivityStatus status) {
        this.status = status;
    }

    public boolean isSharedPlan() {
        return sharedPlan;
    }

    public void setSharedPlan(boolean sharedPlan) {
        this.sharedPlan = sharedPlan;
    }
}
