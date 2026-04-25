package com.tourism.platform.controller;

import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.ShareActivityRequest;
import com.tourism.platform.dto.SharedActivityDecisionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import com.tourism.platform.service.SharedActivityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class SharedActivityController {

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
        SharedActivityResponse result = sharedActivityService.shareActivity(
                activityId,
                request.getReceiverId(),
                authentication.getName()
        );
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
        SharedActivityResponse result = sharedActivityService.resolveSharedActivity(
                id,
                request,
                authentication.getName()
        );
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Shared activity updated successfully", result, httpServletRequest.getRequestURI())
        );
    }
}
