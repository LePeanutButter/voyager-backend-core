package com.tourism.platform.controller;

import com.tourism.platform.config.GoogleOAuthProperties;
import com.tourism.platform.dto.UserDto;
import com.tourism.platform.exception.GlobalExceptionHandler;
import com.tourism.platform.service.GoogleAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GoogleAuthControllerTest {

    @Mock
    private GoogleOAuthProperties properties;

    @Mock
    private GoogleAuthService googleAuthService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GoogleAuthController controller = new GoogleAuthController(properties, googleAuthService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void login_RedirectsToGoogle_WhenConfigured() throws Exception {
        when(properties.getClientId()).thenReturn("cid");
        when(properties.getRedirectUri()).thenReturn("http://localhost/cb");
        when(properties.getScopes()).thenReturn("openid email profile");

        mockMvc.perform(get("/auth/google/login"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("accounts.google.com")))
                .andExpect(header().string("Location", containsString("client_id=cid")));
    }

    @Test
    void login_ReturnsBadRequest_WhenClientIdMissing() throws Exception {
        when(properties.getClientId()).thenReturn(" ");

        mockMvc.perform(get("/auth/google/login"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void callback_RedirectsWithToken_OnSuccess() throws Exception {
        when(properties.getFrontendRedirectUri()).thenReturn("http://frontend/app/oauth");
        UserDto dto = new UserDto();
        dto.setToken("jwt-value");
        when(googleAuthService.authenticateWithAuthorizationCode("abc")).thenReturn(dto);

        mockMvc.perform(get("/auth/google/callback").param("code", "abc"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("token=jwt-value")));

        verify(googleAuthService).authenticateWithAuthorizationCode("abc");
    }

    @Test
    void callback_RedirectsWithError_WhenOAuthError() throws Exception {
        when(properties.getFrontendRedirectUri()).thenReturn("http://frontend/app/oauth");

        mockMvc.perform(get("/auth/google/callback")
                        .param("error", "access_denied")
                        .param("error_description", "User cancelled"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("error=oauth_error")));
    }

    @Test
    void callback_RedirectsWithError_WhenCodeMissing() throws Exception {
        when(properties.getFrontendRedirectUri()).thenReturn("http://frontend/app/oauth");

        mockMvc.perform(get("/auth/google/callback"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("error=missing_code")));
    }

    @Test
    void callback_RedirectsWithError_OnAuthFailure() throws Exception {
        when(properties.getFrontendRedirectUri()).thenReturn("http://frontend/app/oauth");
        when(googleAuthService.authenticateWithAuthorizationCode(anyString()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/auth/google/callback").param("code", "x"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("error=auth_failed")));
    }
}
