package com.tourism.platform.dto;

/**
 * DTO representing a connection (friendship) request between travelers.
 * Used to transfer the minimal information required when sending or
 * responding to connection requests.
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConnectionRequestDto {
    private Long id;
    
    @NotNull(message = "Recipient ID is required")
    private Long recipientId;
    
    @NotNull(message = "Requester ID is required")
    private Long requesterId;
    
    @NotBlank(message = "Status is required")
    private String status;
    
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private String requesterName;
    private String requesterProfileImage;
    private String recipientName;
    private String recipientProfileImage;
}
