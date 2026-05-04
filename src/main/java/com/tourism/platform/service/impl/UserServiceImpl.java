package com.tourism.platform.service.impl;

import com.tourism.platform.dto.UserDto;
import com.tourism.platform.dto.UserRegistrationDto;
import com.tourism.platform.dto.UserUpdateDto;
import com.tourism.platform.model.User;
import com.tourism.platform.model.UserRole;
import com.tourism.platform.model.UserStatus;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service implementation for user management operations
 * 
 * This class provides the concrete implementation of user-related business logic
 * including registration, authentication, profile management, and administrative operations.
 */
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDto registerUser(UserRegistrationDto registrationDto) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(registrationDto.getUsername());
        user.setEmail(registrationDto.getEmail());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());
        user.setPhoneNumber(registrationDto.getPhoneNumber());
        user.setRole(UserRole.TRAVELER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> authenticateUser(String usernameOrEmail, String password) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()) && user.isEnabled())
                .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> getUserById(@NonNull Long userId) {
        return userRepository.findById(userId)
                .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserDto> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::convertToDto);
    }

    @Override
    public Optional<UserDto> updateUser(@NonNull Long userId, @NonNull UserUpdateDto updateDto) {
        return userRepository.findById(userId)
                .map(user -> {
                    validateUpdateDto(updateDto);
                    updateUserFields(user, updateDto);
                    @SuppressWarnings("null")
                    User updatedUser = userRepository.save(user);
                    return convertToDto(updatedUser);
                });
    }

    private void validateUpdateDto(UserUpdateDto updateDto) {
        if (updateDto.getFirstName() == null || updateDto.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("firstName is required");
        }
        if (updateDto.getBio() == null || updateDto.getBio().trim().isEmpty()) {
            throw new IllegalArgumentException("bio is required");
        }
    }

    private void updateUserFields(User user, UserUpdateDto updateDto) {
        updateOptionalField(user::setFirstName, updateDto.getFirstName());
        updateOptionalField(user::setLastName, updateDto.getLastName());
        updateOptionalField(user::setPhoneNumber, updateDto.getPhoneNumber());
        updateOptionalField(user::setProfileImageUrl, updateDto.getProfileImageUrl());
        updateOptionalField(user::setBio, updateDto.getBio());
        
        if (updateDto.getInterests() != null) {
            user.setInterests(new java.util.HashSet<>(updateDto.getInterests()));
        }
    }

    private <T> void updateOptionalField(java.util.function.Consumer<T> setter, T value) {
        if (value != null) {
            setter.accept(value);
        }
    }

    @Override
    public boolean changePassword(@NonNull Long userId, @NonNull String currentPassword, @NonNull String newPassword) {
        return userRepository.findById(userId)
                .map(user -> {
                    if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                        user.setPassword(passwordEncoder.encode(newPassword));
                        userRepository.save(user);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    @Override
    public Optional<UserDto> updateUserRole(@NonNull Long userId, @NonNull UserRole role) {
        return userRepository.findById(userId)
                .map(user -> {
                    user.setRole(role);
                    User updatedUser = userRepository.save(user);
                    return convertToDto(updatedUser);
                });
    }

    @Override
    public Optional<UserDto> updateUserStatus(@NonNull Long userId, @NonNull UserStatus status) {
        return userRepository.findById(userId)
                .map(user -> {
                    user.setStatus(status);
                    User updatedUser = userRepository.save(user);
                    return convertToDto(updatedUser);
                });
    }

    @Override
    public boolean deleteUser(@NonNull Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsers(@NonNull Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getUsersByRole(@NonNull UserRole role, @NonNull Pageable pageable) {
        return userRepository.findByRole(role, pageable)
                .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getUsersByStatus(@NonNull UserStatus status, @NonNull Pageable pageable) {
        return userRepository.findByStatus(status, pageable)
                .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> searchUsersByName(@NonNull String searchTerm, @NonNull Pageable pageable) {
        return userRepository.searchByName(searchTerm, pageable)
                .map(this::convertToDto);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUsernameAvailable(@NonNull String username) {
        return !userRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(@NonNull String email) {
        return !userRepository.existsByEmail(email);
    }

    @Override
    public Optional<UserDto> setUserEnabled(@NonNull Long userId, boolean enabled) {
        return userRepository.findById(userId)
                .map(user -> {
                    user.setEnabled(enabled);
                    User updatedUser = userRepository.save(user);
                    return convertToDto(updatedUser);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatistics getUserStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        return new UserStatistics(totalUsers, activeUsers);
    }

    /**
     * Convert User entity to UserDto
     */
    private UserDto convertToDto(@NonNull User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setProfileImageUrl(user.getProfileImageUrl());
        dto.setBio(user.getBio());
        if (user.getInterests() != null) {
            dto.setInterests(new java.util.HashSet<>(user.getInterests()));
        } else {
            dto.setInterests(null);
        }
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        return dto;
    }
}
