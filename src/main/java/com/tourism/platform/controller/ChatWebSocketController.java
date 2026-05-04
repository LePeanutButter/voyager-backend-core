package com.tourism.platform.controller;

import org.springframework.lang.NonNull;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.tourism.platform.dto.ChatMessage;
import com.tourism.platform.model.Message;
import com.tourism.platform.service.SocialService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

        private static final String USER_QUEUE_PREFIX = "/queue/user/";

    private final SocialService socialService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{connectionId}/sendMessage")
        /**
         * Handle incoming chat messages from WebSocket clients.
         *
         * Persists the message via the SocialService and broadcasts the saved message
         * to the chat topic and to the recipient's private queue. On failure an error
         * notification is sent back to the sender's private queue.
         *
         * @param connectionId id of the connection the message belongs to
         * @param chatMessage  payload containing senderId, content and metadata
         */
        public void sendMessage(@DestinationVariable Long connectionId, @Payload @NonNull ChatMessage chatMessage) {
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
                    java.util.Objects.requireNonNull(response)
            );

            // Send to specific user queue for private notifications
            messagingTemplate.convertAndSend(
                    USER_QUEUE_PREFIX + savedMessage.getRecipientId(),
                    java.util.Objects.requireNonNull(response)
            );

                } catch (RuntimeException e) {
            // Send error back to sender
            ChatMessage error = ChatMessage.builder()
                    .type("ERROR")
                    .content("Failed to send message: " + e.getMessage())
                    .build();
            messagingTemplate.convertAndSend(
                    USER_QUEUE_PREFIX + chatMessage.getSenderId(),
                    java.util.Objects.requireNonNull(error)
            );
        }
    }

    @MessageMapping("/chat/{connectionId}/typing")
        /**
         * Handle typing indicators sent by clients and broadcast them to the chat topic.
         *
         * @param connectionId id of the connection where typing is occurring
         * @param chatMessage  payload containing senderId
         */
        public void handleTyping(@DestinationVariable Long connectionId, @Payload @NonNull ChatMessage chatMessage) {
        ChatMessage typingNotification = ChatMessage.builder()
                .connectionId(connectionId)
                .senderId(chatMessage.getSenderId())
                .type("TYPING")
                .content(chatMessage.getSenderId() + " is typing...")
                .build();

        // Send typing notification to the other user in the connection
        messagingTemplate.convertAndSend(
                "/topic/chat/" + connectionId + "/typing",
                java.util.Objects.requireNonNull(typingNotification)
        );
    }
}