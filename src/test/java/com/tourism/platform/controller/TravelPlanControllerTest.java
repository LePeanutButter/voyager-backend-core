package com.tourism.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tourism.platform.dto.ReservationDto;
import com.tourism.platform.dto.TravelConnectionDto;
import com.tourism.platform.dto.TravelPlanActivityDto;
import com.tourism.platform.dto.TravelPlanDto;
import com.tourism.platform.dto.TravelerMatchDto;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void createTravelPlanWithValidRequestShouldReturnCreated() throws Exception {
        // Given
        when(travelPlanService.createTravelPlan(any(TravelPlanDto.class), eq(1L)))
                .thenReturn(testTravelPlan);

        // When & Then
        mockMvc.perform(post("/travel-plans")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(testTravelPlan))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Travel plan created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Travel Plan"));

        verify(travelPlanService).createTravelPlan(any(TravelPlanDto.class), eq(1L));
    }
    

    @Test
    void getTravelPlanByIdShouldReturnPlanWhenPresent() throws Exception {
        when(travelPlanService.getTravelPlanDtosByUser(1L)).thenReturn(List.of(testTravelPlan));

        mockMvc.perform(get("/travel-plans/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Travel plan retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(travelPlanService).getTravelPlanDtosByUser(1L);
    }

    @Test
    void getTravelPlanWhenIdNotInUserListShouldThrow() {
        when(travelPlanService.getTravelPlanDtosByUser(1L)).thenReturn(List.of(testTravelPlan));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/travel-plans/99");

        assertThrows(EntityNotFoundException.class,
                () -> travelPlanController.getTravelPlan(99L, request));
    }

    @Test
    void createTravelPlanShouldResolveUserWhenPrincipalIsUserInstance() throws Exception {
        when(authentication.getPrincipal()).thenReturn(testUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(travelPlanService.createTravelPlan(any(TravelPlanDto.class), eq(1L)))
                .thenReturn(testTravelPlan);

        mockMvc.perform(post("/travel-plans")
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(testTravelPlan))))
                .andExpect(status().isCreated());

        verify(userRepository).findById(1L);
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void getAuthenticatedUserShouldResolveByEmailWhenUsernameMissing() throws Exception {
        when(authentication.getPrincipal()).thenReturn("anonymous");
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByUsername("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(travelPlanService.getTravelPlanDtosByUser(1L)).thenReturn(List.of(testTravelPlan));

        mockMvc.perform(get("/travel-plans"))
                .andExpect(status().isOk());

        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void getMyTravelPlansShouldReturnUserPlans() throws Exception {
        // Given
        when(travelPlanService.getTravelPlanDtosByUser(1L))
                .thenReturn(List.of(testTravelPlan));

        // When & Then
        mockMvc.perform(get("/travel-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Travel plans retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(travelPlanService).getTravelPlanDtosByUser(1L);
    }

    @Test
    void getTravelPlansByUserShouldReturnPagedResponse() throws Exception {
        // Given
        when(travelPlanService.getTravelPlanDtosByUser(2L))
                .thenReturn(List.of(testTravelPlan));

        // When & Then
        mockMvc.perform(get("/travel-plans/user/{userId}", 2L)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(travelPlanService).getTravelPlanDtosByUser(2L);
    }

    @Test
    void updateTravelPlanWithValidRequestShouldReturnUpdatedPlan() throws Exception {
        // Given
        when(travelPlanService.updateTravelPlan(eq(1L), eq(1L), any(TravelPlanDto.class)))
                .thenReturn(testTravelPlan);

        // When & Then
        mockMvc.perform(put("/travel-plans/{id}", 1L)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(testTravelPlan))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Travel plan updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(travelPlanService).updateTravelPlan(eq(1L), eq(1L), any(TravelPlanDto.class));
    }

    @Test
    void deleteTravelPlanWithValidIdShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(delete("/travel-plans/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Travel plan deleted successfully"));

        verify(travelPlanService).deleteTravelPlan(1L, 1L);
    }

    
    @Test
    void getActivitiesShouldReturnActivityList() throws Exception {
        // Given
        TravelPlanActivityDto activityDto = TravelPlanActivityDto.builder()
                .id(1L)
                .name("Test Activity")
                .build();

        when(travelPlanActivityService.getActivities(1L))
                .thenReturn(List.of(activityDto));

        // When & Then
        mockMvc.perform(get("/travel-plans/{id}/activities", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Activities retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1));

        verify(travelPlanActivityService).getActivities(1L);
    }

    

    @Test
    void deleteActivityShouldReturnSuccess() throws Exception {
        // When & Then
        mockMvc.perform(delete("/travel-plans/{id}/activities/{activityId}", 1L, 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Activity deleted successfully"));

        verify(travelPlanActivityService).deleteActivity(1L, 1L);
    }


    @Test
    void getTravelPlanConnectionsWithNonAcceptedStatusShouldReturnEmptyList() throws Exception {
        // When & Then
        mockMvc.perform(get("/travel-plans/{id}/connections", 1L)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("No accepted connections found for this travel plan"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(socialService, never()).getAcceptedConnectionsByTravelPlan(anyLong());
    }

    @Test
    void getTravelPlanConnectionsWithAcceptedStatusShouldCallSocialService() throws Exception {
        TravelConnectionDto connection = TravelConnectionDto.builder()
                .id(10L)
                .userId(2L)
                .username("peer")
                .build();
        when(socialService.getAcceptedConnectionsByTravelPlan(1L)).thenReturn(List.of(connection));

        mockMvc.perform(get("/travel-plans/{id}/connections", 1L)
                        .param("status", "accepted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Connections retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].username").value("peer"));

        verify(socialService).getAcceptedConnectionsByTravelPlan(1L);
    }

    @Test
    void getTravelPlanConnectionsWithAcceptedStatusAndNoMatchesShouldReturnNoConnectionsMessage() throws Exception {
        when(socialService.getAcceptedConnectionsByTravelPlan(1L)).thenReturn(List.of());

        mockMvc.perform(get("/travel-plans/{id}/connections", 1L)
                        .param("status", "ACCEPTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No accepted connections found for this travel plan"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(socialService).getAcceptedConnectionsByTravelPlan(1L);
    }

    @Test
    void addReservationShouldReturnCreatedReservation() throws Exception {
        // Given
        ReservationDto reservationRequest = ReservationDto.builder()
                .name("Hotel Booking")
                .description("Luxury Hotel")
                .totalCost(BigDecimal.valueOf(500.0))
                .build();

        // When & Then
        mockMvc.perform(post("/travel-plans/{id}/reservations", 1L)
                        .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                        .content(Objects.requireNonNull(objectMapper.writeValueAsString(reservationRequest))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Reservation added successfully"))
                .andExpect(jsonPath("$.data.name").value("Hotel Booking"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void getReservationsShouldReturnReservationList() throws Exception {
        // When & Then
        mockMvc.perform(get("/travel-plans/{id}/reservations", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Reservations retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void shareTravelPlanShouldReturnShareUrl() throws Exception {
        // When & Then
        mockMvc.perform(post("/travel-plans/{id}/share", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Share link generated successfully"))
                .andExpect(jsonPath("$.data").value("https://tourism-platform.com/shared/abc123xyz789"));
    }

    @Test
    void getSharedTravelPlanShouldReturnSharedPlan() throws Exception {
        // When & Then
        mockMvc.perform(get("/travel-plans/shared/{shareToken}", "abc123xyz789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Shared travel plan retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Shared Summer Vacation"))
                .andExpect(jsonPath("$.data.isPublic").value(true));
    }

    @Test
    void getTravelPlansByStatusShouldReturnPagedPlans() throws Exception {
        // When & Then
        mockMvc.perform(get("/travel-plans/status/{status}", TravelPlanStatus.ACTIVE)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getTravelPlansByTypeShouldReturnPagedPlans() throws Exception {
        // When & Then
        mockMvc.perform(get("/travel-plans/type/{type}", TravelType.LEISURE)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void updateTravelPlanStatusShouldReturnUpdatedPlan() throws Exception {
        TravelPlanDto completed = TravelPlanDto.builder()
                .id(1L)
                .title(testTravelPlan.getTitle())
                .description(testTravelPlan.getDescription())
                .status(TravelPlanStatus.COMPLETED)
                .travelType(testTravelPlan.getTravelType())
                .destinationLocation(testTravelPlan.getDestinationLocation())
                .startDate(testTravelPlan.getStartDate())
                .endDate(testTravelPlan.getEndDate())
                .estimatedBudget(testTravelPlan.getEstimatedBudget())
                .numberOfTravelers(testTravelPlan.getNumberOfTravelers())
                .isPublic(testTravelPlan.getIsPublic())
                .build();

        when(travelPlanService.updateTravelPlanStatus(1L, 1L, TravelPlanStatus.COMPLETED))
                .thenReturn(completed);

        mockMvc.perform(put("/travel-plans/{id}/status", 1L)
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Travel plan status updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.title").value(testTravelPlan.getTitle()));

        verify(travelPlanService).updateTravelPlanStatus(1L, 1L, TravelPlanStatus.COMPLETED);
    }

    @Test
    void findCompatibleTravelersWhenEmptyShouldReturnNoMatchesMessage() throws Exception {
        when(travelPlanService.findCompatibleTravelers(1L, 1L)).thenReturn(List.of());

        mockMvc.perform(get("/travel-plans/{id}/compatible-travelers", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No compatible travelers found for this travel plan"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());

        verify(travelPlanService).findCompatibleTravelers(1L, 1L);
    }

    @Test
    void findCompatibleTravelersWithExistingPlanShouldReturnMatches() throws Exception {
        TravelerMatchDto match = TravelerMatchDto.builder()
                .userId(2L)
                .username("otheruser")
                .firstName("Other")
                .lastName("User")
                .travelPlanId(2L)
                .travelPlanTitle("Another Paris Trip")
                .destinationLocation("Paris")
                .daysOverlap(4)
                .compatibilityScore(75.0)
                .build();

        when(travelPlanService.findCompatibleTravelers(1L, 1L)).thenReturn(List.of(match));

        mockMvc.perform(get("/travel-plans/{id}/compatible-travelers", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Compatible travelers found successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].userId").value(2))
                .andExpect(jsonPath("$.data[0].destinationLocation").value("Paris"));

        verify(travelPlanService).findCompatibleTravelers(1L, 1L);
    }


    @Test
    void getAuthenticatedUserWhenAuthenticationNullShouldThrow() {
        when(securityContext.getAuthentication()).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(EntityNotFoundException.class, () -> travelPlanController.getMyTravelPlans(request));
    }

    @Test
    void getAuthenticatedUserWithNonExistentUserShouldThrowException() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> travelPlanController.getTravelPlan(1L, new MockHttpServletRequest()));
    }
}
