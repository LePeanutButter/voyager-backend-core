package com.tourism.platform.dto;

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
