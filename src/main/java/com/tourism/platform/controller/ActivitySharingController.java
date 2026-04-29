package com.tourism.platform.controller;

import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.ShareActivityRequest;
import com.tourism.platform.dto.SharedActivityActionRequest;
import com.tourism.platform.dto.SharedActivityDecisionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.SharedActivityDecisionAction;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.service.SharedActivityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Validated
@RequestMapping("/legacy")
public class ActivitySharingController {
    private static final Logger log = LoggerFactory.getLogger(ActivitySharingController.class);
    private static final String EVENT_ENTRY = "event=controller_entry endpoint={} userId={} resourceId={}";
    private static final String EVENT_EXIT = "event=controller_exit endpoint={} userId={} durationMs={} status={}";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String USER_ID = "userId";

    private final SharedActivityService sharedActivityService;
    private final UserRepository userRepository;

    @PostMapping("/activities/{activityId}/share")
    public ResponseEntity<ApiResponse<SharedActivityResponse>> shareActivity(
            @PathVariable Long activityId,
            @Valid @RequestBody ShareActivityRequest requestBody,
            HttpServletRequest request,
            Authentication authentication) {
        long startNanos = System.nanoTime();
        String path = safePath(request);
        String userId = MDC.get(USER_ID);
        String principal = authenticatedUsername(authentication);
        log.info(EVENT_ENTRY,
                path, userId, activityId);

        SharedActivityResponse responseData = sharedActivityService.shareActivity(
                activityId,
                requestBody.getReceiverId(),
                principal
        );

        ApiResponse<SharedActivityResponse> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Activity shared successfully",
                responseData,
                path
        );
        log.info(EVENT_EXIT,
                path, userId, (System.nanoTime() - startNanos) / 1_000_000, STATUS_SUCCESS);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/shared-activities/{id}")
    public ResponseEntity<ApiResponse<SharedActivityResponse>> updateSharedActivity(
            @PathVariable Long id,
            @Valid @RequestBody SharedActivityActionRequest requestBody,
            HttpServletRequest request,
            Authentication authentication) {
        long startNanos = System.nanoTime();
        String path = safePath(request);
        String userId = MDC.get(USER_ID);
        String principal = authenticatedUsername(authentication);
        log.info(EVENT_ENTRY,
                path, userId, id);
        resolveCurrentUserId(authentication);
        SharedActivityDecisionRequest decisionRequest = new SharedActivityDecisionRequest();
        decisionRequest.setAction(requestBody.getAction() == SharedActivityActionRequest.SharedActivityAction.ACCEPT
                ? SharedActivityDecisionAction.ACCEPT
                : SharedActivityDecisionAction.REJECT);
        SharedActivityResponse responseData = sharedActivityService.resolveSharedActivity(id, decisionRequest, principal);

        ApiResponse<SharedActivityResponse> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Shared activity updated successfully",
                responseData,
                path
        );
        log.info(EVENT_EXIT,
                path, userId, (System.nanoTime() - startNanos) / 1_000_000, STATUS_SUCCESS);
        return ResponseEntity.ok(response);
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        String username = authenticatedUsername(authentication);
        return userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username))
                .getId();
    }

    private String safePath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "";
    }

    private String authenticatedUsername(Authentication authentication) {
        return Objects.requireNonNull(authentication, "authentication is required").getName();
    }
}
