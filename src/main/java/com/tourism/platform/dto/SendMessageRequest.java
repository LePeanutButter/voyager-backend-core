package com.tourism.platform.dto;

/**
 * Request DTO used when sending a chat message. Contains connection, sender
 * identifiers and the text content sent to the recipient.
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SendMessageRequest {
    // Getters and Setters
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

}