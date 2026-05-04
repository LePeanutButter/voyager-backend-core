package com.tourism.platform.security;

import com.tourism.platform.model.User;
import com.tourism.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
    }

    @Test
    void loadUserByUsername_WithValidUsername_ShouldReturnUserDetails() {
        // Given
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername("testuser");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertFalse(result.getAuthorities().isEmpty()); // User has one authority based on their role

        verify(userRepository).findByUsernameOrEmail("testuser", "testuser");
    }

    @Test
    void loadUserByUsername_WithValidEmail_ShouldReturnUserDetails() {
        // Given
        when(userRepository.findByUsernameOrEmail("test@example.com", "test@example.com"))
                .thenReturn(Optional.of(testUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername("test@example.com");

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());

        verify(userRepository).findByUsernameOrEmail("test@example.com", "test@example.com");
    }

    @Test
    void loadUserByUsername_WithNonExistentUser_ShouldThrowUsernameNotFoundException() {
        // Given
        when(userRepository.findByUsernameOrEmail("nonexistent", "nonexistent"))
                .thenReturn(Optional.empty());

        // When & Then
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("nonexistent"));

        assertEquals("User not found with username or email: nonexistent", exception.getMessage());

        verify(userRepository).findByUsernameOrEmail("nonexistent", "nonexistent");
    }

    @Test
    void loadUserByUsername_WithNullUsername_ShouldThrowUsernameNotFoundException() {
        // Given
        when(userRepository.findByUsernameOrEmail(isNull(), isNull()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(null));

        verify(userRepository).findByUsernameOrEmail(isNull(), isNull());
    }

    @Test
    void loadUserByUsername_WithEmptyUsername_ShouldThrowUsernameNotFoundException() {
        // Given
        when(userRepository.findByUsernameOrEmail("", ""))
                .thenReturn(Optional.empty());

        // When & Then
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername(""));

        assertEquals("User not found with username or email: ", exception.getMessage());

        verify(userRepository).findByUsernameOrEmail("", "");
    }

    @Test
    void loadUserById_WithValidId_ShouldReturnUserDetails() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserDetails result = customUserDetailsService.loadUserById(1L);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());

        verify(userRepository).findById(1L);
    }

    @Test
    void loadUserById_WithNonExistentId_ShouldThrowUsernameNotFoundException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserById(999L));

        assertEquals("User not found with ID: 999", exception.getMessage());

        verify(userRepository).findById(999L);
    }

    @Test
    void loadUserById_WithNullId_ShouldThrowUsernameNotFoundException() {
        // Given
        when(userRepository.findById(Objects.requireNonNull(isNull()))).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserById(Objects.requireNonNull(null)));

        verify(userRepository).findById(Objects.requireNonNull(isNull()));
    }

    @Test
    void loadUserByUsername_WithRepositoryException_ShouldPropagateException() {
        // Given
        when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> customUserDetailsService.loadUserByUsername("testuser"));

        verify(userRepository).findByUsernameOrEmail("testuser", "testuser");
    }

    @Test
    void loadUserById_WithRepositoryException_ShouldPropagateException() {
        // Given
        when(userRepository.findById(anyLong()))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> customUserDetailsService.loadUserById(1L));

        verify(userRepository).findById(1L);
    }

    @Test
    void loadUserByUsername_WithDifferentCase_ShouldWorkCorrectly() {
        // Given
        User upperCaseUser = new User();
        upperCaseUser.setId(2L);
        upperCaseUser.setUsername("TestUser");
        upperCaseUser.setEmail("TEST@EXAMPLE.COM");
        upperCaseUser.setPassword("encodedPassword");

        when(userRepository.findByUsernameOrEmail("TestUser", "TestUser"))
                .thenReturn(Optional.of(upperCaseUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername("TestUser");

        // Then
        assertNotNull(result);
        assertEquals("TestUser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());

        verify(userRepository).findByUsernameOrEmail("TestUser", "TestUser");
    }

    @Test
    void loadUserById_WithDifferentIds_ShouldWorkCorrectly() {
        // Given
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setPassword("password1");

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setPassword("password2");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));

        // When
        UserDetails result1 = customUserDetailsService.loadUserById(1L);
        UserDetails result2 = customUserDetailsService.loadUserById(2L);

        // Then
        assertEquals("user1", result1.getUsername());
        assertEquals("password1", result1.getPassword());
        assertEquals("user2", result2.getUsername());
        assertEquals("password2", result2.getPassword());

        verify(userRepository).findById(1L);
        verify(userRepository).findById(2L);
    }

    @Test
    void loadUserByUsername_WithUserHavingAllFields_ShouldReturnCompleteUserDetails() {
        // Given
        User completeUser = new User();
        completeUser.setId(1L);
        completeUser.setUsername("completeuser");
        completeUser.setEmail("complete@example.com");
        completeUser.setPassword("encodedPassword");
        completeUser.setFirstName("Complete");
        completeUser.setLastName("User");
        completeUser.setBio("Complete user bio");
        completeUser.setProfileImageUrl("http://example.com/profile.jpg");

        when(userRepository.findByUsernameOrEmail("completeuser", "completeuser"))
                .thenReturn(Optional.of(completeUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername("completeuser");

        // Then
        assertNotNull(result);
        assertEquals("completeuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());

        verify(userRepository).findByUsernameOrEmail("completeuser", "completeuser");
    }
}
