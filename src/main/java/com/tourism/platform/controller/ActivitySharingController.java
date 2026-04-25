package com.tourism.platform.controller;

import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.ShareActivityRequest;
import com.tourism.platform.dto.SharedActivityActionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.service.SharedActivityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ActivitySharingController {

    private final SharedActivityService sharedActivityService;
    private final UserRepository userRepository;

    @PostMapping("/activities/{activityId}/share")
    public ResponseEntity<ApiResponse<SharedActivityResponse>> shareActivity(
            @PathVariable Long activityId,
            @Valid @RequestBody ShareActivityRequest requestBody,
            HttpServletRequest request,
            Authentication authentication) {

        Long currentUserId = resolveCurrentUserId(authentication);
        SharedActivityResponse responseData = sharedActivityService.shareActivity(
                activityId,
                requestBody.getReceiverUserId(),
                currentUserId
        );

        ApiResponse<SharedActivityResponse> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Activity shared successfully",
                responseData,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/shared-activities/{id}")
    public ResponseEntity<ApiResponse<SharedActivityResponse>> updateSharedActivity(
            @PathVariable Long id,
            @Valid @RequestBody SharedActivityActionRequest requestBody,
            HttpServletRequest request,
            Authentication authentication) {
        Long currentUserId = resolveCurrentUserId(authentication);
        SharedActivityResponse responseData = sharedActivityService.updateSharedActivity(
                id,
                requestBody.getAction(),
                currentUserId
        );

        ApiResponse<SharedActivityResponse> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Shared activity updated successfully",
                responseData,
                request.getRequestURI()
        );
        return ResponseEntity.ok(response);
    }

    private Long resolveCurrentUserId(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found: " + username))
                .getId();
    }
}
