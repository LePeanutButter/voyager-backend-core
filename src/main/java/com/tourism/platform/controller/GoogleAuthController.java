package com.tourism.platform.controller;

import com.tourism.platform.config.GoogleOAuthProperties;
import com.tourism.platform.dto.UserDto;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/google")
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthController {

    private static final String LOCATION_HEADER = "Location";

    private final GoogleOAuthProperties properties;
    private final GoogleAuthService googleAuthService;

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
        response.setHeader(LOCATION_HEADER, authorizeUrl);
    }

    @GetMapping("/callback")
    @Operation(summary = "Google OAuth2 callback", description = "Exchanges code for Google token, fetches profile, upserts user, and redirects frontend with JWT")
    public void callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            HttpServletResponse response) {

        if (error != null && !error.isBlank()) {
            redirectWithError(response, "oauth_error",
                    "Google OAuth2 error: " + error + (errorDescription != null ? " - " + errorDescription : ""));
            return;
        }
        if (code == null || code.isBlank()) {
            redirectWithError(response, "missing_code", "Authorization code is required");
            return;
        }

        try {
            UserDto userDto = googleAuthService.authenticateWithAuthorizationCode(code);
            String redirect = frontendCallbackBase() + "?token=" + url(userDto.getToken());
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.setHeader(LOCATION_HEADER, redirect);
        } catch (BusinessException ex) {
            redirectWithError(response, "business_error", ex.getMessage());
        } catch (Exception ex) {
            redirectWithError(response, "auth_failed", ex.getMessage());
        }
    }

    private void redirectWithError(HttpServletResponse response, String error, String message) {
        String redirect = frontendCallbackBase() +
                "?error=" + url(error) +
                "&message=" + url(message != null ? message : "Authentication failed");
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", redirect);
    }

    private String frontendCallbackBase() {
        if (properties.getFrontendRedirectUri() == null || properties.getFrontendRedirectUri().isBlank()) {
            throw new BusinessException("Google frontend-redirect-uri is not configured");
        }
        return properties.getFrontendRedirectUri();
    }

    private String url(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}

