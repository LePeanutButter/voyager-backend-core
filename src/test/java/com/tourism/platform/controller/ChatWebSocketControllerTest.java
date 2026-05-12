package com.tourism.platform.controller;

import java.time.LocalDateTime;
import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.tourism.platform.dto.ChatMessage;
import com.tourism.platform.model.Message;
import com.tourism.platform.model.MessageStatus;
import com.tourism.platform.model.User;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.service.SocialService;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock
    private SocialService socialService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Principal principal;

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

        User authUser = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .password("encoded")
                .firstName("Alice")
                .lastName("Doe")
                .build();
        lenient().when(principal.getName()).thenReturn("alice");
        lenient().when(userRepository.findByUsernameOrEmail("alice", "alice")).thenReturn(Optional.of(authUser));
    }

    @Test
    @SuppressWarnings("null")
    void sendMessageWithValidMessageShouldSendToTopicAndQueue() {
        // Given
        when(socialService.sendMessage(1L, 1L, "Hello world"))
                .thenReturn(savedMessage);

        // When
        chatWebSocketController.sendMessage(1L, testChatMessage, principal);

        // Then
        verify(socialService).sendMessage(1L, 1L, "Hello world");
        
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/1"), isA(ChatMessage.class));
        verify(messagingTemplate, times(1)).convertAndSend(eq("/queue/user/2"), isA(ChatMessage.class));
    }

    @Test
    @SuppressWarnings("null")
    void sendMessageWithExceptionShouldSendErrorToSender() {
        // Given
        when(socialService.sendMessage(anyLong(), anyLong(), anyString()))
                .thenThrow(new RuntimeException("Connection not found"));

        // When
        chatWebSocketController.sendMessage(1L, testChatMessage, principal);

        // Then
        verify(socialService).sendMessage(1L, 1L, "Hello world");
        
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/chat/1"), isA(ChatMessage.class));
        
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/queue/user/1"),
                isA(ChatMessage.class)
        );
    }

    @Test
    @SuppressWarnings("null")
    void sendMessageWithIllegalArgumentExceptionShouldSendErrorToSender() {
        // Given
        when(socialService.sendMessage(anyLong(), anyLong(), anyString()))
                .thenThrow(new IllegalArgumentException("User not authorized"));

        // When
        chatWebSocketController.sendMessage(1L, testChatMessage, principal);

        // Then
        verify(socialService).sendMessage(1L, 1L, "Hello world");
        
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/queue/user/1"),
                isA(ChatMessage.class)
        );
    }

    @Test
    @SuppressWarnings("null")
    void handleTypingShouldSendTypingNotification() {
        // Given
        ChatMessage typingMessage = ChatMessage.builder()
                .connectionId(1L)
                .senderId(1L)
                .type("TYPING")
                .build();

        // When
        chatWebSocketController.handleTyping(1L, typingMessage, principal);

        // Then
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/chat/1/typing"),
                isA(ChatMessage.class)
        );
    }

    @Test
    @SuppressWarnings("null")
    void handleTypingWithDifferentSenderIdShouldUseCorrectSender() {
        // Given
        ChatMessage typingMessage = ChatMessage.builder()
                .connectionId(2L)
                .senderId(5L)
                .type("TYPING")
                .build();

        // When
        chatWebSocketController.handleTyping(2L, typingMessage, principal);

        // Then
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/chat/2/typing"),
                isA(ChatMessage.class)
        );
    }

    @Test
    @SuppressWarnings("null")
    void sendMessageWithNullRecipientIdShouldStillWork() {
        // Given
        Message messageWithNullRecipient = new Message();
        messageWithNullRecipient.setId(100L);
        messageWithNullRecipient.setConnectionId(1L);
        messageWithNullRecipient.setSenderId(1L);
        messageWithNullRecipient.setRecipientId(null);
        messageWithNullRecipient.setContent("Hello world");
        messageWithNullRecipient.setStatus(MessageStatus.SENT);
        messageWithNullRecipient.setCreatedAt(LocalDateTime.now());

        when(socialService.sendMessage(1L, 1L, "Hello world"))
                .thenReturn(messageWithNullRecipient);

        // When
        chatWebSocketController.sendMessage(1L, testChatMessage, principal);

        // Then
        verify(socialService).sendMessage(1L, 1L, "Hello world");
        
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/chat/1"),
                isA(ChatMessage.class)
        );
    }

    @Test
    @SuppressWarnings("null")
    void sendMessageWithNullPrincipalPropagatesFromErrorHandler() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                chatWebSocketController.sendMessage(1L, testChatMessage, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WebSocket principal is required");
    }

    @Test
    @SuppressWarnings("null")
    void sendMessageWithBlankPrincipalNameThrows() {
        when(principal.getName()).thenReturn("   ");

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                chatWebSocketController.sendMessage(1L, testChatMessage, principal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WebSocket principal is required");
    }

    @Test
    @SuppressWarnings("null")
    void sendMessageWhenUserNotFoundThrows() {
        when(principal.getName()).thenReturn("ghost");
        when(userRepository.findByUsernameOrEmail("ghost", "ghost")).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                chatWebSocketController.sendMessage(1L, testChatMessage, principal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Authenticated user not found");
    }

    @Test
    @SuppressWarnings("null")
    void resolvesUserByEmailWhenPrincipalNameIsEmail() {
        User byEmail = User.builder()
                .id(42L)
                .username("alice")
                .email("alice@example.com")
                .password("p")
                .firstName("A")
                .lastName("B")
                .build();
        when(principal.getName()).thenReturn("alice@example.com");
        when(userRepository.findByUsernameOrEmail("alice@example.com", "alice@example.com"))
                .thenReturn(Optional.of(byEmail));
        when(socialService.sendMessage(1L, 42L, "Hello world")).thenReturn(savedMessage);

        chatWebSocketController.sendMessage(1L, testChatMessage, principal);

        verify(socialService).sendMessage(1L, 42L, "Hello world");
    }
}
