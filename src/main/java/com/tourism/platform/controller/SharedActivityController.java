package com.tourism.platform.controller;

import com.tourism.platform.config.OpenApiConfig;
import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.ShareActivityRequest;
import com.tourism.platform.dto.SharedActivityDecisionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import com.tourism.platform.service.SharedActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import java.util.Objects;

@RestController
@RequestMapping
@Validated
@Tag(name = "Activity sharing", description = "Share a travel-plan activity with another user; accept or reject shares (JWT required).")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
public class SharedActivityController {
    private static final Logger log = LoggerFactory.getLogger(SharedActivityController.class);
    private static final String EVENT_ENTRY = "event=controller_entry endpoint={} userId={} resourceId={}";
    private static final String EVENT_EXIT = "event=controller_exit endpoint={} userId={} durationMs={} status={}";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String USER_ID = "userId";

    private final SharedActivityService sharedActivityService;

    public SharedActivityController(SharedActivityService sharedActivityService) {
        this.sharedActivityService = sharedActivityService;
    }

    @PostMapping("/activities/{activityId}/share")
    @Operation(
            summary = "Share a travel-plan activity",
            description = "Creates a pending shared-activity request from the authenticated user to the receiver for the given travel-plan activity id. "
                    + "Returns 201 on success; 400/404/409 for validation, not found, or conflict.")
    /**
     * Share an activity with another user.
     *
     * @param activityId         id of the activity to share
     * @param request            request DTO containing receiver information and optional message
     * @param authentication     Spring Security authentication of the requester
     * @param httpServletRequest current HTTP request (used for logging and response path)
     * @return ResponseEntity with ApiResponse containing the created SharedActivityResponse and HTTP 201
     */
    public ResponseEntity<ApiResponse<SharedActivityResponse>> shareActivity(
            @Parameter(description = "Travel-plan activity id (travel_plan_activities.id)", required = true)
            @PathVariable Long activityId,
            @Valid @RequestBody ShareActivityRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        long startNanos = System.nanoTime();
        String path = safePath(httpServletRequest);
        String userId = MDC.get(USER_ID);
        String principal = authenticatedUsername(authentication);
        log.info(EVENT_ENTRY,
                path, userId, activityId);
        SharedActivityResponse result = sharedActivityService.shareActivity(
                Objects.requireNonNull(activityId),
                Objects.requireNonNull(request.getReceiverId()),
                principal
        );
        log.info(EVENT_EXIT,
                path, userId, (System.nanoTime() - startNanos) / 1_000_000, STATUS_SUCCESS);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(HttpStatus.CREATED.value(), "Activity shared successfully", result, path)
        );
    }

    @PatchMapping("/shared-activities/{id}")
    @Operation(
            summary = "Update shared-activity decision",
            description = "Accept, reject, or cancel a shared activity. The authenticated user must be allowed to act on the record. "
                    + "Returns 200 on success; 400/404/409 otherwise.")
    /**
     * Update the status of a previously shared activity (accept/reject/cancel).
     *
     * @param id                  id of the shared activity record to update
     * @param request             decision request DTO containing the new status and optional metadata
     * @param authentication      Spring Security authentication of the acting user
     * @param httpServletRequest  current HTTP request (used for logging and response path)
     * @return ResponseEntity with ApiResponse containing the updated SharedActivityResponse
     */
    public ResponseEntity<ApiResponse<SharedActivityResponse>> updateSharedActivityStatus(
            @Parameter(description = "shared_activities.id", required = true) @PathVariable Long id,
            @Valid @RequestBody SharedActivityDecisionRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        long startNanos = System.nanoTime();
        String path = safePath(httpServletRequest);
        String userId = MDC.get(USER_ID);
        String principal = authenticatedUsername(authentication);
        log.info(EVENT_ENTRY,
                path, userId, id);
        SharedActivityResponse result = sharedActivityService.resolveSharedActivity(
                Objects.requireNonNull(id),
                request,
                principal
        );
        log.info(EVENT_EXIT,
                path, userId, (System.nanoTime() - startNanos) / 1_000_000, STATUS_SUCCESS);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Shared activity updated successfully", result, path)
        );
    }

    private String safePath(HttpServletRequest request) {
                /**
                 * Safely obtain the request URI, returning an empty string when request is null.
                 *
                 * @param request HTTP servlet request
                 * @return request URI or empty string when request is null
                 */
                return request != null ? request.getRequestURI() : "";
    }

    private String authenticatedUsername(Authentication authentication) {
                /**
                 * Resolve the authenticated principal's username.
                 *
                 * @param authentication Spring Security authentication object (must not be null)
                 * @return principal username
                 */
                return Objects.requireNonNull(authentication, "authentication is required").getName();
    }
}
