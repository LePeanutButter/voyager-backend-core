package com.tourism.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.InvocationTargetException;
import com.tourism.platform.dto.ShareActivityRequest;
import com.tourism.platform.dto.SharedActivityActionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import com.tourism.platform.model.SharedActivityDecisionAction;
import com.tourism.platform.model.User;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.service.SharedActivityService;
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

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ActivitySharingControllerTest {

    @Mock
    private SharedActivityService sharedActivityService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ActivitySharingController activitySharingController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(activitySharingController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
    }

    @Test
    void shareActivity_WithValidRequest_ShouldReturnCreatedResponse() throws Exception {
        // Given
        Long activityId = 1L;
        ShareActivityRequest shareRequest = new ShareActivityRequest();
        shareRequest.setReceiverId(2L);

        SharedActivityResponse response = new SharedActivityResponse();
        response.setId(100L);

        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(request.getRequestURI()).thenReturn("/activities/1/share");
        when(sharedActivityService.shareActivity(eq(activityId), eq(2L), eq("testuser")))
                .thenReturn(response);

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            mdcMock.when(() -> MDC.get("userId")).thenReturn("1");

            mockMvc.perform(post("/legacy/activities/{activityId}/share", activityId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(shareRequest))
                            .principal(authentication))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.message").value("Activity shared successfully"))
                    .andExpect(jsonPath("$.data.id").value(100));
        }

        verify(sharedActivityService).shareActivity(activityId, 2L, "testuser");
    }

    @Test
    void updateSharedActivity_WithAcceptAction_ShouldReturnOkResponse() throws Exception {
        // Given
        Long sharedActivityId = 1L;
        SharedActivityActionRequest actionRequest = new SharedActivityActionRequest();
        actionRequest.setAction(SharedActivityActionRequest.SharedActivityAction.ACCEPT);

        SharedActivityResponse response = new SharedActivityResponse();
        response.setId(sharedActivityId);

        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(request.getRequestURI()).thenReturn("/shared-activities/1");
        lenient().when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.of(testUser));
        when(sharedActivityService.resolveSharedActivity(eq(sharedActivityId), any(), eq("testuser")))
                .thenReturn(response);

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            mdcMock.when(() -> MDC.get("userId")).thenReturn("1");

            mockMvc.perform(patch("/legacy/shared-activities/{id}", sharedActivityId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionRequest))
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Shared activity updated successfully"))
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        verify(userRepository).findByUsernameOrEmail("testuser", "testuser");
        verify(sharedActivityService).resolveSharedActivity(eq(sharedActivityId), argThat(decision -> 
                decision.getAction() == SharedActivityDecisionAction.ACCEPT), eq("testuser"));
    }

    @Test
    void updateSharedActivity_WithRejectAction_ShouldReturnOkResponse() throws Exception {
        // Given
        Long sharedActivityId = 1L;
        SharedActivityActionRequest actionRequest = new SharedActivityActionRequest();
        actionRequest.setAction(SharedActivityActionRequest.SharedActivityAction.REJECT);

        SharedActivityResponse response = new SharedActivityResponse();
        response.setId(sharedActivityId);

        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(request.getRequestURI()).thenReturn("/shared-activities/1");
        lenient().when(userRepository.findByUsernameOrEmail(anyString(), anyString()))
                .thenReturn(Optional.of(testUser));
        when(sharedActivityService.resolveSharedActivity(eq(sharedActivityId), any(), eq("testuser")))
                .thenReturn(response);

        try (MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            mdcMock.when(() -> MDC.get("userId")).thenReturn("1");

            mockMvc.perform(patch("/legacy/shared-activities/{id}", sharedActivityId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(actionRequest))
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Shared activity updated successfully"));
        }

        verify(sharedActivityService).resolveSharedActivity(eq(sharedActivityId), argThat(decision -> 
                decision.getAction() == SharedActivityDecisionAction.REJECT), eq("testuser"));
    }

    @Test
    void testPrivateMethods() throws Exception {
        // Test resolveCurrentUserId using reflection
        Method resolveCurrentUserId = ActivitySharingController.class.getDeclaredMethod("resolveCurrentUserId", Authentication.class);
        resolveCurrentUserId.setAccessible(true);
        
        when(authentication.getName()).thenReturn("testuser");
        when(userRepository.findByUsernameOrEmail("testuser", "testuser"))
                .thenReturn(Optional.of(testUser));

        Long userId = (Long) resolveCurrentUserId.invoke(activitySharingController, authentication);
        assertEquals(1L, userId);

        // Test safePath using reflection
        Method safePath = ActivitySharingController.class.getDeclaredMethod("safePath", HttpServletRequest.class);
        safePath.setAccessible(true);
        
        when(request.getRequestURI()).thenReturn("/test/path");
        String path = (String) safePath.invoke(activitySharingController, request);
        assertEquals("/test/path", path);
        
        String nullPath = (String) safePath.invoke(activitySharingController, (HttpServletRequest) null);
        assertEquals("", nullPath);

        // Test authenticatedUsername using reflection
        Method authenticatedUsername = ActivitySharingController.class.getDeclaredMethod("authenticatedUsername", Authentication.class);
        authenticatedUsername.setAccessible(true);
        
        when(authentication.getName()).thenReturn("testuser");
        String username = (String) authenticatedUsername.invoke(activitySharingController, authentication);
        assertEquals("testuser", username);
        
        assertThrows(java.lang.reflect.InvocationTargetException.class, () -> {
            authenticatedUsername.invoke(activitySharingController, (Authentication) null);
        });
    }
}
