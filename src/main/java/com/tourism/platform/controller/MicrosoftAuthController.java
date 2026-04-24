package com.tourism.platform.controller;

import com.tourism.platform.config.MicrosoftOAuthProperties;
import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.UserDto;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.service.MicrosoftAuthService;
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

@RestController
@RequestMapping("/auth/microsoft")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "OAuth2 login with Microsoft Azure AD")
public class MicrosoftAuthController {

    private final MicrosoftOAuthProperties properties;
    private final MicrosoftAuthService microsoftAuthService;

    @GetMapping("/login")
    @Operation(summary = "Start Microsoft OAuth2 login", description = "Redirects the user to Microsoft authorization endpoint")
    public void login(HttpServletResponse response) {
        if (properties.getClientId() == null || properties.getClientId().isBlank()) {
            throw new BusinessException("Azure client-id is not configured");
        }

        String tenant = (properties.getTenantId() == null || properties.getTenantId().isBlank())
                ? "common"
                : properties.getTenantId();

        String authorizeUrl = "https://login.microsoftonline.com/" + url(tenant) + "/oauth2/v2.0/authorize" +
                "?client_id=" + url(properties.getClientId()) +
                "&response_type=code" +
                "&redirect_uri=" + url(properties.getRedirectUri()) +
                "&response_mode=query" +
                "&scope=" + url(properties.getScopes());

        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", authorizeUrl);
    }

    @GetMapping("/callback")
    @Operation(summary = "Microsoft OAuth2 callback", description = "Exchanges code for Microsoft token, fetches profile, upserts user, returns platform JWT")
    public ResponseEntity<ApiResponse<UserDto>> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            HttpServletRequest request) {

        if (error != null && !error.isBlank()) {
            throw new BusinessException("Microsoft OAuth2 error: " + error +
                    (errorDescription != null ? " - " + errorDescription : ""));
        }
        if (code == null || code.isBlank()) {
            throw new BusinessException("Authorization code is required");
        }

        UserDto userDto = microsoftAuthService.authenticateWithAuthorizationCode(code);
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

