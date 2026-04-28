package com.tourism.platform.service.impl;

import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.*;
import com.tourism.platform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TravelPlanRepository travelPlanRepository;

    @Mock
    private ConnectionRepository connectionRepository;

    @Mock
    private SharedSpaceAccessRepository sharedSpaceAccessRepository;

    @InjectMocks
    private SocialServiceImpl socialService;

    private User testUser;
    private Connection testConnection;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setBio("Test bio");

        testConnection = new Connection();
        testConnection.setId(1L);
        testConnection.setRequesterId(1L);
        testConnection.setRecipientId(2L);
        testConnection.setStatus(ConnectionStatus.ACCEPTED);
    }

    @Test
    void getTravelerSummary_WithValidUserId_ShouldReturnSummaryDto() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        var result = socialService.getTravelerSummary(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("testuser", result.getDisplayName());
        assertEquals("Test bio", result.getBioShort());
        verify(userRepository).findById(1L);
    }

    @Test
    void getTravelerSummary_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> socialService.getTravelerSummary(999L));
        verify(userRepository).findById(999L);
    }

    @Test
    void getTravelerSummary_WithLongBio_ShouldTruncateTo160Characters() {
        // Given
        String longBio = "a".repeat(200);
        testUser.setBio(longBio);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        var result = socialService.getTravelerSummary(1L);

        // Then
        assertEquals(163, result.getBioShort().length()); // 160 + "..."
        assertTrue(result.getBioShort().endsWith("..."));
    }

    @Test
    void getAcceptedConnectionsByTravelPlan_WithValidPlanId_ShouldReturnConnections() {
        // Given
        when(travelPlanRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findAll()).thenReturn(java.util.List.of(testUser));

        // When
        var result = socialService.getAcceptedConnectionsByTravelPlan(1L);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size()); // Limited to 3 in the implementation
        verify(travelPlanRepository).existsById(1L);
        verify(userRepository).findAll();
    }

    @Test
    void getAcceptedConnectionsByTravelPlan_WithNonExistentPlan_ShouldThrowException() {
        // Given
        when(travelPlanRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> socialService.getAcceptedConnectionsByTravelPlan(999L));
        verify(travelPlanRepository).existsById(999L);
    }

    @Test
    void deleteConnection_WithValidConnectionAndRequester_ShouldDeleteConnection() {
        // Given
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));

        // When
        socialService.deleteConnection(1L, 1L);

        // Then
        verify(messageRepository).deleteByConnectionId(1L);
        verify(sharedSpaceAccessRepository).deleteByConnectionId(1L);
        verify(connectionRepository).deleteById(1L);
    }

    @Test
    void deleteConnection_WithValidConnectionAndRecipient_ShouldDeleteConnection() {
        // Given
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));

        // When
        socialService.deleteConnection(1L, 2L);

        // Then
        verify(messageRepository).deleteByConnectionId(1L);
        verify(sharedSpaceAccessRepository).deleteByConnectionId(1L);
        verify(connectionRepository).deleteById(1L);
    }

    @Test
    void deleteConnection_WithNonExistentConnection_ShouldThrowException() {
        // Given
        when(connectionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> socialService.deleteConnection(999L, 1L));
        verify(connectionRepository).findById(999L);
        verify(messageRepository, never()).deleteByConnectionId(anyLong());
    }

    @Test
    void deleteConnection_WithUnauthorizedUser_ShouldThrowException() {
        // Given
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> socialService.deleteConnection(1L, 3L));
        verify(connectionRepository).findById(1L);
        verify(messageRepository, never()).deleteByConnectionId(anyLong());
    }

    @Test
    void sendMessage_WithValidConnectionAndUser_ShouldReturnMessage() {
        // Given
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Message result = socialService.sendMessage(1L, 1L, "Hello world");

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getConnectionId());
        assertEquals(1L, result.getSenderId());
        assertEquals(2L, result.getRecipientId());
        assertEquals("Hello world", result.getContent());
        assertEquals(MessageStatus.SENT, result.getStatus());
        verify(connectionRepository).findById(1L);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendMessage_WithNonExistentConnection_ShouldThrowException() {
        // Given
        when(connectionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> socialService.sendMessage(999L, 1L, "Hello"));
        verify(connectionRepository).findById(999L);
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendMessage_WithUnauthorizedUser_ShouldThrowException() {
        // Given
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> socialService.sendMessage(1L, 3L, "Hello"));
        verify(connectionRepository).findById(1L);
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendMessage_WithEmptyContent_ShouldThrowException() {
        // Given
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> socialService.sendMessage(1L, 1L, ""));
        verify(connectionRepository).findById(1L);
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendMessage_WithNullContent_ShouldThrowException() {
        // Given
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> socialService.sendMessage(1L, 1L, null));
        verify(connectionRepository).findById(1L);
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void sendMessage_WithPendingConnection_ShouldThrowException() {
        // Given
        testConnection.setStatus(ConnectionStatus.PENDING);
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> socialService.sendMessage(1L, 1L, "Hello"));
        verify(connectionRepository).findById(1L);
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void getConversationMessages_WithValidConnectionAndUser_ShouldReturnMessages() {
        // Given
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));
        when(messageRepository.findBySenderIdAndRecipientIdOrderByCreatedAtDesc(1L, 2L))
                .thenReturn(java.util.List.of());
        when(messageRepository.findBySenderIdAndRecipientIdOrderByCreatedAtDesc(2L, 1L))
                .thenReturn(java.util.List.of());

        // When
        var result = socialService.getConversationMessages(1L, 1L);

        // Then
        assertNotNull(result);
        verify(connectionRepository).findById(1L);
        verify(messageRepository).findBySenderIdAndRecipientIdOrderByCreatedAtDesc(1L, 2L);
        verify(messageRepository).findBySenderIdAndRecipientIdOrderByCreatedAtDesc(2L, 1L);
    }

    @Test
    void getConversationMessages_WithNonExistentConnection_ShouldThrowException() {
        // Given
        when(connectionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> socialService.getConversationMessages(999L, 1L));
        verify(connectionRepository).findById(999L);
    }

    @Test
    void getConversationMessages_WithUnauthorizedUser_ShouldThrowException() {
        // Given
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> socialService.getConversationMessages(1L, 3L));
        verify(connectionRepository).findById(1L);
    }

    @Test
    void markMessageAsRead_WithValidMessageAndRecipient_ShouldMarkAsRead() {
        // Given
        Message message = new Message();
        message.setId(1L);
        message.setRecipientId(1L);
        message.setStatus(MessageStatus.SENT);
        
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        // When
        socialService.markMessageAsRead(1L, 1L);

        // Then
        assertEquals(MessageStatus.READ, message.getStatus());
        verify(messageRepository).findById(1L);
        verify(messageRepository).save(message);
    }

    @Test
    void markMessageAsRead_WithNonExistentMessage_ShouldThrowException() {
        // Given
        when(messageRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> socialService.markMessageAsRead(999L, 1L));
        verify(messageRepository).findById(999L);
    }

    @Test
    void markMessageAsRead_WithUnauthorizedUser_ShouldThrowException() {
        // Given
        Message message = new Message();
        message.setId(1L);
        message.setRecipientId(2L);
        
        when(messageRepository.findById(1L)).thenReturn(Optional.of(message));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> socialService.markMessageAsRead(1L, 1L));
        verify(messageRepository).findById(1L);
        verify(messageRepository, never()).save(any(Message.class));
    }
}
