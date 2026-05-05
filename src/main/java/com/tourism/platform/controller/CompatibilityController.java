package com.tourism.platform.controller;

import com.tourism.platform.config.OpenApiConfig;
import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.CompatibilityMatchRequest;
import com.tourism.platform.dto.CompatibilityMatchResponse;
import com.tourism.platform.service.CompatibilityMatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/compatibility")
@Validated
@Tag(name = "Compatibility matching", description = "Scores candidate travelers against the authenticated user using interests, destination overlap, and other signals.")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
public class CompatibilityController {
    private static final Logger log = LoggerFactory.getLogger(CompatibilityController.class);
    private static final String EVENT_ENTRY = "event=controller_entry endpoint={} userId={}";
    private static final String EVENT_EXIT = "event=controller_exit endpoint={} userId={} durationMs={} status={}";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String USER_ID = "userId";

    private final CompatibilityMatchingService compatibilityMatchingService;

    public CompatibilityController(CompatibilityMatchingService compatibilityMatchingService) {
        this.compatibilityMatchingService = compatibilityMatchingService;
    }

    @PostMapping("/matches")
    @Operation(
            summary = "Find compatibility-ranked travelers",
            description = "Computes compatibility scores for the authenticated user from the JSON body (destination, dates, optional interests).")
    /**
     * Compute compatibility matches for the authenticated user based on the request payload.
     *
     * @param request            compatibility match request containing criteria
     * @param authentication     authentication principal of the requesting user
     * @param httpServletRequest current HTTP request (used to populate response path)
     * @return ResponseEntity wrapping an ApiResponse with a list of CompatibilityMatchResponse
     */
    public ResponseEntity<ApiResponse<List<CompatibilityMatchResponse>>> findMatches(
            @Valid @RequestBody CompatibilityMatchRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        long startNanos = System.nanoTime();
        String path = safePath(httpServletRequest);
        String principal = authenticatedUsername(authentication);
        String userId = MDC.get(USER_ID);
        log.info(EVENT_ENTRY, path, userId);
        List<CompatibilityMatchResponse> matches = compatibilityMatchingService.findMatches(request, principal);
        log.info(EVENT_EXIT,
                path, userId, (System.nanoTime() - startNanos) / 1_000_000, STATUS_SUCCESS);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Matches generated successfully", matches, path)
        );
    }

    private String safePath(HttpServletRequest request) {
        /**
         * Safely extract the request URI or return an empty string when request is null.
         *
         * @param request HTTP servlet request or null
         * @return request URI string or empty string
         */
        return request != null ? request.getRequestURI() : "";
    }

    private String authenticatedUsername(Authentication authentication) {
        /**
         * Return the username from the Authentication object.
         *
         * @param authentication Spring Security authentication (must not be null)
         * @return username string
         */
        return Objects.requireNonNull(authentication, "authentication is required").getName();
    }
}
