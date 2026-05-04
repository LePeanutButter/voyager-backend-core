package com.tourism.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tourism.platform.dto.UserDto;
import com.tourism.platform.dto.UserLoginDto;
import com.tourism.platform.dto.UserRegistrationDto;
import com.tourism.platform.dto.UserUpdateDto;
import com.tourism.platform.model.UserRole;
import com.tourism.platform.model.UserStatus;
import com.tourism.platform.security.JwtTokenProvider;
import com.tourism.platform.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setUsername("testuser");
        userDto.setEmail("test@example.com");
        userDto.setFirstName("Test");
        userDto.setLastName("User");
        userDto.setRole(UserRole.TRAVELER);
        userDto.setStatus(UserStatus.ACTIVE);
    }

    // ── POST /users ───────────────────────────────────────────────────────────

    @Test
    void registerUser_ShouldReturnCreated() throws Exception {
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("testuser");
        registrationDto.setEmail("test@example.com");
        registrationDto.setPassword("password123");
        registrationDto.setFirstName("Test");
        registrationDto.setLastName("User");

        when(userService.registerUser(any(UserRegistrationDto.class))).thenReturn(userDto);

        mockMvc.perform(post("/users")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(registrationDto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));

        verify(userService).registerUser(any(UserRegistrationDto.class));
    }

    // ── POST /users/register ──────────────────────────────────────────────────

    @Test
    void registerUserAlias_ShouldDelegateToRegisterUser() throws Exception {
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("testuser");
        registrationDto.setEmail("test@example.com");
        registrationDto.setPassword("password123");
        registrationDto.setFirstName("Test");
        registrationDto.setLastName("User");

        when(userService.registerUser(any(UserRegistrationDto.class))).thenReturn(userDto);

        mockMvc.perform(post("/users/register")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(registrationDto))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(userService).registerUser(any(UserRegistrationDto.class));
    }

    // ── POST /users/login ─────────────────────────────────────────────────────

    @Test
    void loginUser_WhenValidCredentials_ReturnsOkWithToken() throws Exception {
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setUsernameOrEmail("testuser");
        loginDto.setPassword("password123");

        when(userService.authenticateUser("testuser", "password123"))
                .thenReturn(Optional.of(userDto));
        when(tokenProvider.generateTokenFromUsernameAndUserId("testuser", 1L))
                .thenReturn("jwt.token.here");

        mockMvc.perform(post("/users/login")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginDto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Authentication successful"))
                .andExpect(jsonPath("$.data.token").value("jwt.token.here"));

        verify(tokenProvider).generateTokenFromUsernameAndUserId("testuser", 1L);
    }

    @Test
    void loginUser_WhenInvalidCredentials_ReturnsUnauthorized() throws Exception {
        UserLoginDto loginDto = new UserLoginDto();
        loginDto.setUsernameOrEmail("testuser");
        loginDto.setPassword("wrongpassword");

        when(userService.authenticateUser("testuser", "wrongpassword"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/users/login")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(loginDto))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        verify(tokenProvider, never()).generateTokenFromUsernameAndUserId(any(), any());
    }

    // ── GET /users/{id} ───────────────────────────────────────────────────────

    @Test
    void getUserById_WhenUserExists_ReturnsOk() throws Exception {
        when(userService.getUserById(1L)).thenReturn(Optional.of(userDto));

        mockMvc.perform(get("/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("User retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void getUserById_WhenUserNotFound_ReturnsNotFound() throws Exception {
        when(userService.getUserById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ── GET /users/username/{username} ────────────────────────────────────────

    @Test
    void getUserByUsername_WhenUserExists_ReturnsOk() throws Exception {
        when(userService.getUserByUsername("testuser")).thenReturn(Optional.of(userDto));

        mockMvc.perform(get("/users/username/{username}", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void getUserByUsername_WhenUserNotFound_ReturnsNotFound() throws Exception {
        when(userService.getUserByUsername("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/username/{username}", "ghost"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ── GET /users/email/{email} ──────────────────────────────────────────────

    @Test
    void getUserByEmail_WhenUserExists_ReturnsOk() throws Exception {
        when(userService.getUserByEmail("test@example.com")).thenReturn(Optional.of(userDto));

        mockMvc.perform(get("/users/email/{email}", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    void getUserByEmail_WhenUserNotFound_ReturnsNotFound() throws Exception {
        when(userService.getUserByEmail("ghost@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/users/email/{email}", "ghost@example.com"))
                .andExpect(status().isNotFound());
    }

    // ── PUT /users/{id} ───────────────────────────────────────────────────────

    @Test
    void updateUser_WhenUserExists_ReturnsOk() throws Exception {
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setFirstName("Updated");
        updateDto.setLastName("Name");
        updateDto.setBio("Updated bio");

        userDto.setFirstName("Updated");
        when(userService.updateUser(Objects.requireNonNull(1L), Objects.requireNonNull(any(UserUpdateDto.class)))).thenReturn(Optional.of(userDto));

        mockMvc.perform(put("/users/{id}", 1L)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(updateDto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User updated successfully"))
                .andExpect(jsonPath("$.data.firstName").value("Updated"));
    }

    @Test
    void updateUser_WhenUserNotFound_ReturnsNotFound() throws Exception {
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setFirstName("Updated");
        updateDto.setBio("bio");

        when(userService.updateUser(Objects.requireNonNull(999L), Objects.requireNonNull(any(UserUpdateDto.class)))).thenReturn(Optional.empty());

        mockMvc.perform(put("/users/{id}", 999L)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(updateDto))))
                .andExpect(status().isNotFound());
    }

    // ── PUT /users/{id}/password ──────────────────────────────────────────────

    @Test
    void changePassword_WhenSuccess_ReturnsOk() throws Exception {
        when(userService.changePassword(1L, "currentPass", "newPass")).thenReturn(true);

        mockMvc.perform(put("/users/{id}/password", 1L)
                        .param("currentPassword", "currentPass")
                        .param("newPassword", "newPass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    void changePassword_WhenInvalidCurrentPassword_ReturnsBadRequest() throws Exception {
        when(userService.changePassword(1L, "wrongPass", "newPass")).thenReturn(false);

        mockMvc.perform(put("/users/{id}/password", 1L)
                        .param("currentPassword", "wrongPass")
                        .param("newPassword", "newPass"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid current password"));
    }

    // ── GET /users ────────────────────────────────────────────────────────────

    @Test
    void getAllUsers_ShouldReturnPagedResponse() throws Exception {
        Page<UserDto> page = new PageImpl<>(Objects.requireNonNull(List.of(userDto)), PageRequest.of(0, 20), 1);
        when(userService.getAllUsers(Objects.requireNonNull(any(Pageable.class)))).thenReturn(page);

        mockMvc.perform(get("/users")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data[0].username").value("testuser"));
    }

    @Test
    void getAllUsers_WithAscSortDirection_ShouldReturnOk() throws Exception {
        Page<UserDto> page = new PageImpl<>(Objects.requireNonNull(List.of(userDto)));
        when(userService.getAllUsers(Objects.requireNonNull(any(Pageable.class)))).thenReturn(page);

        mockMvc.perform(get("/users")
                        .param("sortDir", "asc")
                        .param("sortBy", "username"))
                .andExpect(status().isOk());
    }

    // ── PUT /users/{id}/role ──────────────────────────────────────────────────

    @Test
    void updateUserRole_WhenUserExists_ReturnsOk() throws Exception {
        userDto.setRole(UserRole.ADMIN);
        when(userService.updateUserRole(1L, UserRole.ADMIN)).thenReturn(Optional.of(userDto));

        mockMvc.perform(put("/users/{id}/role", 1L).param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User role updated successfully"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void updateUserRole_WhenUserNotFound_ReturnsNotFound() throws Exception {
        when(userService.updateUserRole(999L, UserRole.ADMIN)).thenReturn(Optional.empty());

        mockMvc.perform(put("/users/{id}/role", 999L).param("role", "ADMIN"))
                .andExpect(status().isNotFound());
    }

    // ── PUT /users/{id}/status ────────────────────────────────────────────────

    @Test
    void updateUserStatus_WhenUserExists_ReturnsOk() throws Exception {
        userDto.setStatus(UserStatus.SUSPENDED);
        when(userService.updateUserStatus(1L, UserStatus.SUSPENDED)).thenReturn(Optional.of(userDto));

        mockMvc.perform(put("/users/{id}/status", 1L).param("status", "SUSPENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User status updated successfully"))
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));
    }

    @Test
    void updateUserStatus_WhenUserNotFound_ReturnsNotFound() throws Exception {
        when(userService.updateUserStatus(999L, UserStatus.SUSPENDED)).thenReturn(Optional.empty());

        mockMvc.perform(put("/users/{id}/status", 999L).param("status", "SUSPENDED"))
                .andExpect(status().isNotFound());
    }

    // ── DELETE /users/{id} ────────────────────────────────────────────────────

    @Test
    void deleteUser_WhenUserExists_ReturnsOk() throws Exception {
        when(userService.deleteUser(1L)).thenReturn(true);

        mockMvc.perform(delete("/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));
    }

    @Test
    void deleteUser_WhenUserNotFound_ReturnsNotFound() throws Exception {
        when(userService.deleteUser(999L)).thenReturn(false);

        mockMvc.perform(delete("/users/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    // ── GET /users/statistics ─────────────────────────────────────────────────

    @Test
    void getUserStatistics_ShouldReturnOk() throws Exception {
        UserService.UserStatistics stats = new UserService.UserStatistics(100L, 80L);
        when(userService.getUserStatistics()).thenReturn(stats);

        mockMvc.perform(get("/users/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User statistics retrieved successfully"))
                .andExpect(jsonPath("$.data.totalUsers").value(100))
                .andExpect(jsonPath("$.data.activeUsers").value(80));
    }

    // ── GET /users/check-username ─────────────────────────────────────────────

    @Test
    void checkUsernameAvailability_WhenAvailable_ReturnsTrue() throws Exception {
        when(userService.isUsernameAvailable("freeuser")).thenReturn(true);

        mockMvc.perform(get("/users/check-username").param("username", "freeuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Username availability checked"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void checkUsernameAvailability_WhenTaken_ReturnsFalse() throws Exception {
        when(userService.isUsernameAvailable("testuser")).thenReturn(false);

        mockMvc.perform(get("/users/check-username").param("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }

    // ── GET /users/check-email ────────────────────────────────────────────────

    @Test
    void checkEmailAvailability_WhenAvailable_ReturnsTrue() throws Exception {
        when(userService.isEmailAvailable("free@example.com")).thenReturn(true);

        mockMvc.perform(get("/users/check-email").param("email", "free@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email availability checked"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void checkEmailAvailability_WhenTaken_ReturnsFalse() throws Exception {
        when(userService.isEmailAvailable("test@example.com")).thenReturn(false);

        mockMvc.perform(get("/users/check-email").param("email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }
}