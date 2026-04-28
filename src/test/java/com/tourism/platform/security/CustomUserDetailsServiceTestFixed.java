package com.tourism.platform.security;

import com.tourism.platform.model.User;
import com.tourism.platform.model.UserRole;
import com.tourism.platform.model.UserStatus;
import com.tourism.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTestFixed {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRole(UserRole.TRAVELER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.isEnabled());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_TRAVELER")));

        verify(userRepository).findByUsernameOrEmail("testuser", "testuser");
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findByUsernameOrEmail("nonexistent", "nonexistent"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("nonexistent"));

        verify(userRepository).findByUsernameOrEmail("nonexistent", "nonexistent");
    }

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExistsByEmail() {
        when(userRepository.findByUsernameOrEmail("test@example.com", "test@example.com"))
                .thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("test@example.com");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.isEnabled());

        verify(userRepository).findByUsernameOrEmail("test@example.com", "test@example.com");
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserDisabled() {
        user.setEnabled(false);
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(user));

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("testuser"));

        verify(userRepository).findByUsernameOrEmail("testuser", "testuser");
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserInactive() {
        user.setStatus(UserStatus.DEACTIVATED);
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(user));

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("testuser"));

        verify(userRepository).findByUsernameOrEmail("testuser", "testuser");
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserSuspended() {
        user.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(user));

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("testuser"));

        verify(userRepository).findByUsernameOrEmail("testuser", "testuser");
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserBanned() {
        user.setStatus(UserStatus.BANNED);
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(user));

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("testuser"));

        verify(userRepository).findByUsernameOrEmail("testuser", "testuser");
    }

    @Test
    void loadUserByUsername_ShouldThrowException_WhenUserPendingVerification() {
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(user));

        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("testuser"));

        verify(userRepository).findByUsernameOrEmail("testuser", "testuser");
    }
}
