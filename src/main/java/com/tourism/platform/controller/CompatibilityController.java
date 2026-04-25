package com.tourism.platform.controller;

import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.CompatibilityMatchRequest;
import com.tourism.platform.dto.CompatibilityMatchResponse;
import com.tourism.platform.service.CompatibilityMatchingService;
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

@RestController
@RequestMapping("/compatibility")
@Validated
public class CompatibilityController {
    private static final Logger log = LoggerFactory.getLogger(CompatibilityController.class);

    private final CompatibilityMatchingService compatibilityMatchingService;

    public CompatibilityController(CompatibilityMatchingService compatibilityMatchingService) {
        this.compatibilityMatchingService = compatibilityMatchingService;
    }

    @PostMapping("/matches")
    public ResponseEntity<ApiResponse<List<CompatibilityMatchResponse>>> findMatches(
            @Valid @RequestBody CompatibilityMatchRequest request,
            Authentication authentication,
            HttpServletRequest httpServletRequest) {
        long startNanos = System.nanoTime();
        log.info("event=controller_entry endpoint={} userId={} destination={}",
                httpServletRequest.getRequestURI(), MDC.get("userId"), request.getDestination());
        List<CompatibilityMatchResponse> matches = compatibilityMatchingService.findMatches(request, authentication.getName());
        log.info("event=controller_exit endpoint={} userId={} durationMs={} status=SUCCESS",
                httpServletRequest.getRequestURI(), MDC.get("userId"), (System.nanoTime() - startNanos) / 1_000_000);
        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK.value(), "Matches generated successfully", matches, httpServletRequest.getRequestURI())
        );
    }
}
