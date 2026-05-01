package com.tourism.platform.service.impl;

import com.tourism.platform.dto.UserDto;
import com.tourism.platform.dto.UserRegistrationDto;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
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

    // ── registerUser ──────────────────────────────────────────────────────────────

    @Test
    void registerUser_WhenValidData_ReturnsUserDto() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("newuser");
        dto.setEmail("new@example.com");
        dto.setPassword("plainPassword");
        dto.setFirstName("New");
        dto.setLastName("User");
        dto.setPhoneNumber("123456789");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        UserDto result = userService.registerUser(dto);

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("new@example.com", result.getEmail());
        assertEquals(UserRole.TRAVELER, result.getRole());
        assertEquals(UserStatus.ACTIVE, result.getStatus());
        verify(passwordEncoder).encode("plainPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_WhenUsernameAlreadyExists_ThrowsIllegalArgumentException() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("testuser");
        dto.setEmail("new@example.com");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(dto));

        assertEquals("Username already exists", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_WhenEmailAlreadyExists_ThrowsIllegalArgumentException() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("brandnewuser");
        dto.setEmail("test@example.com");

        when(userRepository.existsByUsername("brandnewuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.registerUser(dto));

        assertEquals("Email already exists", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

// ── authenticateUser ──────────────────────────────────────────────────────────

    @Test
    void authenticateUser_WhenValidCredentialsAndEnabled_ReturnsUserDto() {
        testUser.setEnabled(true);
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);

        Optional<UserDto> result = userService.authenticateUser("testuser", "correctPassword");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void authenticateUser_WhenWrongPassword_ReturnsEmpty() {
        testUser.setEnabled(true);
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        Optional<UserDto> result = userService.authenticateUser("testuser", "wrongPassword");

        assertFalse(result.isPresent());
    }

    @Test
    void authenticateUser_WhenUserIsDisabled_ReturnsEmpty() {
        testUser.setEnabled(false);
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);

        Optional<UserDto> result = userService.authenticateUser("testuser", "correctPassword");

        assertFalse(result.isPresent());
    }

    @Test
    void authenticateUser_WhenUserNotFound_ReturnsEmpty() {
        when(userRepository.findByUsernameOrEmail("ghost", "ghost"))
                .thenReturn(Optional.empty());

        Optional<UserDto> result = userService.authenticateUser("ghost", "anyPassword");

        assertFalse(result.isPresent());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

// ── getUserByUsername ─────────────────────────────────────────────────────────

    @Test
    void getUserByUsername_WhenUserExists_ReturnsUserDto() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<UserDto> result = userService.getUserByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserByUsername_WhenUserNotFound_ReturnsEmpty() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        Optional<UserDto> result = userService.getUserByUsername("ghost");

        assertFalse(result.isPresent());
    }

// ── updateUserStatus ──────────────────────────────────────────────────────────

    @Test
    void updateUserStatus_WhenUserExists_ReturnsUpdatedUserDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        Optional<UserDto> result = userService.updateUserStatus(1L, UserStatus.DEACTIVATED);

        assertTrue(result.isPresent());
        assertEquals(UserStatus.DEACTIVATED, testUser.getStatus());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserStatus_WhenUserNotFound_ReturnsEmpty() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<UserDto> result = userService.updateUserStatus(999L, UserStatus.DEACTIVATED);

        assertFalse(result.isPresent());
        verify(userRepository, never()).save(any());
    }

// ── deleteUser ────────────────────────────────────────────────────────────────

    @Test
    void deleteUser_WhenUserExists_ReturnsTrueAndDeletes() {
        when(userRepository.existsById(1L)).thenReturn(true);

        boolean result = userService.deleteUser(1L);

        assertTrue(result);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_WhenUserNotFound_ReturnsFalse() {
        when(userRepository.existsById(999L)).thenReturn(false);

        boolean result = userService.deleteUser(999L);

        assertFalse(result);
        verify(userRepository, never()).deleteById(any());
    }

// ── getAllUsers ───────────────────────────────────────────────────────────────

    @Test
    void getAllUsers_ShouldReturnPageOfUserDtos() {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<User> page =
                new org.springframework.data.domain.PageImpl<>(List.of(testUser));

        when(userRepository.findAll(pageable)).thenReturn(page);

        org.springframework.data.domain.Page<UserDto> result = userService.getAllUsers(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("testuser", result.getContent().get(0).getUsername());
    }

// ── getUsersByRole ────────────────────────────────────────────────────────────

    @Test
    void getUsersByRole_ShouldReturnFilteredPage() {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<User> page =
                new org.springframework.data.domain.PageImpl<>(List.of(testUser));

        when(userRepository.findByRole(UserRole.TRAVELER, pageable)).thenReturn(page);

        org.springframework.data.domain.Page<UserDto> result =
                userService.getUsersByRole(UserRole.TRAVELER, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(UserRole.TRAVELER, result.getContent().get(0).getRole());
    }

// ── getUsersByStatus ──────────────────────────────────────────────────────────

    @Test
    void getUsersByStatus_ShouldReturnFilteredPage() {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<User> page =
                new org.springframework.data.domain.PageImpl<>(List.of(testUser));

        when(userRepository.findByStatus(UserStatus.ACTIVE, pageable)).thenReturn(page);

        org.springframework.data.domain.Page<UserDto> result =
                userService.getUsersByStatus(UserStatus.ACTIVE, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(UserStatus.ACTIVE, result.getContent().get(0).getStatus());
    }

// ── searchUsersByName ─────────────────────────────────────────────────────────

    @Test
    void searchUsersByName_ShouldReturnMatchingPage() {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<User> page =
                new org.springframework.data.domain.PageImpl<>(List.of(testUser));

        when(userRepository.searchByName("Test", pageable)).thenReturn(page);

        org.springframework.data.domain.Page<UserDto> result =
                userService.searchUsersByName("Test", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("testuser", result.getContent().get(0).getUsername());
    }

// ── isUsernameAvailable ───────────────────────────────────────────────────────

    @Test
    void isUsernameAvailable_WhenUsernameNotTaken_ReturnsTrue() {
        when(userRepository.existsByUsername("freeuser")).thenReturn(false);

        assertTrue(userService.isUsernameAvailable("freeuser"));
    }

    @Test
    void isUsernameAvailable_WhenUsernameTaken_ReturnsFalse() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertFalse(userService.isUsernameAvailable("testuser"));
    }

// ── isEmailAvailable ──────────────────────────────────────────────────────────

    @Test
    void isEmailAvailable_WhenEmailNotTaken_ReturnsTrue() {
        when(userRepository.existsByEmail("free@example.com")).thenReturn(false);

        assertTrue(userService.isEmailAvailable("free@example.com"));
    }

    @Test
    void isEmailAvailable_WhenEmailTaken_ReturnsFalse() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertFalse(userService.isEmailAvailable("test@example.com"));
    }

// ── setUserEnabled ────────────────────────────────────────────────────────────

    @Test
    void setUserEnabled_WhenUserExists_EnablesAndReturnsDto() {
        testUser.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        Optional<UserDto> result = userService.setUserEnabled(1L, true);

        assertTrue(result.isPresent());
        assertTrue(testUser.isEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    void setUserEnabled_WhenUserExists_DisablesAndReturnsDto() {
        testUser.setEnabled(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        Optional<UserDto> result = userService.setUserEnabled(1L, false);

        assertTrue(result.isPresent());
        assertFalse(testUser.isEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    void setUserEnabled_WhenUserNotFound_ReturnsEmpty() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<UserDto> result = userService.setUserEnabled(999L, true);

        assertFalse(result.isPresent());
        verify(userRepository, never()).save(any());
    }

// ── getUserStatistics ─────────────────────────────────────────────────────────

    @Test
    void getUserStatistics_ShouldReturnCorrectCounts() {
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(35L);

        UserService.UserStatistics stats = userService.getUserStatistics();

        assertEquals(50L, stats.getTotalUsers());
        assertEquals(35L, stats.getActiveUsers());
        verify(userRepository).count();
        verify(userRepository).countByStatus(UserStatus.ACTIVE);
    }

    @Test
    void getUserStatistics_WhenNoUsers_ReturnsZeroCounts() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(0L);

        UserService.UserStatistics stats = userService.getUserStatistics();

        assertEquals(0L, stats.getTotalUsers());
        assertEquals(0L, stats.getActiveUsers());
    }

// ── updateUser — null firstName branch ───────────────────────────────────────

    @Test
    void updateUser_WhenFirstNameIsNull_ThrowsIllegalArgumentException() {
        updateDto.setFirstName(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(1L, updateDto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_WhenBioIsNull_ThrowsIllegalArgumentException() {
        updateDto.setBio(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(1L, updateDto));
        verify(userRepository, never()).save(any());
    }
}
