package com.tourism.platform.controller;

import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.CompatibilityMatchRequest;
import com.tourism.platform.dto.CompatibilityMatchResponse;
import com.tourism.platform.service.CompatibilityMatchingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/compatibility")
public class CompatibilityController {

    private final CompatibilityMatchingService compatibilityMatchingService;

    public CompatibilityController(CompatibilityMatchingService compatibilityMatchingService) {
        this.compatibilityMatchingService = compatibilityMatchingService;
    }

    @PostMapping("/matches")
    public ResponseEntity<ApiResponse<List<CompatibilityMatchResponse>>> findMatches(
            @Valid @RequestBody CompatibilityMatchRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        List<CompatibilityMatchResponse> matches = compatibilityMatchingService.findMatches(request, authentication.getName());
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Matches generated successfully", matches, httpServletRequest.getRequestURI())
        );
    }
}
