package com.tourism.platform.controller;

import com.tourism.platform.dto.ChatMessage;
import com.tourism.platform.model.Message;
import com.tourism.platform.model.MessageStatus;
import com.tourism.platform.service.SocialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock
    private SocialService socialService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatWebSocketController chatWebSocketController;

    private ChatMessage testChatMessage;
    private Message savedMessage;

    @BeforeEach
    void setUp() {
        testChatMessage = ChatMessage.builder()
                .connectionId(1L)
                .senderId(1L)
                .content("Hello world")
                .type("MESSAGE")
                .build();

        savedMessage = new Message();
        savedMessage.setId(100L);
        savedMessage.setConnectionId(1L);
        savedMessage.setSenderId(1L);
        savedMessage.setRecipientId(2L);
        savedMessage.setContent("Hello world");
        savedMessage.setStatus(MessageStatus.SENT);
        savedMessage.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void sendMessage_WithValidMessage_ShouldSendToTopicAndQueue() {
        // Given
        when(socialService.sendMessage(eq(1L), eq(1L), eq("Hello world")))
                .thenReturn(savedMessage);

        // When
        chatWebSocketController.sendMessage(1L, testChatMessage);

        // Then
        verify(socialService).sendMessage(1L, 1L, "Hello world");
        
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/1"), any(ChatMessage.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/queue/user/2"), any(ChatMessage.class));
    }

    @Test
    void sendMessage_WithException_ShouldSendErrorToSender() {
        // Given
        when(socialService.sendMessage(anyLong(), anyLong(), anyString()))
                .thenThrow(new RuntimeException("Connection not found"));

        // When
        chatWebSocketController.sendMessage(1L, testChatMessage);

        // Then
        verify(socialService).sendMessage(1L, 1L, "Hello world");
        
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/chat/1"), any(ChatMessage.class));
        
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/queue/user/1"),
                any(ChatMessage.class)
        );
    }

    @Test
    void sendMessage_WithIllegalArgumentException_ShouldSendErrorToSender() {
        // Given
        when(socialService.sendMessage(anyLong(), anyLong(), anyString()))
                .thenThrow(new IllegalArgumentException("User not authorized"));

        // When
        chatWebSocketController.sendMessage(1L, testChatMessage);

        // Then
        verify(socialService).sendMessage(1L, 1L, "Hello world");
        
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/queue/user/1"),
                any(ChatMessage.class)
        );
    }

    @Test
    void handleTyping_ShouldSendTypingNotification() {
        // Given
        ChatMessage typingMessage = ChatMessage.builder()
                .connectionId(1L)
                .senderId(1L)
                .type("TYPING")
                .build();

        // When
        chatWebSocketController.handleTyping(1L, typingMessage);

        // Then
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/chat/1/typing"),
                any(ChatMessage.class)
        );
    }

    @Test
    void handleTyping_WithDifferentSenderId_ShouldUseCorrectSender() {
        // Given
        ChatMessage typingMessage = ChatMessage.builder()
                .connectionId(2L)
                .senderId(5L)
                .type("TYPING")
                .build();

        // When
        chatWebSocketController.handleTyping(2L, typingMessage);

        // Then
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/chat/2/typing"),
                any(ChatMessage.class)
        );
    }

    @Test
    void sendMessage_WithNullRecipientId_ShouldStillWork() {
        // Given
        Message messageWithNullRecipient = new Message();
        messageWithNullRecipient.setId(100L);
        messageWithNullRecipient.setConnectionId(1L);
        messageWithNullRecipient.setSenderId(1L);
        messageWithNullRecipient.setRecipientId(null);
        messageWithNullRecipient.setContent("Hello world");
        messageWithNullRecipient.setStatus(MessageStatus.SENT);
        messageWithNullRecipient.setCreatedAt(LocalDateTime.now());

        when(socialService.sendMessage(eq(1L), eq(1L), eq("Hello world")))
                .thenReturn(messageWithNullRecipient);

        // When
        chatWebSocketController.sendMessage(1L, testChatMessage);

        // Then
        verify(socialService).sendMessage(1L, 1L, "Hello world");
        
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/chat/1"),
                any(ChatMessage.class)
        );
    }
}
