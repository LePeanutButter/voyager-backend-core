package com.tourism.platform.dto;

/**
 * Lightweight DTO representing a chat message exchanged between travelers.
 * Used by messaging endpoints and WebSocket payloads.
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private Long id;
    private Long connectionId;
    private Long senderId;
    private String senderName;
    private Long recipientId;
    private String content;
    private String status;
    private LocalDateTime createdAt;
    private String type; // MESSAGE, TYPING, READ
}