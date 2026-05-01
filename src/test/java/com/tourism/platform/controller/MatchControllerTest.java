package com.tourism.platform.controller;

import com.tourism.platform.dto.MatchResponseDto;
import com.tourism.platform.service.MatchingService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MatchControllerTest {

    @Mock
    private MatchingService matchingService;

    private MockMvc mockMvc;
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        MatchController controller = new MatchController(matchingService, meterRegistry);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getMatches_ReturnsLimitedResults() throws Exception {
        MatchResponseDto dto = MatchResponseDto.builder()
                .userId(2L)
                .username("bob")
                .destination("Paris")
                .score(88.0)
                .destinationPoints(10)
                .datePoints(5)
                .interestPoints(3)
                .build();
        when(matchingService.getMatches(eq("Paris"), any(LocalDate.class), any(LocalDate.class), isNull()))
                .thenReturn(List.of(dto));

        try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("userId")).thenReturn("9");

            mockMvc.perform(get("/matches")
                            .param("destination", "Paris")
                            .param("startDate", "2024-07-01")
                            .param("endDate", "2024-07-10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Matches calculated successfully (interest filtering mode: ANY)"))
                    .andExpect(jsonPath("$.data[0].userId").value(2));
        }
    }

    @Test
    void getMetricsRecorded() throws Exception {
        when(matchingService.getMatches(anyString(), any(LocalDate.class), any(LocalDate.class), any()))
                .thenReturn(List.of());

        try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("userId")).thenReturn("1");
            mockMvc.perform(get("/matches")
                            .param("destination", "Rome")
                            .param("startDate", "2024-08-01")
                            .param("endDate", "2024-08-05")
                            .param("limit", "5"))
                    .andExpect(status().isOk());
        }
    }
}
