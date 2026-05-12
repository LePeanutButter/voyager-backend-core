package com.tourism.platform.service.impl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tourism.platform.dto.UserDto;
import com.tourism.platform.dto.UserRegistrationDto;
import com.tourism.platform.dto.UserUpdateDto;
import com.tourism.platform.model.User;
import com.tourism.platform.model.UserRole;
import com.tourism.platform.model.UserStatus;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.service.UserService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
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
    void getUserByIdWhenUserExistsReturnsUserDto() {
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
    void getUserByIdWhenUserNotFoundReturnsEmpty() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<UserDto> result = userService.getUserById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findById(999L);
    }

    @Test
    void getUserByEmailWhenUserExistsReturnsUserDto() {
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
    void getUserByEmailWhenUserNotFoundReturnsEmpty() {
        // Given
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        Optional<UserDto> result = userService.getUserByEmail("nonexistent@example.com");

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void updateUserWhenUserExistsAndValidDataReturnsUpdatedUserDto() {
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
    void updateUserWhenUserNotFoundReturnsEmpty() {
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
    void updateUserWhenFirstNameIsEmptyThrowsIllegalArgumentException() {
        // Given
        updateDto.setFirstName("");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException firstNameException = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(1L, updateDto));
        assertNotNull(firstNameException);
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserWhenBioIsEmptyThrowsIllegalArgumentException() {
        // Given
        updateDto.setBio("");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        IllegalArgumentException bioException = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(1L, updateDto));
        assertNotNull(bioException);
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserWithNullFieldsUpdatesOnlyNonNullFields() {
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
    void changePasswordWithValidCurrentPasswordReturnsTrue() {
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
    void changePasswordWithInvalidCurrentPasswordReturnsFalse() {
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
    void changePasswordWhenUserNotFoundReturnsFalse() {
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
    void updateUserRoleWhenUserExistsReturnsUpdatedUserDto() {
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
    void updateUserRoleWhenUserNotFoundReturnsEmpty() {
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
    void updateUserWithInterestsUpdatesInterestsSet() {
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
    void registerUserWhenValidDataReturnsUserDto() {
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
    void registerUserWhenUsernameAlreadyExistsThrowsIllegalArgumentException() {
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
    void registerUserWhenEmailAlreadyExistsThrowsIllegalArgumentException() {
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
    void authenticateUserWhenValidCredentialsAndEnabledReturnsUserDto() {
        testUser.setEnabled(true);
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);

        Optional<UserDto> result = userService.authenticateUser("testuser", "correctPassword");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void authenticateUserWhenWrongPasswordReturnsEmpty() {
        testUser.setEnabled(true);
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        Optional<UserDto> result = userService.authenticateUser("testuser", "wrongPassword");

        assertFalse(result.isPresent());
    }

    @Test
    void authenticateUserWhenUserIsDisabledReturnsEmpty() {
        testUser.setEnabled(false);
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("correctPassword", "encodedPassword")).thenReturn(true);

        Optional<UserDto> result = userService.authenticateUser("testuser", "correctPassword");

        assertFalse(result.isPresent());
    }

    @Test
    void authenticateUserWhenUserNotFoundReturnsEmpty() {
        when(userRepository.findByUsernameOrEmail("ghost", "ghost"))
                .thenReturn(Optional.empty());

        Optional<UserDto> result = userService.authenticateUser("ghost", "anyPassword");

        assertFalse(result.isPresent());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

// ── getUserByUsername ─────────────────────────────────────────────────────────

    @Test
    void getUserByUsernameWhenUserExistsReturnsUserDto() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<UserDto> result = userService.getUserByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserByUsernameWhenUserNotFoundReturnsEmpty() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        Optional<UserDto> result = userService.getUserByUsername("ghost");

        assertFalse(result.isPresent());
    }

// ── updateUserStatus ──────────────────────────────────────────────────────────

    @Test
    void updateUserStatusWhenUserExistsReturnsUpdatedUserDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        Optional<UserDto> result = userService.updateUserStatus(1L, UserStatus.DEACTIVATED);

        assertTrue(result.isPresent());
        assertEquals(UserStatus.DEACTIVATED, testUser.getStatus());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUserStatusWhenUserNotFoundReturnsEmpty() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<UserDto> result = userService.updateUserStatus(999L, UserStatus.DEACTIVATED);

        assertFalse(result.isPresent());
        verify(userRepository, never()).save(any());
    }

// ── deleteUser ────────────────────────────────────────────────────────────────

    @Test
    void deleteUserWhenUserExistsReturnsTrueAndDeletes() {
        when(userRepository.existsById(1L)).thenReturn(true);

        boolean result = userService.deleteUser(1L);

        assertTrue(result);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUserWhenUserNotFoundReturnsFalse() {
        when(userRepository.existsById(999L)).thenReturn(false);

        boolean result = userService.deleteUser(999L);

        assertFalse(result);
        verify(userRepository, never()).deleteById(any());
    }

// ── getAllUsers ───────────────────────────────────────────────────────────────

    @Test
    void getAllUsersShouldReturnPageOfUserDtos() {
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
    void getUsersByRoleShouldReturnFilteredPage() {
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
    void getUsersByStatusShouldReturnFilteredPage() {
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
    void searchUsersByNameShouldReturnMatchingPage() {
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
    void isUsernameAvailableWhenUsernameNotTakenReturnsTrue() {
        when(userRepository.existsByUsername("freeuser")).thenReturn(false);

        assertTrue(userService.isUsernameAvailable("freeuser"));
    }

    @Test
    void isUsernameAvailableWhenUsernameTakenReturnsFalse() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertFalse(userService.isUsernameAvailable("testuser"));
    }

// ── isEmailAvailable ──────────────────────────────────────────────────────────

    @Test
    void isEmailAvailableWhenEmailNotTakenReturnsTrue() {
        when(userRepository.existsByEmail("free@example.com")).thenReturn(false);

        assertTrue(userService.isEmailAvailable("free@example.com"));
    }

    @Test
    void isEmailAvailableWhenEmailTakenReturnsFalse() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertFalse(userService.isEmailAvailable("test@example.com"));
    }

// ── setUserEnabled ────────────────────────────────────────────────────────────

    @Test
    void setUserEnabledWhenUserExistsEnablesAndReturnsDto() {
        testUser.setEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        Optional<UserDto> result = userService.setUserEnabled(1L, true);

        assertTrue(result.isPresent());
        assertTrue(testUser.isEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    void setUserEnabledWhenUserExistsDisablesAndReturnsDto() {
        testUser.setEnabled(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        Optional<UserDto> result = userService.setUserEnabled(1L, false);

        assertTrue(result.isPresent());
        assertFalse(testUser.isEnabled());
        verify(userRepository).save(testUser);
    }

    @Test
    void setUserEnabledWhenUserNotFoundReturnsEmpty() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<UserDto> result = userService.setUserEnabled(999L, true);

        assertFalse(result.isPresent());
        verify(userRepository, never()).save(any());
    }

// ── getUserStatistics ─────────────────────────────────────────────────────────

    @Test
    void getUserStatisticsShouldReturnCorrectCounts() {
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(35L);

        UserService.UserStatistics stats = userService.getUserStatistics();

        assertEquals(50L, stats.getTotalUsers());
        assertEquals(35L, stats.getActiveUsers());
        verify(userRepository).count();
        verify(userRepository).countByStatus(UserStatus.ACTIVE);
    }

    @Test
    void getUserStatisticsWhenNoUsersReturnsZeroCounts() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByStatus(UserStatus.ACTIVE)).thenReturn(0L);

        UserService.UserStatistics stats = userService.getUserStatistics();

        assertEquals(0L, stats.getTotalUsers());
        assertEquals(0L, stats.getActiveUsers());
    }

// ── updateUser — null firstName branch ───────────────────────────────────────

    @Test
    void updateUserWhenFirstNameIsNullThrowsIllegalArgumentException() {
        updateDto.setFirstName(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(1L, updateDto));
        assertNotNull(ex);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserWhenBioIsNullThrowsIllegalArgumentException() {
        updateDto.setBio(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.updateUser(1L, updateDto));
        assertNotNull(ex);
        verify(userRepository, never()).save(any());
    }
}
