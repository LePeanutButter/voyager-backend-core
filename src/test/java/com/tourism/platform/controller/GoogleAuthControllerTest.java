package com.tourism.platform.controller;

import com.tourism.platform.config.GoogleOAuthProperties;
import com.tourism.platform.dto.UserDto;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.service.GoogleAuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleAuthControllerTest {

    @Mock
    private GoogleOAuthProperties properties;

    @Mock
    private GoogleAuthService googleAuthService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private GoogleAuthController googleAuthController;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleAuthController, "properties", properties);
    }

    @Test
    void login_WithValidConfig_ShouldRedirectToGoogle() throws IOException {
        // Given
        when(properties.getClientId()).thenReturn("test-client-id");
        when(properties.getRedirectUri()).thenReturn("http://localhost:8080/callback");
        when(properties.getScopes()).thenReturn("openid email profile");

        // When
        googleAuthController.login(response);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader(eq("Location"), contains("accounts.google.com/o/oauth2/v2/auth"));
        verify(response).setHeader(eq("Location"), contains("client_id=test-client-id"));
        verify(response).setHeader(eq("Location"), contains("redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fcallback"));
    }

    @Test
    void login_WithMissingClientId_ShouldThrowBusinessException() {
        // Given
        when(properties.getClientId()).thenReturn(null);

        // When & Then
        assertThrows(BusinessException.class, () -> googleAuthController.login(response));
        verify(response, never()).setHeader(anyString(), anyString());
    }

    @Test
    void login_WithBlankClientId_ShouldThrowBusinessException() {
        // Given
        when(properties.getClientId()).thenReturn("");

        // When & Then
        assertThrows(BusinessException.class, () -> googleAuthController.login(response));
        verify(response, never()).setHeader(anyString(), anyString());
    }

    @Test
    void callback_WithValidCode_ShouldRedirectWithToken() throws IOException {
        // Given
        String code = "valid-auth-code";
        UserDto userDto = new UserDto();
        userDto.setToken("jwt-token");
        
        when(properties.getFrontendRedirectUri()).thenReturn("http://localhost:3000");
        when(googleAuthService.authenticateWithAuthorizationCode(code)).thenReturn(userDto);

        // When
        googleAuthController.callback(code, null, null, response);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader(eq("Location"), contains("token=jwt-token"));
        verify(googleAuthService).authenticateWithAuthorizationCode(code);
    }

    @Test
    void callback_WithOAuthError_ShouldRedirectWithError() throws IOException {
        // Given
        String error = "access_denied";
        String errorDescription = "User denied access";
        
        when(properties.getFrontendRedirectUri()).thenReturn("http://localhost:3000");

        // When
        googleAuthController.callback(null, error, errorDescription, response);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader(eq("Location"), contains("error=oauth_error&message=Google+OAuth2+error%3A+access_denied+-+User+denied+access"));
        verify(response).setHeader(eq("Location"), contains("message=User denied access"));
        verify(googleAuthService, never()).authenticateWithAuthorizationCode(anyString());
    }

    @Test
    void callback_WithMissingCode_ShouldRedirectWithError() throws IOException {
        // Given
        when(properties.getFrontendRedirectUri()).thenReturn("http://localhost:3000");

        // When
        googleAuthController.callback(null, null, null, response);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader(eq("Location"), contains("error=missing_code"));
        verify(googleAuthService, never()).authenticateWithAuthorizationCode(anyString());
    }

    @Test
    void callback_WithBlankCode_ShouldRedirectWithError() throws IOException {
        // Given
        when(properties.getFrontendRedirectUri()).thenReturn("http://localhost:3000");

        // When
        googleAuthController.callback("", null, null, response);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader(eq("Location"), contains("error=missing_code"));
        verify(googleAuthService, never()).authenticateWithAuthorizationCode(anyString());
    }

    @Test
    void callback_WithBusinessException_ShouldRedirectWithError() throws IOException {
        // Given
        String code = "valid-code";
        String errorMessage = "Invalid token";
        
        when(properties.getFrontendRedirectUri()).thenReturn("http://localhost:3000");
        when(googleAuthService.authenticateWithAuthorizationCode(code))
                .thenThrow(new BusinessException(errorMessage));

        // When
        googleAuthController.callback(code, null, null, response);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader(eq("Location"), contains("error=business_error"));
        verify(response).setHeader(eq("Location"), contains("message=" + errorMessage));
    }

    @Test
    void callback_WithGenericException_ShouldRedirectWithError() throws IOException {
        // Given
        String code = "valid-code";
        
        when(properties.getFrontendRedirectUri()).thenReturn("http://localhost:3000");
        when(googleAuthService.authenticateWithAuthorizationCode(code))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When
        googleAuthController.callback(code, null, null, response);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader(eq("Location"), contains("error=auth_failed"));
        verify(response).setHeader(eq("Location"), contains("redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fcallback"));
        verify(response).setHeader(eq("Location"), contains("message=Unexpected+error"));
    }

    @Test
    void callback_WithNullErrorMessage_ShouldUseDefaultMessage() throws IOException {
        // Given
        String code = "valid-code";
        
        when(properties.getFrontendRedirectUri()).thenReturn("http://localhost:3000");
        when(googleAuthService.authenticateWithAuthorizationCode(code))
                .thenThrow(new BusinessException(null));

        // When
        googleAuthController.callback(code, null, null, response);

        // Then
        verify(response).setStatus(HttpServletResponse.SC_FOUND);
        verify(response).setHeader(eq("Location"), contains("error=business_error"));
        verify(response).setHeader(eq("Location"), contains("message=Authentication+failed"));
    }

    @Test
    void login_ShouldIncludeStateParameterForCSRFProtection() throws IOException {
        // Given
        when(properties.getClientId()).thenReturn("test-client-id");
        when(properties.getRedirectUri()).thenReturn("http://localhost:8080/callback");
        when(properties.getScopes()).thenReturn("openid email profile");

        // When
        googleAuthController.login(response);

        // Then
        verify(response).setHeader(eq("Location"), contains("state="));
        // State should be a UUID (36 characters with dashes)
        verify(response).setHeader(eq("Location"), argThat(url -> url.split("state=")[1].split("&")[0].length() == 36));
    }

    @Test
    void login_ShouldIncludeCorrectScopesAndParameters() throws IOException {
        // Given
        when(properties.getClientId()).thenReturn("test-client-id");
        when(properties.getRedirectUri()).thenReturn("http://localhost:8080/callback");
        when(properties.getScopes()).thenReturn("openid email profile");

        // When
        googleAuthController.login(response);

        // Then
        verify(response).setHeader(eq("Location"), argThat(url -> 
            url.contains("response_type=code") &&
            url.contains("access_type=online") &&
            url.contains("include_granted_scopes=true") &&
            url.contains("scope=openid+email+profile")
        ));
    }
}
