package com.tourism.platform.controller;

import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.MatchResponseDto;
import com.tourism.platform.service.MatchingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class MatchController {

    private final MatchingService matchingService;

    @GetMapping("/matches")
    public ResponseEntity<ApiResponse<List<MatchResponseDto>>> getMatches(
            @RequestParam String destination,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) List<String> interests,
            HttpServletRequest request) {
        List<MatchResponseDto> matches = matchingService.getMatches(destination, startDate, endDate, interests);
        return ResponseEntity.ok(ApiResponse.success(
                200,
                "Matches calculated successfully (interest filtering mode: ANY)",
                matches,
                request.getRequestURI()
        ));
    }
}
