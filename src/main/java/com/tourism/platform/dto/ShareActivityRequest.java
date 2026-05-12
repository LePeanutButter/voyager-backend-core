package com.tourism.platform.dto;

/**
 * Request DTO used to share an activity with another user. Contains the
 * receiver identifier and optional message or metadata for the share operation.
 */

import jakarta.validation.constraints.NotNull;

public class ShareActivityRequest {

    @NotNull(message = "receiverId is required")
    private Long receiverId;

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }
}
