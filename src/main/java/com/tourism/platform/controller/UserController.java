package com.tourism.platform.controller;

import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.PagedResponse;
import com.tourism.platform.dto.UserDto;
import com.tourism.platform.dto.UserLoginDto;
import com.tourism.platform.dto.UserRegistrationDto;
import com.tourism.platform.dto.UserUpdateDto;
import com.tourism.platform.model.UserRole;
import com.tourism.platform.model.UserStatus;
import com.tourism.platform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST Controller for user management operations
 * 
 * This controller follows REST best practices and Richardson Maturity Model Level 3:
 * - Proper HTTP methods (GET, POST, PUT, DELETE)
 * - Resource-based URIs
 * - HATEOAS principles with self-descriptive responses
 * - Standard HTTP status codes
 * - Content negotiation
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing user accounts and authentication")
@Validated
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided information")
    public ResponseEntity<ApiResponse<UserDto>> registerUser(
            @Valid @RequestBody UserRegistrationDto registrationDto,
            HttpServletRequest request) {
        
        UserDto createdUser = userService.registerUser(registrationDto);
        ApiResponse<UserDto> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "User registered successfully",
                createdUser,
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates user credentials and returns user information")
    public ResponseEntity<ApiResponse<UserDto>> loginUser(
            @Valid @RequestBody UserLoginDto loginDto,
            HttpServletRequest request) {
        
        Optional<UserDto> userOpt = userService.authenticateUser(
                loginDto.getUsernameOrEmail(), loginDto.getPassword());
        
        if (userOpt.isPresent()) {
            ApiResponse<UserDto> response = ApiResponse.success(
                    HttpStatus.OK.value(),
                    "Authentication successful",
                    userOpt.get(),
                    request.getRequestURI()
            );
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<UserDto> response = ApiResponse.error(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Invalid credentials",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves user information by their unique ID")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id,
            HttpServletRequest request) {
        
        Optional<UserDto> userOpt = userService.getUserById(id);
        
        if (userOpt.isPresent()) {
            ApiResponse<UserDto> response = ApiResponse.success(
                    HttpStatus.OK.value(),
                    "User retrieved successfully",
                    userOpt.get(),
                    request.getRequestURI()
            );
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<UserDto> response = ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "User not found",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "Retrieves user information by their username")
    public ResponseEntity<ApiResponse<UserDto>> getUserByUsername(
            @Parameter(description = "Username") @PathVariable String username,
            HttpServletRequest request) {
        
        Optional<UserDto> userOpt = userService.getUserByUsername(username);
        
        if (userOpt.isPresent()) {
            ApiResponse<UserDto> response = ApiResponse.success(
                    HttpStatus.OK.value(),
                    "User retrieved successfully",
                    userOpt.get(),
                    request.getRequestURI()
            );
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<UserDto> response = ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "User not found",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email", description = "Retrieves user information by their email address")
    public ResponseEntity<ApiResponse<UserDto>> getUserByEmail(
            @Parameter(description = "Email address") @PathVariable String email,
            HttpServletRequest request) {
        
        Optional<UserDto> userOpt = userService.getUserByEmail(email);
        
        if (userOpt.isPresent()) {
            ApiResponse<UserDto> response = ApiResponse.success(
                    HttpStatus.OK.value(),
                    "User retrieved successfully",
                    userOpt.get(),
                    request.getRequestURI()
            );
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<UserDto> response = ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "User not found",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile", description = "Updates user profile information")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody UserUpdateDto updateDto,
            HttpServletRequest request) {
        
        Optional<UserDto> userOpt = userService.updateUser(id, updateDto);
        
        if (userOpt.isPresent()) {
            ApiResponse<UserDto> response = ApiResponse.success(
                    HttpStatus.OK.value(),
                    "User updated successfully",
                    userOpt.get(),
                    request.getRequestURI()
            );
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<UserDto> response = ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "User not found",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Change user password", description = "Updates user password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Parameter(description = "User ID") @PathVariable Long id,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            HttpServletRequest request) {
        
        boolean success = userService.changePassword(id, currentPassword, newPassword);
        
        if (success) {
            ApiResponse<Void> response = ApiResponse.success(
                    HttpStatus.OK.value(),
                    "Password changed successfully",
                    request.getRequestURI()
            );
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<Void> response = ApiResponse.error(
                    HttpStatus.BAD_REQUEST.value(),
                    "Invalid current password",
                    request.getRequestURI()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves a paginated list of all users")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<UserDto>> getAllUsers(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {
        
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<UserDto> users = userService.getAllUsers(pageable);
        PagedResponse<UserDto> response = PagedResponse.fromPage(
                users,
                "Users retrieved successfully",
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Get users by role", description = "Retrieves users filtered by their role")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<UserDto>> getUsersByRole(
            @Parameter(description = "User role") @PathVariable UserRole role,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserDto> users = userService.getUsersByRole(role, pageable);
        PagedResponse<UserDto> response = PagedResponse.fromPage(
                users,
                "Users retrieved successfully",
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get users by status", description = "Retrieves users filtered by their status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<UserDto>> getUsersByStatus(
            @Parameter(description = "User status") @PathVariable UserStatus status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserDto> users = userService.getUsersByStatus(status, pageable);
        PagedResponse<UserDto> response = PagedResponse.fromPage(
                users,
                "Users retrieved successfully",
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by name", description = "Searches users by first name or last name")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<PagedResponse<UserDto>> searchUsersByName(
            @Parameter(description = "Search term") @RequestParam String searchTerm,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserDto> users = userService.searchUsersByName(searchTerm, pageable);
        PagedResponse<UserDto> response = PagedResponse.fromPage(
                users,
                "Users retrieved successfully",
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Update user role", description = "Updates user role (admin operation)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> updateUserRole(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Parameter(description = "New role") @RequestParam UserRole role,
            HttpServletRequest request) {
        
        Optional<UserDto> userOpt = userService.updateUserRole(id, role);
        
        if (userOpt.isPresent()) {
            ApiResponse<UserDto> response = ApiResponse.success(
                    HttpStatus.OK.value(),
                    "User role updated successfully",
                    userOpt.get(),
                    request.getRequestURI()
            );
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<UserDto> response = ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "User not found",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update user status", description = "Updates user status (admin operation)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam UserStatus status,
            HttpServletRequest request) {
        
        Optional<UserDto> userOpt = userService.updateUserStatus(id, status);
        
        if (userOpt.isPresent()) {
            ApiResponse<UserDto> response = ApiResponse.success(
                    HttpStatus.OK.value(),
                    "User status updated successfully",
                    userOpt.get(),
                    request.getRequestURI()
            );
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<UserDto> response = ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "User not found",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user account")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "User ID") @PathVariable Long id,
            HttpServletRequest request) {
        
        boolean deleted = userService.deleteUser(id);
        
        if (deleted) {
            ApiResponse<Void> response = ApiResponse.success(
                    HttpStatus.OK.value(),
                    "User deleted successfully",
                    request.getRequestURI()
            );
            return ResponseEntity.ok(response);
        } else {
            ApiResponse<Void> response = ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    "User not found",
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get user statistics", description = "Retrieves user statistics for dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<UserService.UserStatistics>> getUserStatistics(HttpServletRequest request) {
        UserService.UserStatistics statistics = userService.getUserStatistics();
        ApiResponse<UserService.UserStatistics> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "User statistics retrieved successfully",
                statistics,
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-username")
    @Operation(summary = "Check username availability", description = "Checks if a username is available for registration")
    public ResponseEntity<ApiResponse<Boolean>> checkUsernameAvailability(
            @Parameter(description = "Username to check") @RequestParam String username,
            HttpServletRequest request) {
        
        boolean available = userService.isUsernameAvailable(username);
        ApiResponse<Boolean> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Username availability checked",
                available,
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/check-email")
    @Operation(summary = "Check email availability", description = "Checks if an email is available for registration")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailAvailability(
            @Parameter(description = "Email to check") @RequestParam String email,
            HttpServletRequest request) {
        
        boolean available = userService.isEmailAvailable(email);
        ApiResponse<Boolean> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Email availability checked",
                available,
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }
}
