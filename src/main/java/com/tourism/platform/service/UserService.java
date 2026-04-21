package com.tourism.platform.service;

import com.tourism.platform.dto.UserDto;
import com.tourism.platform.dto.UserRegistrationDto;
import com.tourism.platform.dto.UserUpdateDto;
import com.tourism.platform.model.UserRole;
import com.tourism.platform.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Service interface for user management operations
 * 
 * This interface defines the contract for user-related business logic
 * including registration, authentication, profile management, and administrative operations.
 */
public interface UserService {

    /**
     * Register a new user
     * 
     * @param registrationDto user registration data
     * @return UserDto of the created user
     */
    UserDto registerUser(UserRegistrationDto registrationDto);

    /**
     * Authenticate user credentials
     * 
     * @param usernameOrEmail username or email
     * @param password user password
     * @return Optional UserDto if authentication successful
     */
    Optional<UserDto> authenticateUser(String usernameOrEmail, String password);

    /**
     * Get user by ID
     * 
     * @param userId user ID
     * @return Optional UserDto if found
     */
    Optional<UserDto> getUserById(Long userId);

    /**
     * Get user by username
     * 
     * @param username username
     * @return Optional UserDto if found
     */
    Optional<UserDto> getUserByUsername(String username);

    /**
     * Get user by email
     * 
     * @param email email address
     * @return Optional UserDto if found
     */
    Optional<UserDto> getUserByEmail(String email);

    /**
     * Update user profile
     * 
     * @param userId user ID
     * @param updateDto user update data
     * @return Optional UserDto if update successful
     */
    Optional<UserDto> updateUser(Long userId, UserUpdateDto updateDto);

    /**
     * Change user password
     * 
     * @param userId user ID
     * @param currentPassword current password
     * @param newPassword new password
     * @return true if password change successful
     */
    boolean changePassword(Long userId, String currentPassword, String newPassword);

    /**
     * Update user role (admin operation)
     * 
     * @param userId user ID
     * @param role new role
     * @return Optional UserDto if update successful
     */
    Optional<UserDto> updateUserRole(Long userId, UserRole role);

    /**
     * Update user status (admin operation)
     * 
     * @param userId user ID
     * @param status new status
     * @return Optional UserDto if update successful
     */
    Optional<UserDto> updateUserStatus(Long userId, UserStatus status);

    /**
     * Delete user account
     * 
     * @param userId user ID
     * @return true if deletion successful
     */
    boolean deleteUser(Long userId);

    /**
     * Get all users with pagination
     * 
     * @param pageable pagination information
     * @return Page of UserDto
     */
    Page<UserDto> getAllUsers(Pageable pageable);

    /**
     * Get users by role with pagination
     * 
     * @param role user role
     * @param pageable pagination information
     * @return Page of UserDto
     */
    Page<UserDto> getUsersByRole(UserRole role, Pageable pageable);

    /**
     * Get users by status with pagination
     * 
     * @param status user status
     * @param pageable pagination information
     * @return Page of UserDto
     */
    Page<UserDto> getUsersByStatus(UserStatus status, Pageable pageable);

    /**
     * Search users by name with pagination
     * 
     * @param searchTerm search term
     * @param pageable pagination information
     * @return Page of UserDto
     */
    Page<UserDto> searchUsersByName(String searchTerm, Pageable pageable);

    /**
     * Check if username is available
     * 
     * @param username username to check
     * @return true if username is available
     */
    boolean isUsernameAvailable(String username);

    /**
     * Check if email is available
     * 
     * @param email email to check
     * @return true if email is available
     */
    boolean isEmailAvailable(String email);

    /**
     * Enable/disable user account
     * 
     * @param userId user ID
     * @param enabled account status
     * @return Optional UserDto if update successful
     */
    Optional<UserDto> setUserEnabled(Long userId, boolean enabled);

    /**
     * Get user statistics
     * 
     * @return User statistics object
     */
    UserStatistics getUserStatistics();

    /**
     * Inner class for user statistics
     */
    class UserStatistics {
        private final long totalUsers;
        private final long activeUsers;

        public UserStatistics(long totalUsers, long activeUsers) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
        }

        public long getTotalUsers() {
            return totalUsers;
        }

        public long getActiveUsers() {
            return activeUsers;
        }
    }
}
