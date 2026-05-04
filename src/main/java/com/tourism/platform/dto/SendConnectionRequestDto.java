package com.tourism.platform.dto;

/**
 * DTO used to send a connection request to another traveler. Contains the
 * target user id and optional message for the request.
 */

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendConnectionRequestDto {
    @NotNull(message = "Recipient ID is required")
    private Long recipientId;
    
    private String message;
}
