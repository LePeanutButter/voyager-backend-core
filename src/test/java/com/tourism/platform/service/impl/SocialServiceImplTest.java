package com.tourism.platform.service.impl;

import com.tourism.platform.dto.ConnectionRequestDto;
import com.tourism.platform.dto.SendConnectionRequestDto;
import com.tourism.platform.dto.TravelConnectionDto;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.Connection;
import com.tourism.platform.model.ConnectionStatus;
import com.tourism.platform.model.Message;
import com.tourism.platform.model.MessageStatus;
import com.tourism.platform.model.User;
import com.tourism.platform.repository.ConnectionRepository;
import com.tourism.platform.repository.MessageRepository;
import com.tourism.platform.repository.SharedSpaceAccessRepository;
import com.tourism.platform.repository.TravelPlanRepository;
import com.tourism.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.params.provider.Arguments.arguments;

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
        testUser.setFirstName("Test");
        testUser.setLastName("User");
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
        assertEquals("Test User", result.getDisplayName()); // firstName + lastName
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
        assertEquals(1, result.size()); // Returns 1 user from the mock
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

    @ParameterizedTest
    @MethodSource("invalidSendMessageInputs")
    void sendMessage_WithInvalidInput_ShouldThrowException(Long senderId, String content) {
        // Given
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> socialService.sendMessage(1L, senderId, content));
        verify(connectionRepository).findById(1L);
        verify(messageRepository, never()).save(any(Message.class));
    }

    private static Stream<Arguments> invalidSendMessageInputs() {
        return Stream.of(
                arguments(3L, "Hello"),
                arguments(1L, ""),
                arguments(1L, null)
        );
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
        verify(messageRepository).save(Objects.requireNonNull(message));
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

    // ── sendConnectionRequest ─────────────────────────────────────────────────────

    @Test
    void sendConnectionRequest_WithValidRequest_ShouldCreateConnection() {
        // Given
        SendConnectionRequestDto request = new SendConnectionRequestDto();
        request.setRecipientId(2L);
        request.setMessage("Let's connect!");
        
        User recipient = new User();
        recipient.setId(2L);
        recipient.setFirstName("Jane");
        recipient.setLastName("Doe");
        
        when(userRepository.existsById(2L)).thenReturn(true);
        when(connectionRepository.findByRequesterIdAndRecipientId(1L, 2L)).thenReturn(Optional.empty());
        when(connectionRepository.findByRequesterIdAndRecipientId(2L, 1L)).thenReturn(Optional.empty());
        when(connectionRepository.save(any(Connection.class))).thenAnswer(invocation -> {
            Connection conn = invocation.getArgument(0);
            conn.setId(1L);
            conn.setCreatedAt(LocalDateTime.now());
            conn.setUpdatedAt(LocalDateTime.now());
            return conn;
        });
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));

        // When
        ConnectionRequestDto result = socialService.sendConnectionRequest(request, 1L);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getRecipientId());
        assertEquals(1L, result.getRequesterId());
        assertEquals("PENDING", result.getStatus());
        assertEquals("Let's connect!", result.getMessage());
        verify(userRepository).existsById(2L);
        verify(connectionRepository).save(any(Connection.class));
    }

    @Test
    void sendConnectionRequest_WithNonExistentRecipient_ShouldThrowException() {
        // Given
        SendConnectionRequestDto request = new SendConnectionRequestDto();
        request.setRecipientId(999L);
        
        when(userRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> socialService.sendConnectionRequest(request, 1L));
        verify(userRepository).existsById(999L);
        verify(connectionRepository, never()).save(any(Connection.class));
    }

    @Test
    void sendConnectionRequest_ToSelf_ShouldThrowException() {
        // Given
        SendConnectionRequestDto request = new SendConnectionRequestDto();
        request.setRecipientId(1L);
        
        when(userRepository.existsById(1L)).thenReturn(true);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> socialService.sendConnectionRequest(request, 1L));
        verify(userRepository).existsById(1L);
        verify(connectionRepository, never()).save(any(Connection.class));
    }

    @Test
    void sendConnectionRequest_WithExistingPendingConnection_ShouldThrowException() {
        // Given
        SendConnectionRequestDto request = new SendConnectionRequestDto();
        request.setRecipientId(2L);
        
        Connection existingConnection = new Connection();
        existingConnection.setStatus(ConnectionStatus.PENDING);
        
        when(userRepository.existsById(2L)).thenReturn(true);
        when(connectionRepository.findByRequesterIdAndRecipientId(1L, 2L))
                .thenReturn(Optional.of(existingConnection));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> socialService.sendConnectionRequest(request, 1L));
        verify(userRepository).existsById(2L);
        verify(connectionRepository, never()).save(any(Connection.class));
    }

    @Test
    void sendConnectionRequest_WithExistingAcceptedConnection_ShouldThrowException() {
        // Given
        SendConnectionRequestDto request = new SendConnectionRequestDto();
        request.setRecipientId(2L);
        
        Connection existingConnection = new Connection();
        existingConnection.setStatus(ConnectionStatus.ACCEPTED);
        
        when(userRepository.existsById(2L)).thenReturn(true);
        when(connectionRepository.findByRequesterIdAndRecipientId(1L, 2L))
                .thenReturn(Optional.of(existingConnection));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> socialService.sendConnectionRequest(request, 1L));
        verify(userRepository).existsById(2L);
        verify(connectionRepository, never()).save(any(Connection.class));
    }

    @Test
    void sendConnectionRequest_WithRejectedConnection_ShouldCreateNewRequest() {
        // Given
        SendConnectionRequestDto request = new SendConnectionRequestDto();
        request.setRecipientId(2L);
        request.setMessage("Let's try again!");
        
        Connection existingConnection = new Connection();
        existingConnection.setId(1L);
        existingConnection.setRequesterId(1L);
        existingConnection.setRecipientId(2L);
        existingConnection.setStatus(ConnectionStatus.REJECTED);
        existingConnection.setCreatedAt(LocalDateTime.now());
        existingConnection.setUpdatedAt(LocalDateTime.now());
        
        when(userRepository.existsById(2L)).thenReturn(true);
        when(connectionRepository.findByRequesterIdAndRecipientId(1L, 2L))
                .thenReturn(Optional.of(existingConnection));
        when(connectionRepository.save(any(Connection.class))).thenReturn(existingConnection);
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));

        // When
        ConnectionRequestDto result = socialService.sendConnectionRequest(request, 1L);

        // Then
        assertNotNull(result);
        assertEquals(2L, result.getRecipientId());
        assertEquals(1L, result.getRequesterId());
        verify(connectionRepository).save(existingConnection);
    }

    // ── acceptConnectionRequest ───────────────────────────────────────────────────

    @Test
    void acceptConnectionRequest_WithValidRequest_ShouldAcceptConnection() {
        // Given
        Connection connection = new Connection();
        connection.setId(1L);
        connection.setRequesterId(1L);
        connection.setRecipientId(2L);
        connection.setStatus(ConnectionStatus.PENDING);
        
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(connection));
        when(connectionRepository.save(any(Connection.class))).thenReturn(connection);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

        // When
        ConnectionRequestDto result = socialService.acceptConnectionRequest(1L, 2L);

        // Then
        assertNotNull(result);
        assertEquals(ConnectionStatus.ACCEPTED.toString(), result.getStatus());
        verify(connectionRepository).save(Objects.requireNonNull(connection));
    }

    @Test
    void acceptConnectionRequest_WithNonExistentRequest_ShouldThrowException() {
        // Given
        when(connectionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> socialService.acceptConnectionRequest(999L, 2L));
        verify(connectionRepository).findById(999L);
        verify(connectionRepository, never()).save(any(Connection.class));
    }

    @Test
    void acceptConnectionRequest_WithUnauthorizedUser_ShouldThrowException() {
        // Given
        Connection connection = new Connection();
        connection.setId(1L);
        connection.setRequesterId(1L);
        connection.setRecipientId(2L);
        
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(connection));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> socialService.acceptConnectionRequest(1L, 3L));
        verify(connectionRepository).findById(1L);
        verify(connectionRepository, never()).save(any(Connection.class));
    }

    @Test
    void acceptConnectionRequest_WithNonPendingRequest_ShouldThrowException() {
        // Given
        Connection connection = new Connection();
        connection.setId(1L);
        connection.setRequesterId(1L);
        connection.setRecipientId(2L);
        connection.setStatus(ConnectionStatus.ACCEPTED);
        
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(connection));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> socialService.acceptConnectionRequest(1L, 2L));
        verify(connectionRepository).findById(1L);
        verify(connectionRepository, never()).save(any(Connection.class));
    }

    // ── rejectConnectionRequest ───────────────────────────────────────────────────

    @Test
    void rejectConnectionRequest_WithValidRequest_ShouldRejectConnection() {
        // Given
        Connection connection = new Connection();
        connection.setId(1L);
        connection.setRequesterId(1L);
        connection.setRecipientId(2L);
        connection.setStatus(ConnectionStatus.PENDING);
        
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(connection));
        when(connectionRepository.save(any(Connection.class))).thenReturn(connection);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

        // When
        ConnectionRequestDto result = socialService.rejectConnectionRequest(1L, 2L);

        // Then
        assertNotNull(result);
        assertEquals(ConnectionStatus.REJECTED.toString(), result.getStatus());
        verify(connectionRepository).save(Objects.requireNonNull(connection));
    }

    @Test
    void rejectConnectionRequest_WithNonExistentRequest_ShouldThrowException() {
        // Given
        when(connectionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> socialService.rejectConnectionRequest(999L, 2L));
        verify(connectionRepository).findById(999L);
        verify(connectionRepository, never()).save(any(Connection.class));
    }

    @Test
    void rejectConnectionRequest_WithUnauthorizedUser_ShouldThrowException() {
        // Given
        Connection connection = new Connection();
        connection.setId(1L);
        connection.setRequesterId(1L);
        connection.setRecipientId(2L);
        
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(connection));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> socialService.rejectConnectionRequest(1L, 3L));
        verify(connectionRepository).findById(1L);
        verify(connectionRepository, never()).save(any(Connection.class));
    }

    // ── getPendingRequestsForUser ───────────────────────────────────────────────

    @Test
    void getPendingRequestsForUser_WithValidUser_ShouldReturnPendingRequests() {
        // Given
        Connection connection1 = new Connection();
        connection1.setId(1L);
        connection1.setRequesterId(1L);
        connection1.setRecipientId(2L);
        connection1.setStatus(ConnectionStatus.PENDING);
        
        Connection connection2 = new Connection();
        connection2.setId(2L);
        connection2.setRequesterId(3L);
        connection2.setRecipientId(2L);
        connection2.setStatus(ConnectionStatus.PENDING);
        
        when(connectionRepository.findByRecipientIdAndStatus(2L, ConnectionStatus.PENDING))
                .thenReturn(Objects.requireNonNull(List.of(connection1, connection2)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));

        // When
        List<ConnectionRequestDto> result = socialService.getPendingRequestsForUser(2L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(ConnectionStatus.PENDING.toString(), result.get(0).getStatus());
        assertEquals(ConnectionStatus.PENDING.toString(), result.get(1).getStatus());
        verify(connectionRepository).findByRecipientIdAndStatus(2L, ConnectionStatus.PENDING);
    }

    @Test
    void getPendingRequestsForUser_WithNoPendingRequests_ShouldReturnEmptyList() {
        // Given
        when(connectionRepository.findByRecipientIdAndStatus(2L, ConnectionStatus.PENDING))
                .thenReturn(List.of());

        // When
        List<ConnectionRequestDto> result = socialService.getPendingRequestsForUser(2L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(connectionRepository).findByRecipientIdAndStatus(2L, ConnectionStatus.PENDING);
    }

    // ── getSentRequestsForUser ───────────────────────────────────────────────────

    @Test
    void getSentRequestsForUser_WithValidUser_ShouldReturnSentRequests() {
        // Given
        Connection connection1 = new Connection();
        connection1.setId(1L);
        connection1.setRequesterId(1L);
        connection1.setRecipientId(2L);
        connection1.setStatus(ConnectionStatus.PENDING);
        
        Connection connection2 = new Connection();
        connection2.setId(2L);
        connection2.setRequesterId(1L);
        connection2.setRecipientId(3L);
        connection2.setStatus(ConnectionStatus.PENDING);
        
        when(connectionRepository.findByRequesterIdAndStatus(1L, ConnectionStatus.PENDING))
                .thenReturn(Objects.requireNonNull(List.of(connection1, connection2)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(testUser));

        // When
        List<ConnectionRequestDto> result = socialService.getSentRequestsForUser(1L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(ConnectionStatus.PENDING.toString(), result.get(0).getStatus());
        assertEquals(ConnectionStatus.PENDING.toString(), result.get(1).getStatus());
        verify(connectionRepository).findByRequesterIdAndStatus(1L, ConnectionStatus.PENDING);
    }

    @Test
    void getSentRequestsForUser_WithNoSentRequests_ShouldReturnEmptyList() {
        // Given
        when(connectionRepository.findByRequesterIdAndStatus(1L, ConnectionStatus.PENDING))
                .thenReturn(List.of());

        // When
        List<ConnectionRequestDto> result = socialService.getSentRequestsForUser(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(connectionRepository).findByRequesterIdAndStatus(1L, ConnectionStatus.PENDING);
    }

    // ── getUserConnections ─────────────────────────────────────────────────────

    @Test
    void getUserConnections_WithValidUser_ShouldReturnConnections() {
        // Given
        Connection connection1 = new Connection();
        connection1.setId(1L);
        connection1.setRequesterId(1L);
        connection1.setRecipientId(2L);
        connection1.setStatus(ConnectionStatus.ACCEPTED);
        
        Connection connection2 = new Connection();
        connection2.setId(2L);
        connection2.setRequesterId(3L);
        connection2.setRecipientId(1L);
        connection2.setStatus(ConnectionStatus.ACCEPTED);
        
        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setFirstName("User");
        user2.setLastName("Two");
        
        User user3 = new User();
        user3.setId(3L);
        user3.setUsername("user3");
        user3.setFirstName("User");
        user3.setLastName("Three");
        
        when(connectionRepository.findByRequesterIdAndStatus(1L, ConnectionStatus.ACCEPTED))
                .thenReturn(Objects.requireNonNull(new java.util.ArrayList<>(java.util.List.of(connection1))));
        when(connectionRepository.findByRecipientIdAndStatus(1L, ConnectionStatus.ACCEPTED))
                .thenReturn(Objects.requireNonNull(new java.util.ArrayList<>(java.util.List.of(connection2))));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(user3));

        // When
        List<TravelConnectionDto> result = socialService.getUserConnections(1L);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getUserId());
        assertEquals("user2", result.get(0).getUsername());
        assertEquals(3L, result.get(1).getUserId());
        assertEquals("user3", result.get(1).getUsername());
        verify(connectionRepository).findByRequesterIdAndStatus(1L, ConnectionStatus.ACCEPTED);
        verify(connectionRepository).findByRecipientIdAndStatus(1L, ConnectionStatus.ACCEPTED);
    }

    @Test
    void getUserConnections_WithNoConnections_ShouldReturnEmptyList() {
        // Given
        when(connectionRepository.findByRequesterIdAndStatus(1L, ConnectionStatus.ACCEPTED))
                .thenReturn(Objects.requireNonNull(new java.util.ArrayList<>()));
        when(connectionRepository.findByRecipientIdAndStatus(1L, ConnectionStatus.ACCEPTED))
                .thenReturn(Objects.requireNonNull(new java.util.ArrayList<>()));

        // When
        List<TravelConnectionDto> result = socialService.getUserConnections(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(connectionRepository).findByRequesterIdAndStatus(1L, ConnectionStatus.ACCEPTED);
        verify(connectionRepository).findByRecipientIdAndStatus(1L, ConnectionStatus.ACCEPTED);
    }

    // ── getConversationMessagesPaginated ───────────────────────────────────────────

    @Test
    void getConversationMessagesPaginated_WithValidConnectionAndUser_ShouldReturnPage() {
        Message message1 = new Message();
        message1.setId(1L);
        message1.setConnectionId(1L);
        message1.setSenderId(1L);
        message1.setRecipientId(2L);
        message1.setContent("Hello");

        Message message2 = new Message();
        message2.setId(2L);
        message2.setConnectionId(1L);
        message2.setSenderId(2L);
        message2.setRecipientId(1L);
        message2.setContent("Hi");

        Page<Message> expectedPage = new PageImpl<>(List.of(message1, message2), PageRequest.of(0, 10), 2);
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));
        when(messageRepository.findConversationMessages(eq(1L), any(Pageable.class))).thenReturn(expectedPage);

        Page<Message> result = socialService.getConversationMessagesPaginated(1L, 1L, 0, 10);

        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        verify(connectionRepository).findById(1L);
        verify(messageRepository).findConversationMessages(eq(1L), any(Pageable.class));
    }

    @Test
    void getConversationMessagesPaginated_WithNonExistentConnection_ShouldThrowException() {
        // Given
        when(connectionRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, 
                () -> socialService.getConversationMessagesPaginated(999L, 1L, 0, 10));
        verify(connectionRepository).findById(999L);
        verify(messageRepository, never()).findConversationMessages(anyLong(), any(Pageable.class));
    }

    @Test
    void getConversationMessagesPaginated_WithUnauthorizedUser_ShouldThrowException() {
        // Given
        when(connectionRepository.findById(1L)).thenReturn(Optional.of(testConnection));

        // When & Then
        assertThrows(IllegalArgumentException.class, 
                () -> socialService.getConversationMessagesPaginated(1L, 3L, 0, 10));
        verify(connectionRepository).findById(1L);
        verify(messageRepository, never()).findConversationMessages(anyLong(), any(Pageable.class));
    }
}
