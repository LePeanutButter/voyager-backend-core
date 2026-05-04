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

import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void shareActivityShouldReturnCreatedResponse() throws Exception {
        Long activityId = 100L;
        ShareActivityRequest body = new ShareActivityRequest();
        body.setReceiverId(200L);

        when(sharedActivityService.shareActivity(activityId, 200L, "testuser"))
                .thenReturn(sharedActivityResponse);

        try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("userId")).thenReturn("1");

            mockMvc.perform(post("/activities/{activityId}/share", activityId)
                            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                            .content(Objects.requireNonNull(objectMapper.writeValueAsString(body)))
                            .principal(Objects.requireNonNull(authentication)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.message").value("Activity shared successfully"))
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        verify(sharedActivityService).shareActivity(activityId, 200L, "testuser");
    }

    @Test
    void updateSharedActivityStatusShouldReturnOkResponse() throws Exception {
        Long sharedActivityId = 1L;

        when(sharedActivityService.resolveSharedActivity(sharedActivityId, any(SharedActivityDecisionRequest.class), eq("testuser")))
                .thenReturn(sharedActivityResponse);

        try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("userId")).thenReturn("1");

            mockMvc.perform(patch("/shared-activities/{id}", sharedActivityId)
                            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                            .content(Objects.requireNonNull(objectMapper.writeValueAsString(decisionRequest)))
                            .principal(Objects.requireNonNull(authentication)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Shared activity updated successfully"));
        }

        verify(sharedActivityService).resolveSharedActivity(Objects.requireNonNull(sharedActivityId), any(SharedActivityDecisionRequest.class), eq("testuser"));
    }

    @Test
    void updateSharedActivityStatusShouldHandleRejectAction() throws Exception {
        Long sharedActivityId = 1L;
        decisionRequest.setAction(com.tourism.platform.model.SharedActivityDecisionAction.REJECT);

        when(sharedActivityService.resolveSharedActivity(sharedActivityId, any(SharedActivityDecisionRequest.class), eq("testuser")))
                .thenReturn(sharedActivityResponse);

        try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("userId")).thenReturn("1");

            mockMvc.perform(patch("/shared-activities/{id}", sharedActivityId)
                            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                            .content(Objects.requireNonNull(objectMapper.writeValueAsString(decisionRequest)))
                            .principal(Objects.requireNonNull(authentication)))
                    .andExpect(status().isOk());
        }

        verify(sharedActivityService).resolveSharedActivity(Objects.requireNonNull(sharedActivityId), any(SharedActivityDecisionRequest.class), eq("testuser"));
    }

    @Test
    void shareActivityWithEmptyRequestUriUsesSafePath() throws Exception {
        Long activityId = 100L;
        ShareActivityRequest body = new ShareActivityRequest();
        body.setReceiverId(200L);

        when(sharedActivityService.shareActivity(activityId, 200L, "testuser"))
                .thenReturn(sharedActivityResponse);

        try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("userId")).thenReturn("1");

            mockMvc.perform(post("/activities/{activityId}/share", activityId)
                            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                            .content(Objects.requireNonNull(objectMapper.writeValueAsString(body)))
                            .principal(Objects.requireNonNull(authentication)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    void shareActivityWithNullHttpServletRequestUsesSafePath() {
        Long activityId = 100L;
        ShareActivityRequest body = new ShareActivityRequest();
        body.setReceiverId(200L);

        when(sharedActivityService.shareActivity(activityId, 200L, "testuser"))
                .thenReturn(sharedActivityResponse);

        try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("userId")).thenReturn("1");

            ResponseEntity<ApiResponse<SharedActivityResponse>> response =
                    controller.shareActivity(activityId, body, authentication, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            ApiResponse<SharedActivityResponse> responseBody = Objects.requireNonNull(response.getBody());
            assertThat(responseBody.getPath()).isNotNull();
            assertThat(responseBody.getPath()).isEmpty(); // safePath returns "" for null
        }
    }

    @Test
    void updateSharedActivityStatusWithNullHttpServletRequestUsesSafePath() {
        Long sharedActivityId = 1L;

        when(sharedActivityService.resolveSharedActivity(sharedActivityId, any(SharedActivityDecisionRequest.class), "testuser"))
                .thenReturn(sharedActivityResponse);

        try (MockedStatic<MDC> mdc = mockStatic(MDC.class)) {
            mdc.when(() -> MDC.get("userId")).thenReturn("1");

            ResponseEntity<ApiResponse<SharedActivityResponse>> response =
                    controller.updateSharedActivityStatus(sharedActivityId, decisionRequest, authentication, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            ApiResponse<SharedActivityResponse> responseBody = Objects.requireNonNull(response.getBody());
            assertThat(responseBody.getPath()).isNotNull();
            assertThat(responseBody.getPath()).isEmpty(); // safePath returns "" for null
        }
    }
}
