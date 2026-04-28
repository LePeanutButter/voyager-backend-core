package com.tourism.platform.service.impl;

import com.tourism.platform.dto.UserDto;
import com.tourism.platform.dto.UserUpdateDto;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.User;
import com.tourism.platform.model.UserRole;
import com.tourism.platform.model.UserStatus;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserUpdateDto updateDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setPassword("encodedPassword");
        testUser.setRole(UserRole.TRAVELER);
        testUser.setStatus(UserStatus.ACTIVE);

        updateDto = new UserUpdateDto();
        updateDto.setFirstName("Updated");
        updateDto.setLastName("Name");
        updateDto.setBio("Updated bio");
    }

    @Test
    void getUserById_WhenUserExists_ReturnsUserDto() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<UserDto> result = userService.getUserById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        assertEquals("test@example.com", result.get().getEmail());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_WhenUserNotFound_ReturnsEmpty() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<UserDto> result = userService.getUserById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findById(999L);
    }

    @Test
    void getUserByEmail_WhenUserExists_ReturnsUserDto() {
        // Given
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<UserDto> result = userService.getUserByEmail("test@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void getUserByEmail_WhenUserNotFound_ReturnsEmpty() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        Optional<UserDto> result = userService.getUserByEmail("nonexistent@example.com");

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void updateUser_WhenUserExistsAndValidData_ReturnsUpdatedUserDto() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        Optional<UserDto> result = userService.updateUser(1L, updateDto);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Updated", testUser.getFirstName());
        assertEquals("Name", testUser.getLastName());
        assertEquals("Updated bio", testUser.getBio());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUser_WhenUserNotFound_ReturnsEmpty() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<UserDto> result = userService.updateUser(999L, updateDto);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WhenFirstNameIsEmpty_ThrowsIllegalArgumentException() {
        // Given
        updateDto.setFirstName("");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(1L, updateDto));
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WhenBioIsEmpty_ThrowsIllegalArgumentException() {
        // Given
        updateDto.setBio("");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(1L, updateDto));
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WithNullFields_UpdatesOnlyNonNullFields() {
        // Given
        updateDto.setLastName(null);
        updateDto.setPhoneNumber(null);
        updateDto.setProfileImageUrl(null);
        updateDto.setInterests(null);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        Optional<UserDto> result = userService.updateUser(1L, updateDto);

        // Then
        assertTrue(result.isPresent());
        assertEquals("Updated", testUser.getFirstName());
        assertEquals("Updated bio", testUser.getBio());
        // These should remain unchanged
        assertEquals("User", testUser.getLastName());
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_WithValidCurrentPassword_ReturnsTrue() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("currentPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("newEncodedPassword");

        // When
        boolean result = userService.changePassword(1L, "currentPassword", "newPassword");

        // Then
        assertTrue(result);
        assertEquals("newEncodedPassword", testUser.getPassword());
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_WithInvalidCurrentPassword_ReturnsFalse() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // When
        boolean result = userService.changePassword(1L, "wrongPassword", "newPassword");

        // Then
        assertFalse(result);
        assertEquals("encodedPassword", testUser.getPassword()); // Password should remain unchanged
        verify(userRepository, never()).save(testUser);
    }

    @Test
    void changePassword_WhenUserNotFound_ReturnsFalse() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        boolean result = userService.changePassword(999L, "currentPassword", "newPassword");

        // Then
        assertFalse(result);
        verify(userRepository).findById(999L);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserRole_WhenUserExists_ReturnsUpdatedUserDto() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        Optional<UserDto> result = userService.updateUserRole(1L, UserRole.ADMIN);

        // Then
        assertTrue(result.isPresent());
        assertEquals(UserRole.ADMIN, testUser.getRole());
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserRole_WhenUserNotFound_ReturnsEmpty() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<UserDto> result = userService.updateUserRole(999L, UserRole.ADMIN);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_WithInterests_UpdatesInterestsSet() {
        // Given
        updateDto.setInterests(java.util.List.of("travel", "music", "food"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        Optional<UserDto> result = userService.updateUser(1L, updateDto);

        // Then
        assertTrue(result.isPresent());
        assertTrue(testUser.getInterests().contains("travel"));
        assertTrue(testUser.getInterests().contains("music"));
        assertTrue(testUser.getInterests().contains("food"));
        verify(userRepository).save(testUser);
    }
}
