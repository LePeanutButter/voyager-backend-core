package com.tourism.platform.controller;

import com.tourism.platform.dto.ChatMessage;
import com.tourism.platform.model.Message;
import com.tourism.platform.service.SocialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final SocialService socialService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{connectionId}/sendMessage")
    public void sendMessage(@DestinationVariable Long connectionId, @Payload ChatMessage chatMessage) {
        try {
            // Save message to database
            Message savedMessage = socialService.sendMessage(
                    connectionId,
                    chatMessage.getSenderId(),
                    chatMessage.getContent()
            );

            // Convert to chat message
            ChatMessage response = ChatMessage.builder()
                    .id(savedMessage.getId())
                    .connectionId(savedMessage.getConnectionId())
                    .senderId(savedMessage.getSenderId())
                    .recipientId(savedMessage.getRecipientId())
                    .content(savedMessage.getContent())
                    .status(savedMessage.getStatus().toString())
                    .createdAt(savedMessage.getCreatedAt())
                    .type("MESSAGE")
                    .build();

            // Send to specific connection topic
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + connectionId,
                    response
            );

            // Send to specific user queue for private notifications
            messagingTemplate.convertAndSend(
                    "/queue/user/" + savedMessage.getRecipientId(),
                    response
            );

        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage());
            // Send error back to sender
            ChatMessage error = ChatMessage.builder()
                    .type("ERROR")
                    .content("Failed to send message: " + e.getMessage())
                    .build();
            messagingTemplate.convertAndSend(
                    "/queue/user/" + chatMessage.getSenderId(),
                    error
            );
        }
    }

    @MessageMapping("/chat/{connectionId}/typing")
    public void handleTyping(@DestinationVariable Long connectionId, @Payload ChatMessage chatMessage) {
        ChatMessage typingNotification = ChatMessage.builder()
                .connectionId(connectionId)
                .senderId(chatMessage.getSenderId())
                .type("TYPING")
                .content(chatMessage.getSenderId() + " is typing...")
                .build();

        // Send typing notification to the other user in the connection
        messagingTemplate.convertAndSend(
                "/topic/chat/" + connectionId + "/typing",
                typingNotification
        );
    }
}