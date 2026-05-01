package com.tourism.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourism.platform.dto.UserDto;
import com.tourism.platform.dto.UserLoginDto;
import com.tourism.platform.exception.GlobalExceptionHandler;
import com.tourism.platform.security.JwtTokenProvider;
import com.tourism.platform.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerLoginTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider tokenProvider;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController(userService, tokenProvider);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void login_Success_ReturnsTokenWithUserIdClaimSource() throws Exception {
        UserDto dto = UserDto.builder()
                .id(42L)
                .username("alice")
                .email("a@x.com")
                .firstName("A")
                .lastName("B")
                .build();
        when(userService.authenticateUser(eq("alice"), eq("secret"))).thenReturn(Optional.of(dto));
        when(tokenProvider.generateTokenFromUsernameAndUserId("alice", 42L)).thenReturn("jwt-token");

        UserLoginDto login = UserLoginDto.builder()
                .usernameOrEmail("alice")
                .password("secret")
                .build();

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.id").value(42));
    }

    @Test
    void login_InvalidCredentials_Unauthorized() throws Exception {
        when(userService.authenticateUser(eq("x"), eq("y"))).thenReturn(Optional.empty());

        UserLoginDto login = UserLoginDto.builder().usernameOrEmail("x").password("y").build();

        mockMvc.perform(post("/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }
}
