package com.tourism.platform.controller;

import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.ShareActivityRequest;
import com.tourism.platform.dto.SharedActivityDecisionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import com.tourism.platform.service.SharedActivityService;
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

@RestController
@RequestMapping
@Validated
public class SharedActivityController {
    private static final Logger log = LoggerFactory.getLogger(SharedActivityController.class);
    private static final String EVENT_ENTRY = "event=controller_entry endpoint={} userId={} resourceId={}";
    private static final String EVENT_EXIT = "event=controller_exit endpoint={} userId={} durationMs={} status={}";
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final SharedActivityService sharedActivityService;

    public SharedActivityController(SharedActivityService sharedActivityService) {
        this.sharedActivityService = sharedActivityService;
    }

    @PostMapping("/activities/{activityId}/share")
    public ResponseEntity<ApiResponse<SharedActivityResponse>> shareActivity(
            @PathVariable Long activityId,
            @Valid @RequestBody ShareActivityRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        long startNanos = System.nanoTime();
        log.info(EVENT_ENTRY,
                httpServletRequest.getRequestURI(), MDC.get("userId"), activityId);
        SharedActivityResponse result = sharedActivityService.shareActivity(
                activityId,
                request.getReceiverId(),
                authentication.getName()
        );
        log.info(EVENT_EXIT,
                httpServletRequest.getRequestURI(), MDC.get("userId"), (System.nanoTime() - startNanos) / 1_000_000, STATUS_SUCCESS);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(HttpStatus.CREATED.value(), "Activity shared successfully", result, httpServletRequest.getRequestURI())
        );
    }

    @PatchMapping("/shared-activities/{id}")
    public ResponseEntity<ApiResponse<SharedActivityResponse>> updateSharedActivityStatus(
            @PathVariable Long id,
            @Valid @RequestBody SharedActivityDecisionRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        long startNanos = System.nanoTime();
        log.info(EVENT_ENTRY,
                httpServletRequest.getRequestURI(), MDC.get("userId"), id);
        SharedActivityResponse result = sharedActivityService.resolveSharedActivity(
                id,
                request,
                authentication.getName()
        );
        log.info(EVENT_EXIT,
                httpServletRequest.getRequestURI(), MDC.get("userId"), (System.nanoTime() - startNanos) / 1_000_000, STATUS_SUCCESS);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Shared activity updated successfully", result, httpServletRequest.getRequestURI())
        );
    }
}
