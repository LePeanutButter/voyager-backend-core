package com.tourism.platform.controller;

import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.PagedResponse;
import com.tourism.platform.dto.UserDto;
import com.tourism.platform.dto.UserLoginDto;
import com.tourism.platform.dto.UserRegistrationDto;
import com.tourism.platform.dto.UserUpdateDto;
import com.tourism.platform.model.UserRole;
import com.tourism.platform.model.UserStatus;
import com.tourism.platform.security.JwtTokenProvider;
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
import org.springframework.lang.NonNull;
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
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing user accounts and authentication")
@Validated
public class UserController {
    private static final String USER_NOT_FOUND = "User not found";
    private static final String USER_RETRIEVED_SUCCESSFULLY = "User retrieved successfully";
    private static final String USERS_RETRIEVED_SUCCESSFULLY = "Users retrieved successfully";
    private static final String CREATED_AT = "createdAt";

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    @PostMapping
    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided information")
    /**
     * Register a new user account.
     *
     * @param registrationDto DTO containing registration details
     * @param request         current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing created UserDto and HTTP 201
     */
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

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided information")
    public ResponseEntity<ApiResponse<UserDto>> registerUserAlias(
            @Valid @RequestBody UserRegistrationDto registrationDto,
            HttpServletRequest request) {
        return registerUser(registrationDto, request);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates user credentials and returns user information with JWT token")
    /**
     * Authenticate a user with username/email and password. On success returns user details including JWT.
     *
     * @param loginDto login DTO containing username/email and password
     * @param request  current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing UserDto and token on success, or 401 on failure
     */
    public ResponseEntity<ApiResponse<UserDto>> loginUser(
            @Valid @RequestBody UserLoginDto loginDto,
            HttpServletRequest request) {
        Optional<UserDto> userOpt = userService.authenticateUser(
                loginDto.getUsernameOrEmail(), loginDto.getPassword());
        if (userOpt.isPresent()) {
            UserDto userDto = userOpt.get();
            String token = tokenProvider.generateTokenFromUsernameAndUserId(
                    userDto.getUsername(), userDto.getId());
            userDto.setToken(token);
            ApiResponse<UserDto> response = ApiResponse.success(
                    HttpStatus.OK.value(),
                    "Authentication successful",
                    userDto,
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
        /**
         * Retrieve a user by their id.
         *
         * @param id      user id
         * @param request current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing UserDto or 404 when not found
         */
        public ResponseEntity<ApiResponse<UserDto>> getUserById(
                        @Parameter(description = "User ID") @PathVariable @NonNull Long id,
                        HttpServletRequest request) {
                return toUserResponse(userService.getUserById(id), request.getRequestURI(), USER_RETRIEVED_SUCCESSFULLY);
        }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "Retrieves user information by their username")
        /**
         * Retrieve a user by username.
         *
         * @param username username to look up
         * @param request  current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing UserDto or 404 when not found
         */
        public ResponseEntity<ApiResponse<UserDto>> getUserByUsername(
                        @Parameter(description = "Username") @PathVariable String username,
                        HttpServletRequest request) {
                return toUserResponse(userService.getUserByUsername(username), request.getRequestURI(), USER_RETRIEVED_SUCCESSFULLY);
        }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email", description = "Retrieves user information by their email address")
        /**
         * Retrieve a user by email address.
         *
         * @param email   email address to look up
         * @param request current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing UserDto or 404 when not found
         */
        public ResponseEntity<ApiResponse<UserDto>> getUserByEmail(
                        @Parameter(description = "Email address") @PathVariable String email,
                        HttpServletRequest request) {
                return toUserResponse(userService.getUserByEmail(email), request.getRequestURI(), USER_RETRIEVED_SUCCESSFULLY);
        }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile", description = "Updates user profile information")
        /**
         * Update a user's profile information.
         *
         * @param id        user id
         * @param updateDto DTO with updated user fields
         * @param request   current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing updated UserDto
         */
        public ResponseEntity<ApiResponse<UserDto>> updateUser(
                        @Parameter(description = "User ID") @PathVariable @NonNull Long id,
                        @Valid @RequestBody @NonNull UserUpdateDto updateDto,
                        HttpServletRequest request) {
                return toUserResponse(userService.updateUser(id, updateDto), request.getRequestURI(), "User updated successfully");
        }

    @PutMapping("/{id}/password")
    @Operation(summary = "Change user password", description = "Updates user password")
        /**
         * Change a user's password.
         *
         * @param id              user id
         * @param currentPassword user's current password
         * @param newPassword     new password to set
         * @param request         current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse<Void> indicating success or failure
         */
        public ResponseEntity<ApiResponse<Void>> changePassword(
                        @Parameter(description = "User ID") @PathVariable @NonNull Long id,
                        @RequestParam @NonNull String currentPassword,
                        @RequestParam @NonNull String newPassword,
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
    /**
     * Retrieve a paginated list of all users. Requires ADMIN or SUPER_ADMIN.
     *
     * @param page    page number (0-based)
     * @param size    page size
     * @param sortBy  field to sort by
     * @param sortDir sort direction (asc|desc)
     * @param request current HTTP request used to build response path
     * @return ResponseEntity with PagedResponse containing UserDto
     */
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
                USERS_RETRIEVED_SUCCESSFULLY,
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Get users by role", description = "Retrieves users filtered by their role")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    /**
     * Retrieve users filtered by role. Requires ADMIN or SUPER_ADMIN.
     *
     * @param role    user role to filter by
     * @param page    page number (0-based)
     * @param size    page size
     * @param request current HTTP request used to build response path
     * @return ResponseEntity with PagedResponse containing UserDto
     */
    public ResponseEntity<PagedResponse<UserDto>> getUsersByRole(
            @Parameter(description = "User role") @PathVariable @NonNull UserRole role,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_AT).descending());
        Page<UserDto> users = userService.getUsersByRole(role, pageable);
        PagedResponse<UserDto> response = PagedResponse.fromPage(
                users,
                USERS_RETRIEVED_SUCCESSFULLY,
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get users by status", description = "Retrieves users filtered by their status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    /**
     * Retrieve users filtered by status. Requires ADMIN or SUPER_ADMIN.
     *
     * @param status  user status to filter by
     * @param page    page number (0-based)
     * @param size    page size
     * @param request current HTTP request used to build response path
     * @return ResponseEntity with PagedResponse containing UserDto
     */
    public ResponseEntity<PagedResponse<UserDto>> getUsersByStatus(
            @Parameter(description = "User status") @PathVariable @NonNull UserStatus status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_AT).descending());
        Page<UserDto> users = userService.getUsersByStatus(status, pageable);
        PagedResponse<UserDto> response = PagedResponse.fromPage(
                users,
                USERS_RETRIEVED_SUCCESSFULLY,
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by name", description = "Searches users by first name or last name")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    /**
     * Search users by first or last name. Requires ADMIN or SUPER_ADMIN.
     *
     * @param searchTerm search term to match against first or last name
     * @param page       page number (0-based)
     * @param size       page size
     * @param request    current HTTP request used to build response path
     * @return ResponseEntity with PagedResponse containing UserDto
     */
    public ResponseEntity<PagedResponse<UserDto>> searchUsersByName(
            @Parameter(description = "Search term") @RequestParam @NonNull String searchTerm,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_AT).descending());
        Page<UserDto> users = userService.searchUsersByName(searchTerm, pageable);
        PagedResponse<UserDto> response = PagedResponse.fromPage(
                users,
                USERS_RETRIEVED_SUCCESSFULLY,
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Update user role", description = "Updates user role (admin operation)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
        /**
         * Update a user's role. Requires ADMIN or SUPER_ADMIN.
         *
         * @param id      user id
         * @param role    new role to set
         * @param request current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing updated UserDto
         */
        public ResponseEntity<ApiResponse<UserDto>> updateUserRole(
                        @Parameter(description = "User ID") @PathVariable @NonNull Long id,
                        @Parameter(description = "New role") @RequestParam @NonNull UserRole role,
                        HttpServletRequest request) {
                return toUserResponse(userService.updateUserRole(id, role), request.getRequestURI(), "User role updated successfully");
        }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update user status", description = "Updates user status (admin operation)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
        /**
         * Update a user's status. Requires ADMIN or SUPER_ADMIN.
         *
         * @param id      user id
         * @param status  new status to set
         * @param request current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing updated UserDto
         */
        public ResponseEntity<ApiResponse<UserDto>> updateUserStatus(
                        @Parameter(description = "User ID") @PathVariable @NonNull Long id,
                        @Parameter(description = "New status") @RequestParam @NonNull UserStatus status,
                        HttpServletRequest request) {
                return toUserResponse(userService.updateUserStatus(id, status), request.getRequestURI(), "User status updated successfully");
        }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user", description = "Deletes a user account")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
        /**
         * Delete a user account. Requires SUPER_ADMIN.
         *
         * @param id      user id to delete
         * @param request current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse<Void> indicating deletion success or 404 when not found
         */
        public ResponseEntity<ApiResponse<Void>> deleteUser(
                        @Parameter(description = "User ID") @PathVariable @NonNull Long id,
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
                                        USER_NOT_FOUND,
                                        request.getRequestURI()
                        );
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                }
        }

    private ResponseEntity<ApiResponse<UserDto>> toUserResponse(Optional<UserDto> userOpt, String path, String successMessage) {
        /**
         * Helper that converts an Optional<UserDto> into a ResponseEntity<ApiResponse<UserDto>>.
         *
         * @param userOpt        optional user DTO
         * @param path           request path used in the ApiResponse
         * @param successMessage message to use on successful retrieval
         * @return ResponseEntity with ApiResponse containing UserDto or 404 error
         */
        return userOpt
                .map(user -> ResponseEntity.ok(ApiResponse.success(HttpStatus.OK.value(), successMessage, user, path)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), USER_NOT_FOUND, path)));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get user statistics", description = "Retrieves user statistics for dashboard")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
        /**
         * Retrieve aggregated user statistics for dashboard. Requires ADMIN or SUPER_ADMIN.
         *
         * @param request current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing UserStatistics
         */
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
            @Parameter(description = "Username to check") @RequestParam @NonNull String username,
            HttpServletRequest request) {
        /**
         * Check whether a username is available for registration.
         *
         * @param username candidate username
         * @param request  current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing boolean availability
         */
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
            @Parameter(description = "Email to check") @RequestParam @NonNull String email,
            HttpServletRequest request) {
        /**
         * Check whether an email address is available for registration.
         *
         * @param email   candidate email address
         * @param request current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing boolean availability
         */
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
