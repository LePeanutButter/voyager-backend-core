package com.tourism.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendConnectionRequestDto {
    @NotNull(message = "Recipient ID is required")
    private Long recipientId;
    
    private String message;
}
