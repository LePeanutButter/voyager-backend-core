package com.tourism.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import com.tourism.platform.dto.CompatibilityMatchRequest;
import com.tourism.platform.dto.CompatibilityMatchResponse;
import com.tourism.platform.service.CompatibilityMatchingService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CompatibilityControllerTest {

    @Mock
    private CompatibilityMatchingService compatibilityMatchingService;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private CompatibilityController compatibilityController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(compatibilityController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void findMatches_WithValidRequest_ShouldReturnMatches() throws Exception {
        // Given
        CompatibilityMatchRequest matchRequest = new CompatibilityMatchRequest();
        matchRequest.setDestination("Paris");
        matchRequest.setStartDate(java.time.LocalDate.now().plusDays(1));
        matchRequest.setEndDate(java.time.LocalDate.now().plusDays(7));

        CompatibilityMatchResponse matchResponse = new CompatibilityMatchResponse();
        matchResponse.setUserId(2L);
        matchResponse.setTotalScore(85.5);

        List<CompatibilityMatchResponse> matches = List.of(matchResponse);

        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(request.getRequestURI()).thenReturn("/compatibility/matches");
        when(compatibilityMatchingService.findMatches(any(CompatibilityMatchRequest.class), eq("testuser")))
                .thenReturn(matches);

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            mdcMock.when(() -> MDC.get("userId")).thenReturn("1");

            // When & Then
            mockMvc.perform(post("/compatibility/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(matchRequest))
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Matches generated successfully"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].userId").value(2))
                    .andExpect(jsonPath("$.data[0].totalScore").value(85.5));
        }

        verify(compatibilityMatchingService).findMatches(any(CompatibilityMatchRequest.class), eq("testuser"));
    }

    @Test
    void findMatches_WithEmptyResults_ShouldReturnEmptyList() throws Exception {
        // Given
        CompatibilityMatchRequest matchRequest = new CompatibilityMatchRequest();
        matchRequest.setDestination("RemoteLocation");
        matchRequest.setStartDate(java.time.LocalDate.now().plusDays(1));
        matchRequest.setEndDate(java.time.LocalDate.now().plusDays(7));

        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(request.getRequestURI()).thenReturn("/compatibility/matches");
        when(compatibilityMatchingService.findMatches(any(CompatibilityMatchRequest.class), eq("testuser")))
                .thenReturn(List.of());

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            mdcMock.when(() -> MDC.get("userId")).thenReturn("1");

            // When & Then
            mockMvc.perform(post("/compatibility/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(matchRequest))
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Matches generated successfully"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        verify(compatibilityMatchingService).findMatches(any(CompatibilityMatchRequest.class), eq("testuser"));
    }

    @Test
    void findMatches_WithMultipleMatches_ShouldReturnAllMatches() throws Exception {
        // Given
        CompatibilityMatchRequest matchRequest = new CompatibilityMatchRequest();
        matchRequest.setDestination("Paris");
        matchRequest.setStartDate(java.time.LocalDate.now().plusDays(1));
        matchRequest.setEndDate(java.time.LocalDate.now().plusDays(7));

        CompatibilityMatchResponse match1 = new CompatibilityMatchResponse();
        match1.setUserId(2L);
        match1.setTotalScore(90.0);

        CompatibilityMatchResponse match2 = new CompatibilityMatchResponse();
        match2.setUserId(3L);
        match2.setTotalScore(75.5);

        List<CompatibilityMatchResponse> matches = List.of(match1, match2);

        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(request.getRequestURI()).thenReturn("/compatibility/matches");
        when(compatibilityMatchingService.findMatches(any(CompatibilityMatchRequest.class), eq("testuser")))
                .thenReturn(matches);

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            mdcMock.when(() -> MDC.get("userId")).thenReturn("1");

            // When & Then
            mockMvc.perform(post("/compatibility/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(matchRequest))
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].userId").value(2))
                    .andExpect(jsonPath("$.data[1].userId").value(3));
        }

        verify(compatibilityMatchingService).findMatches(any(CompatibilityMatchRequest.class), eq("testuser"));
    }

    @Test
    void findMatches_WithNullAuthentication_ShouldThrowException() throws Exception {
        // Given
        CompatibilityMatchRequest matchRequest = new CompatibilityMatchRequest();
        matchRequest.setDestination("Paris");
        matchRequest.setStartDate(java.time.LocalDate.now().plusDays(1));
        matchRequest.setEndDate(java.time.LocalDate.now().plusDays(7));

        // When & Then
        assertThrows(ServletException.class, () -> {
            mockMvc.perform(post("/compatibility/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(matchRequest)))
                    .andExpect(status().isBadRequest());
        });

        verify(compatibilityMatchingService, never()).findMatches(any(), anyString());
    }

    @Test
    void testPrivateMethods() throws Exception {
        // Test safePath using reflection
        java.lang.reflect.Method safePath = CompatibilityController.class.getDeclaredMethod("safePath", HttpServletRequest.class);
        safePath.setAccessible(true);

        when(request.getRequestURI()).thenReturn("/test/path");
        String path = (String) safePath.invoke(compatibilityController, request);
        assertEquals("/test/path", path);

        String nullPath = (String) safePath.invoke(compatibilityController, (HttpServletRequest) null);
        assertEquals("", nullPath);

        // Test authenticatedUsername using reflection
        java.lang.reflect.Method authenticatedUsername = CompatibilityController.class.getDeclaredMethod("authenticatedUsername", Authentication.class);
        authenticatedUsername.setAccessible(true);

        when(authentication.getName()).thenReturn("testuser");
        String username = (String) authenticatedUsername.invoke(compatibilityController, authentication);
        assertEquals("testuser", username);

        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            authenticatedUsername.invoke(compatibilityController, (Authentication) null);
        });
    }

    @Test
    void constructor_ShouldInitializeService() {
        // Given
        CompatibilityMatchingService service = mock(CompatibilityMatchingService.class);

        // When
        CompatibilityController controller = new CompatibilityController(service);

        // Then
        assertNotNull(controller);
        // The service should be initialized (we can't directly access it, but the controller should work)
    }

    @Test
    void findMatches_WithServiceException_ShouldPropagateException() throws Exception {
        // Given
        CompatibilityMatchRequest matchRequest = new CompatibilityMatchRequest();
        matchRequest.setDestination("Paris");
        matchRequest.setStartDate(java.time.LocalDate.now().plusDays(1));
        matchRequest.setEndDate(java.time.LocalDate.now().plusDays(7));

        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(request.getRequestURI()).thenReturn("/compatibility/matches");
        when(compatibilityMatchingService.findMatches(any(CompatibilityMatchRequest.class), eq("testuser")))
                .thenThrow(new RuntimeException("Service error"));

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            mdcMock.when(() -> MDC.get("userId")).thenReturn("1");

            // When & Then
            assertThrows(ServletException.class, () -> {
                mockMvc.perform(post("/compatibility/matches")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(matchRequest))
                                .principal(authentication))
                        .andExpect(status().isInternalServerError());
            });
        }

        verify(compatibilityMatchingService).findMatches(any(CompatibilityMatchRequest.class), eq("testuser"));
    }

    @Test
    void findMatches_WithInvalidRequest_ShouldHandleValidationError() throws Exception {
        // Given - Invalid request with missing required fields
        String invalidRequest = "{}";

        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(request.getRequestURI()).thenReturn("/compatibility/matches");

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            mdcMock.when(() -> MDC.get("userId")).thenReturn("1");

            // When & Then
            mockMvc.perform(post("/compatibility/matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest)
                            .principal(authentication))
                    .andExpect(status().isBadRequest());
        }

        verify(compatibilityMatchingService, never()).findMatches(any(), anyString());
    }
}
