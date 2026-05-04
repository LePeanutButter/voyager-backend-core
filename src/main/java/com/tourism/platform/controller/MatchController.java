package com.tourism.platform.controller;

import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.MatchResponseDto;
import com.tourism.platform.service.MatchingService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
@Validated
public class MatchController {
    private static final Logger log = LoggerFactory.getLogger(MatchController.class);
    private static final int MAX_LIMIT = 100;
    private static final String EVENT_ENTRY = "event=controller_entry endpoint={} userId={}";
    private static final String EVENT_EXIT = "event=controller_exit endpoint={} userId={} resultCount={} durationMs={} status={}";
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final MatchingService matchingService;
    private final MeterRegistry meterRegistry;

    @GetMapping("/matches")
        /**
         * Endpoint to find matching travelers based on destination, date range and interests.
         *
         * @param destination required destination string (max 120 chars)
         * @param startDate   requested start date (inclusive)
         * @param endDate     requested end date (inclusive)
         * @param interests   optional list of interests used to bias matches
         * @param limit       maximum number of results requested (defaults to 20)
         * @param request     current HTTP servlet request
         * @return ResponseEntity containing an ApiResponse with a list of MatchResponseDto
         * @throws IllegalArgumentException for invalid input
         */
        public ResponseEntity<ApiResponse<List<MatchResponseDto>>> getMatches(
            @RequestParam @NotBlank @Size(max = 120) String destination,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) List<String> interests,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            HttpServletRequest request) {
        String endpoint = request.getRequestURI();
        String userId = MDC.get("userId");
        long start = System.nanoTime();
        log.info(EVENT_ENTRY, endpoint, userId);

        List<MatchResponseDto> matches = matchingService.getMatches(destination, startDate, endDate, interests);
        List<MatchResponseDto> limitedMatches = matches.stream().limit(Math.min(MAX_LIMIT, limit)).toList();

        Counter.builder("matching_requests_total")
                .description("Total matching requests")
                .register(meterRegistry)
                .increment();
        Timer.builder("matching_execution_time")
                .description("Matching execution time")
                .register(meterRegistry)
                .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);

        log.info(EVENT_EXIT,
                endpoint, userId, limitedMatches.size(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), STATUS_SUCCESS);

        return ResponseEntity.ok(ApiResponse.success(
                200,
                "Matches calculated successfully (interest filtering mode: ANY)",
                limitedMatches,
                request.getRequestURI()
        ));
    }
}
