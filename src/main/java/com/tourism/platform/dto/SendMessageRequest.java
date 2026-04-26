package com.tourism.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class SendMessageRequest {
    @NotNull
    private Long connectionId;

    @NotNull
    private Long senderId;

    @NotBlank
    @Size(max = 1000)
    private String content;

    // Constructors
    public SendMessageRequest() {}

    public SendMessageRequest(Long connectionId, Long senderId, String content) {
        this.connectionId = connectionId;
        this.senderId = senderId;
        this.content = content;
    }

    // Getters and Setters
    public Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}