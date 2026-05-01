package com.tourism.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.ShareActivityRequest;
import com.tourism.platform.dto.SharedActivityDecisionRequest;
import com.tourism.platform.dto.SharedActivityResponse;
import com.tourism.platform.service.SharedActivityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SharedActivityControllerTest {

    @Mock
    private SharedActivityService sharedActivityService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SharedActivityController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private SharedActivityResponse sharedActivityResponse;
    private SharedActivityDecisionRequest decisionRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();

        sharedActivityResponse = new SharedActivityResponse();
        sharedActivityResponse.setId(1L);
        sharedActivityResponse.setActivityId(100L);
        sharedActivityResponse.setReceiverId(200L);

        decisionRequest = new SharedActivityDecisionRequest();
        decisionRequest.setAction(com.tourism.platform.model.SharedActivityDecisionAction.ACCEPT);

        when(authentication.getName()).thenReturn("testuser");
    }

    @Test
    void shareActivity_ShouldReturnCreatedResponse() throws Exception {
        Long activityId = 100L;
        ShareActivityRequest body = new ShareActivityRequest();
        body.setReceiverId(200L);

        lenient().when(sharedActivityService.shareActivity(eq(activityId), eq(200L), eq("testuser")))
                .thenReturn(sharedActivityResponse);

        try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("userId")).thenReturn("1");

            mockMvc.perform(post("/activities/{activityId}/share", activityId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .principal(authentication))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.message").value("Activity shared successfully"))
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        verify(sharedActivityService).shareActivity(activityId, 200L, "testuser");
    }

    @Test
    void updateSharedActivityStatus_ShouldReturnOkResponse() throws Exception {
        Long sharedActivityId = 1L;

        lenient().when(sharedActivityService.resolveSharedActivity(eq(sharedActivityId), any(SharedActivityDecisionRequest.class), eq("testuser")))
                .thenReturn(sharedActivityResponse);

        try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("userId")).thenReturn("1");

            mockMvc.perform(patch("/shared-activities/{id}", sharedActivityId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(decisionRequest))
                            .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Shared activity updated successfully"));
        }

        verify(sharedActivityService).resolveSharedActivity(eq(sharedActivityId), any(SharedActivityDecisionRequest.class), eq("testuser"));
    }

    @Test
    void updateSharedActivityStatus_ShouldHandleRejectAction() throws Exception {
        Long sharedActivityId = 1L;
        decisionRequest.setAction(com.tourism.platform.model.SharedActivityDecisionAction.REJECT);

        lenient().when(sharedActivityService.resolveSharedActivity(eq(sharedActivityId), any(SharedActivityDecisionRequest.class), eq("testuser")))
                .thenReturn(sharedActivityResponse);

        try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("userId")).thenReturn("1");

            mockMvc.perform(patch("/shared-activities/{id}", sharedActivityId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(decisionRequest))
                            .principal(authentication))
                    .andExpect(status().isOk());
        }

        verify(sharedActivityService).resolveSharedActivity(eq(sharedActivityId), any(SharedActivityDecisionRequest.class), eq("testuser"));
    }

    @Test
    void shareActivity_WithEmptyRequestUri_UsesSafePath() throws Exception {
        Long activityId = 100L;
        ShareActivityRequest body = new ShareActivityRequest();
        body.setReceiverId(200L);

        lenient().when(sharedActivityService.shareActivity(eq(activityId), eq(200L), eq("testuser")))
                .thenReturn(sharedActivityResponse);

        try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("userId")).thenReturn("1");

            mockMvc.perform(post("/activities/{activityId}/share", activityId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .principal(authentication))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void shareActivity_WithNullHttpServletRequest_UsesSafePath() {
        Long activityId = 100L;
        ShareActivityRequest body = new ShareActivityRequest();
        body.setReceiverId(200L);

        when(sharedActivityService.shareActivity(eq(activityId), eq(200L), eq("testuser")))
                .thenReturn(sharedActivityResponse);

        try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("userId")).thenReturn("1");

            ResponseEntity<ApiResponse<SharedActivityResponse>> response =
                    controller.shareActivity(activityId, body, authentication, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPath()).isEmpty(); // safePath returns "" for null
        }
    }

    @Test
    void updateSharedActivityStatus_WithNullHttpServletRequest_UsesSafePath() {
        Long sharedActivityId = 1L;

        when(sharedActivityService.resolveSharedActivity(eq(sharedActivityId), any(SharedActivityDecisionRequest.class), eq("testuser")))
                .thenReturn(sharedActivityResponse);

        try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("userId")).thenReturn("1");

            ResponseEntity<ApiResponse<SharedActivityResponse>> response =
                    controller.updateSharedActivityStatus(sharedActivityId, decisionRequest, authentication, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getPath()).isEmpty(); // safePath returns "" for null
        }
    }
}
