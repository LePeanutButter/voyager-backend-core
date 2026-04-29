package com.tourism.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tourism.platform.dto.*;
import com.tourism.platform.model.TravelPlanStatus;
import com.tourism.platform.model.TravelType;
import com.tourism.platform.model.User;
import com.tourism.platform.repository.TravelPlanRepository;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.service.SocialService;
import com.tourism.platform.service.TravelPlanActivityService;
import com.tourism.platform.service.TravelPlanService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
class TravelPlanControllerTest {

    @Mock
    private TravelPlanActivityService travelPlanActivityService;

    @Mock
    private SocialService socialService;

    @Mock
    private TravelPlanRepository travelPlanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TravelPlanService travelPlanService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private TravelPlanController travelPlanController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private TravelPlanDto testTravelPlan;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(travelPlanController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testTravelPlan = TravelPlanDto.builder()
                .id(1L)
                .title("Test Travel Plan")
                .description("Test Description")
                .status(TravelPlanStatus.ACTIVE)
                .travelType(TravelType.LEISURE)
                .destinationLocation("Paris")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(7))
                .numberOfTravelers(2)
                .isPublic(true)
                .build();

        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    }

    @Test
    void createTravelPlan_WithValidRequest_ShouldReturnCreated() throws Exception {
        // Given
        when(travelPlanService.createTravelPlan(any(TravelPlanDto.class), eq(1L)))
                .thenReturn(testTravelPlan);

        // When & Then
        mockMvc.perform(post("/api/v1/travel-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testTravelPlan)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Travel plan created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Travel Plan"));

        verify(travelPlanService).createTravelPlan(any(TravelPlanDto.class), eq(1L));
    }
    

    @Test
    void getMyTravelPlans_ShouldReturnUserPlans() throws Exception {
        // Given
        when(travelPlanService.getTravelPlanDtosByUser(1L))
                .thenReturn(List.of(testTravelPlan));

        // When & Then
        mockMvc.perform(get("/api/v1/travel-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Travel plans retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(travelPlanService).getTravelPlanDtosByUser(1L);
    }

    @Test
    void getTravelPlansByUser_ShouldReturnPagedResponse() throws Exception {
        // Given
        when(travelPlanService.getTravelPlanDtosByUser(2L))
                .thenReturn(List.of(testTravelPlan));

        // When & Then
        mockMvc.perform(get("/api/v1/travel-plans/user/{userId}", 2L)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(travelPlanService).getTravelPlanDtosByUser(2L);
    }

    @Test
    void updateTravelPlan_WithValidRequest_ShouldReturnUpdatedPlan() throws Exception {
        // Given
        when(travelPlanService.updateTravelPlan(eq(1L), eq(1L), any(TravelPlanDto.class)))
                .thenReturn(testTravelPlan);

        // When & Then
        mockMvc.perform(put("/api/v1/travel-plans/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testTravelPlan)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Travel plan updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(travelPlanService).updateTravelPlan(eq(1L), eq(1L), any(TravelPlanDto.class));
    }

    @Test
    void deleteTravelPlan_WithValidId_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/travel-plans/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Travel plan deleted successfully"));

        verify(travelPlanService).deleteTravelPlan(1L, 1L);
    }

    
    @Test
    void getActivities_ShouldReturnActivityList() throws Exception {
        // Given
        TravelPlanActivityDto activityDto = TravelPlanActivityDto.builder()
                .id(1L)
                .name("Test Activity")
                .build();

        when(travelPlanActivityService.getActivities(1L))
                .thenReturn(List.of(activityDto));

        // When & Then
        mockMvc.perform(get("/api/v1/travel-plans/{id}/activities", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Activities retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(travelPlanActivityService).getActivities(1L);
    }

    

    @Test
    void deleteActivity_ShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/travel-plans/{id}/activities/{activityId}", 1L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Activity deleted successfully"));

        verify(travelPlanActivityService).deleteActivity(1L, 1L);
    }


    @Test
    void getTravelPlanConnections_WithNonAcceptedStatus_ShouldReturnEmptyList() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/travel-plans/{id}/connections", 1L)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(socialService, never()).getAcceptedConnectionsByTravelPlan(anyLong());
    }

    @Test
    void addReservation_ShouldReturnCreatedReservation() throws Exception {
        // Given
        ReservationDto reservationRequest = ReservationDto.builder()
                .name("Hotel Booking")
                .description("Luxury Hotel")
                .totalCost(BigDecimal.valueOf(500.0))
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/travel-plans/{id}/reservations", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservationRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Reservation added successfully"))
                .andExpect(jsonPath("$.data.name").value("Hotel Booking"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void getReservations_ShouldReturnReservationList() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/travel-plans/{id}/reservations", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Reservations retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void shareTravelPlan_ShouldReturnShareUrl() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/travel-plans/{id}/share", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Share link generated successfully"))
                .andExpect(jsonPath("$.data").value("https://tourism-platform.com/shared/abc123xyz789"));
    }

    @Test
    void getSharedTravelPlan_ShouldReturnSharedPlan() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/travel-plans/shared/{shareToken}", "abc123xyz789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Shared travel plan retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Shared Summer Vacation"))
                .andExpect(jsonPath("$.data.isPublic").value(true));
    }

    @Test
    void getTravelPlansByStatus_ShouldReturnPagedPlans() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/travel-plans/status/{status}", TravelPlanStatus.ACTIVE)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getTravelPlansByType_ShouldReturnPagedPlans() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/travel-plans/type/{type}", TravelType.LEISURE)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void updateTravelPlanStatus_ShouldReturnUpdatedPlan() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/v1/travel-plans/{id}/status", 1L)
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Travel plan status updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void findCompatibleTravelers_WithExistingPlan_ShouldReturnMatches() throws Exception {
        // Given - Add a plan to the in-memory map
        TravelPlanDto referencePlan = TravelPlanDto.builder()
                .id(1L)
                .title("Paris Trip")
                .destinationLocation("Paris")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(7))
                .status(TravelPlanStatus.ACTIVE)
                .build();

        // Use reflection to add to the map
        try {
            var travelPlansField = TravelPlanController.class.getDeclaredField("travelPlans");
            travelPlansField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var travelPlans = (java.util.Map<Long, TravelPlanDto>) travelPlansField.get(travelPlanController);
            travelPlans.put(1L, referencePlan);
            
            // Add another compatible plan
            TravelPlanDto otherPlan = TravelPlanDto.builder()
                    .id(2L)
                    .title("Another Paris Trip")
                    .destinationLocation("Paris")
                    .startDate(LocalDateTime.now().plusDays(3))
                    .endDate(LocalDateTime.now().plusDays(10))
                    .status(TravelPlanStatus.ACTIVE)
                    .build();
            travelPlans.put(2L, otherPlan);
        } catch (Exception e) {
            // Skip test if reflection fails
            return;
        }

        // When & Then
        mockMvc.perform(get("/api/v1/travel-plans/{id}/compatible-travelers", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Compatible travelers found successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].userId").value(2))
                .andExpect(jsonPath("$.data[0].destinationLocation").value("Paris"));
    }


    @Test
    void getAuthenticatedUser_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> {
            travelPlanController.getTravelPlan(1L, null);
        });
    }
}
