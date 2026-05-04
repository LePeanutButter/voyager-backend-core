package com.tourism.platform.controller;

import com.tourism.platform.config.GoogleOAuthProperties;
import com.tourism.platform.dto.UserDto;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class GoogleAuthController {

    private static final String LOCATION_HEADER = "Location";

    private final GoogleOAuthProperties properties;
    private final GoogleAuthService googleAuthService;

    @GetMapping("/login")
    @Operation(summary = "Start Google OAuth2 login", description = "Redirects the user to Google authorization endpoint")
    /**
     * Initiate the Google OAuth2 login flow by redirecting the client to Google's
     * authorization endpoint.
     *
     * @param response HTTP servlet response used to set a 302 redirect and Location header
     * @throws BusinessException if the Google client id is not configured
     */
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
    /**
     * OAuth2 callback endpoint that receives the authorization code from Google.
     *
     * Exchanges the code for an access token, upserts the user and redirects the
     * client to the frontend callback URL with a JWT token or error details.
     *
     * @param code             authorization code (may be null when an error occurred)
     * @param error            optional error code returned by the provider
     * @param errorDescription optional error description returned by the provider
     * @param response         HTTP servlet response used to redirect the client
     */
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
        /**
         * Helper to redirect the client to the frontend callback URL with error details.
         *
         * @param response HTTP servlet response used for the redirect
         * @param error    short error code to include
         * @param message  human readable error message
         */
        String redirect = frontendCallbackBase() +
            "?error=" + url(error) +
            "&message=" + url(message != null ? message : "Authentication failed");
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader(LOCATION_HEADER, redirect);
    }

    private String frontendCallbackBase() {
        /**
         * Resolve the configured frontend callback base URL.
         *
         * @return frontend callback base URL
         * @throws BusinessException when the property is not configured
         */
        if (properties.getFrontendRedirectUri() == null || properties.getFrontendRedirectUri().isBlank()) {
            throw new BusinessException("Google frontend-redirect-uri is not configured");
        }
        return properties.getFrontendRedirectUri();
    }

    private String url(String v) {
        /**
         * URL-encode the provided value using UTF-8.
         *
         * @param v raw value to encode
         * @return encoded string
         */
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}

