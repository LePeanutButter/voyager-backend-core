package com.tourism.platform.controller;

import com.tourism.platform.config.GoogleOAuthProperties;
import com.tourism.platform.dto.UserDto;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.exception.GlobalExceptionHandler;
import com.tourism.platform.service.GoogleAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Objects;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        GoogleAuthController controller = new GoogleAuthController(properties, googleAuthService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void tokenReturnsWrappedUserWithJwt() throws Exception {
        UserDto dto = new UserDto();
        dto.setId(5L);
        dto.setUsername("g");
        dto.setEmail("g@g.com");
        dto.setToken("jwt-1");
        when(googleAuthService.authenticateWithMobileServerAuthCode("c1")).thenReturn(dto);

        mockMvc.perform(post("/auth/google/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"c1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt-1"));

        verify(googleAuthService).authenticateWithMobileServerAuthCode("c1");
    }

    @Test
    void tokenReturnsBadRequestWhenCodeBlank() throws Exception {
        mockMvc.perform(post("/auth/google/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginRedirectsToGoogleWhenConfigured() throws Exception {
        when(properties.getClientId()).thenReturn("cid");
        when(properties.getRedirectUri()).thenReturn("http://localhost/cb");
        when(properties.getScopes()).thenReturn("openid email profile");

        mockMvc.perform(get("/auth/google/login"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", Objects.requireNonNull(org.hamcrest.Matchers.containsString("accounts.google.com"))))
                .andExpect(header().string("Location", Objects.requireNonNull(org.hamcrest.Matchers.containsString("client_id=cid"))));
    }

    @Test
    void loginReturnsBadRequestWhenClientIdMissing() throws Exception {
        when(properties.getClientId()).thenReturn(" ");

        mockMvc.perform(get("/auth/google/login"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsBadRequestWhenClientIdNull() throws Exception {
        when(properties.getClientId()).thenReturn(null);

        mockMvc.perform(get("/auth/google/login"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void callbackRedirectsWithTokenOnSuccess() throws Exception {
        when(properties.getFrontendRedirectUri()).thenReturn("http://frontend/app/oauth");
        UserDto dto = new UserDto();
        dto.setToken("jwt-value");
        when(googleAuthService.authenticateWithAuthorizationCode("abc")).thenReturn(dto);

        mockMvc.perform(get("/auth/google/callback")
                        .param("code", "abc")
                        .param("state", "ok-state")
                        .cookie(new jakarta.servlet.http.Cookie("google_oauth_state", "ok-state")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", Objects.requireNonNull(org.hamcrest.Matchers.containsString("token=jwt-value"))));

        verify(googleAuthService).authenticateWithAuthorizationCode("abc");
    }

    @Test
    void callbackRedirectsWithErrorWhenOAuthError() throws Exception {
        when(properties.getFrontendRedirectUri()).thenReturn("http://frontend/app/oauth");

        mockMvc.perform(get("/auth/google/callback")
                        .param("error", "access_denied")
                        .param("error_description", "User cancelled"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", Objects.requireNonNull(org.hamcrest.Matchers.containsString("error=oauth_error"))));
    }

    @Test
    void callbackRedirectsWithErrorWhenCodeMissing() throws Exception {
        when(properties.getFrontendRedirectUri()).thenReturn("http://frontend/app/oauth");

        mockMvc.perform(get("/auth/google/callback"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", Objects.requireNonNull(org.hamcrest.Matchers.containsString("error=missing_code"))));
    }

    @Test
    void callbackRedirectsWithErrorOnAuthFailure() throws Exception {
        when(properties.getFrontendRedirectUri()).thenReturn("http://frontend/app/oauth");
        when(googleAuthService.authenticateWithAuthorizationCode(anyString()))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/auth/google/callback")
                        .param("code", "x")
                        .param("state", "ok-state")
                        .cookie(new jakarta.servlet.http.Cookie("google_oauth_state", "ok-state")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", Objects.requireNonNull(org.hamcrest.Matchers.containsString("error=auth_failed"))));
    }

    @Test
    void callbackReturnsBadRequestWhenStateMismatch() throws Exception {
        mockMvc.perform(get("/auth/google/callback")
                        .param("code", "abc")
                        .param("state", "expected")
                        .cookie(new jakarta.servlet.http.Cookie("google_oauth_state", "different")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void callbackRedirectsWithBusinessErrorWhenServiceThrowsBusinessException() throws Exception {
        when(properties.getFrontendRedirectUri()).thenReturn("http://frontend/app/oauth");
        when(googleAuthService.authenticateWithAuthorizationCode("ok"))
                .thenThrow(new BusinessException("token exchange failed"));

        mockMvc.perform(get("/auth/google/callback")
                        .param("code", "ok")
                        .param("state", "s")
                        .cookie(new jakarta.servlet.http.Cookie("google_oauth_state", "s")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", Objects.requireNonNull(org.hamcrest.Matchers.containsString("error=business_error"))));
    }

    @Test
    void callbackRedirectsWithErrorWhenCodeIsBlank() throws Exception {
        when(properties.getFrontendRedirectUri()).thenReturn("http://frontend/app/oauth");

        mockMvc.perform(get("/auth/google/callback")
                        .param("code", "   ")
                        .param("state", "s")
                        .cookie(new jakarta.servlet.http.Cookie("google_oauth_state", "s")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", Objects.requireNonNull(org.hamcrest.Matchers.containsString("error=missing_code"))));
    }
}
