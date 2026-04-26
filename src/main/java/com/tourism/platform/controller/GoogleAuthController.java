package com.tourism.platform.controller;

import com.tourism.platform.config.GoogleOAuthProperties;
import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.UserDto;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/auth/google")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "OAuth2 login with Google")
public class GoogleAuthController {

    private final GoogleOAuthProperties properties;
    private final GoogleAuthService googleAuthService;

    @GetMapping("/login")
    @Operation(summary = "Start Google OAuth2 login", description = "Redirects the user to Google authorization endpoint")
    public void login(HttpServletResponse response) {
        if (properties.getClientId() == null || properties.getClientId().isBlank()) {
            throw new BusinessException("Google client-id is not configured");
        }

        // Minimal CSRF protection for the authorization response
        String state = UUID.randomUUID().toString();

        String authorizeUrl = "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + url(properties.getClientId()) +
                "&response_type=code" +
                "&redirect_uri=" + url(properties.getRedirectUri()) +
                "&scope=" + url(properties.getScopes()) +
                "&access_type=online" +
                "&include_granted_scopes=true" +
                "&state=" + url(state);

        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", authorizeUrl);
    }

    @GetMapping("/callback")
    @Operation(summary = "Google OAuth2 callback", description = "Exchanges code for Google token, fetches profile, upserts user, returns platform JWT")
    public ResponseEntity<ApiResponse<UserDto>> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            HttpServletRequest request) {

        if (error != null && !error.isBlank()) {
            throw new BusinessException("Google OAuth2 error: " + error +
                    (errorDescription != null ? " - " + errorDescription : ""));
        }
        if (code == null || code.isBlank()) {
            throw new BusinessException("Authorization code is required");
        }

        UserDto userDto = googleAuthService.authenticateWithAuthorizationCode(code);
        ApiResponse<UserDto> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Authentication successful",
                userDto,
                request.getRequestURI()
        );
        return ResponseEntity.ok(response);
    }

    private String url(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}

