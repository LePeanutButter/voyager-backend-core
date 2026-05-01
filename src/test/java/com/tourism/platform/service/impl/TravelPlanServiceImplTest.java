package com.tourism.platform.service.impl;

import com.tourism.platform.dto.TravelPlanDto;
import com.tourism.platform.dto.TravelerMatchDto;
import com.tourism.platform.exception.BusinessException;
import com.tourism.platform.exception.ResourceNotFoundException;
import com.tourism.platform.model.TravelPlan;
import com.tourism.platform.model.TravelPlanStatus;
import com.tourism.platform.model.TravelType;
import com.tourism.platform.model.User;
import com.tourism.platform.repository.TravelPlanRepository;
import com.tourism.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TravelPlanServiceImplTest {

    @Mock
    private TravelPlanRepository travelPlanRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TravelPlanServiceImpl travelPlanService;

    private User testUser;
    private TravelPlan testTravelPlan;
    private TravelPlanDto testTravelPlanDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        testUser.setBio("Test bio");

        testTravelPlan = new TravelPlan();
        testTravelPlan.setId(1L);
        testTravelPlan.setUser(testUser);
        testTravelPlan.setTitle("Test Travel Plan");
        testTravelPlan.setDescription("Test Description");
        testTravelPlan.setDestinationLocation("Paris");
        testTravelPlan.setOriginLocation("New York");
        testTravelPlan.setStartDate(LocalDateTime.now().plusDays(1));
        testTravelPlan.setEndDate(LocalDateTime.now().plusDays(7));
        testTravelPlan.setEstimatedBudget(BigDecimal.valueOf(5000.0));
        testTravelPlan.setActualCost(BigDecimal.valueOf(4500.0));
        testTravelPlan.setNumberOfTravelers(2);
        testTravelPlan.setStatus(TravelPlanStatus.ACTIVE);
        testTravelPlan.setTravelType(TravelType.LEISURE);
        testTravelPlan.setIsPublic(true);

        testTravelPlanDto = TravelPlanDto.builder()
                .id(1L)
                .title("Test Travel Plan")
                .description("Test Description")
                .destinationLocation("Paris")
                .originLocation("New York")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(7))
                .estimatedBudget(BigDecimal.valueOf(5000.0))
                .actualCost(BigDecimal.valueOf(4500.0))
                .numberOfTravelers(2)
                .status(TravelPlanStatus.ACTIVE)
                .travelType(TravelType.LEISURE)
                .isPublic(true)
                .build();
    }

    @Test
    void findCompatibleTravelers_WithValidPlan_ShouldReturnMatches() {
        // Given
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");
        otherUser.setFirstName("Other");
        otherUser.setLastName("User");

        TravelPlan compatiblePlan = new TravelPlan();
        compatiblePlan.setId(2L);
        compatiblePlan.setUser(otherUser);
        compatiblePlan.setTitle("Compatible Plan");
        compatiblePlan.setDestinationLocation("Paris");
        compatiblePlan.setStartDate(LocalDateTime.now().plusDays(3));
        compatiblePlan.setEndDate(LocalDateTime.now().plusDays(10));
        compatiblePlan.setNumberOfTravelers(3);

        when(travelPlanRepository.findById(1L)).thenReturn(Optional.of(testTravelPlan));
        when(travelPlanRepository.findCompatibleTravelPlans(anyString(), any(), any(), eq(1L), eq(TravelPlanStatus.ACTIVE)))
                .thenReturn(List.of(compatiblePlan));

        // When
        List<TravelerMatchDto> result = travelPlanService.findCompatibleTravelers(1L, 1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        TravelerMatchDto match = result.get(0);
        assertEquals(2L, match.getUserId());
        assertEquals("otheruser", match.getUsername());
        assertEquals("Other", match.getFirstName());
        assertEquals("User", match.getLastName());
        assertEquals(2L, match.getTravelPlanId());
        assertEquals("Compatible Plan", match.getTravelPlanTitle());
        assertEquals("Paris", match.getDestinationLocation());
        assertTrue(match.getDaysOverlap() > 0);
        assertTrue(match.getCompatibilityScore() > 0.0);

        verify(travelPlanRepository).findById(1L);
        verify(travelPlanRepository).findCompatibleTravelPlans(anyString(), any(), any(), eq(1L), eq(TravelPlanStatus.ACTIVE));
    }

    @Test
    void findCompatibleTravelers_WithNonExistentPlan_ShouldThrowException() {
        // Given
        when(travelPlanRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> travelPlanService.findCompatibleTravelers(999L, 1L));

        verify(travelPlanRepository).findById(999L);
        verify(travelPlanRepository, never()).findCompatibleTravelPlans(anyString(), any(), any(), anyLong(), any());
    }

    @Test
    void findCompatibleTravelers_WithUnauthorizedUser_ShouldThrowException() {
        // Given
        when(travelPlanRepository.findById(1L)).thenReturn(Optional.of(testTravelPlan));

        // When & Then
        assertThrows(AccessDeniedException.class,
                () -> travelPlanService.findCompatibleTravelers(1L, 2L));

        verify(travelPlanRepository).findById(1L);
        verify(travelPlanRepository, never()).findCompatibleTravelPlans(anyString(), any(), any(), anyLong(), any());
    }

    @Test
    void calculateOverlappingDays_WithOverlap_ShouldReturnCorrectDays() {
        // Given
        LocalDateTime start1 = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end1 = LocalDateTime.of(2024, 1, 7, 0, 0);
        LocalDateTime start2 = LocalDateTime.of(2024, 1, 3, 0, 0);
        LocalDateTime end2 = LocalDateTime.of(2024, 1, 10, 0, 0);

        // When
        Integer result = travelPlanService.calculateOverlappingDays(start1, end1, start2, end2);

        // Then
        assertEquals(5, result); // Jan 3-7 inclusive
    }

    @Test
    void calculateOverlappingDays_WithNoOverlap_ShouldReturnZero() {
        // Given
        LocalDateTime start1 = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime end1 = LocalDateTime.of(2024, 1, 7, 0, 0);
        LocalDateTime start2 = LocalDateTime.of(2024, 1, 8, 0, 0);
        LocalDateTime end2 = LocalDateTime.of(2024, 1, 15, 0, 0);

        // When
        Integer result = travelPlanService.calculateOverlappingDays(start1, end1, start2, end2);

        // Then
        assertEquals(0, result);
    }

    @Test
    void calculateOverlappingDays_WithPartialDayOverlap_ShouldReturnOne() {
        // Given
        LocalDateTime start1 = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime end1 = LocalDateTime.of(2024, 1, 1, 14, 0);
        LocalDateTime start2 = LocalDateTime.of(2024, 1, 1, 12, 0);
        LocalDateTime end2 = LocalDateTime.of(2024, 1, 1, 16, 0);

        // When
        Integer result = travelPlanService.calculateOverlappingDays(start1, end1, start2, end2);

        // Then
        assertEquals(1, result); // Partial day counts as 1
    }

    @Test
    void getTravelPlanById_WithValidId_ShouldReturnPlan() {
        // Given
        when(travelPlanRepository.findById(1L)).thenReturn(Optional.of(testTravelPlan));

        // When
        TravelPlan result = travelPlanService.getTravelPlanById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Travel Plan", result.getTitle());
        verify(travelPlanRepository).findById(1L);
    }

    @Test
    void getTravelPlanById_WithNonExistentId_ShouldThrowException() {
        // Given
        when(travelPlanRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> travelPlanService.getTravelPlanById(999L));

        verify(travelPlanRepository).findById(999L);
    }

    @Test
    void getTravelPlansByUser_ShouldReturnPage() {
        // Given
        Pageable pageable = mock(Pageable.class);
        Page<TravelPlan> page = new PageImpl<>(List.of(testTravelPlan));
        when(travelPlanRepository.findByUserId(1L, pageable)).thenReturn(page);

        // When
        Page<TravelPlan> result = travelPlanService.getTravelPlansByUser(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getId());
        verify(travelPlanRepository).findByUserId(1L, pageable);
    }

    @Test
    void getActiveTravelPlansByUser_ShouldReturnPage() {
        // Given
        Pageable pageable = mock(Pageable.class);
        Page<TravelPlan> page = new PageImpl<>(List.of(testTravelPlan));
        when(travelPlanRepository.findByUserIdAndStatus(1L, TravelPlanStatus.ACTIVE, pageable))
                .thenReturn(page);

        // When
        Page<TravelPlan> result = travelPlanService.getActiveTravelPlansByUser(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getContent().get(0).getId());
        verify(travelPlanRepository).findByUserIdAndStatus(1L, TravelPlanStatus.ACTIVE, pageable);
    }

    @Test
    void createTravelPlan_WithValidData_ShouldReturnCreatedPlan() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(travelPlanRepository.save(any(TravelPlan.class))).thenReturn(testTravelPlan);

        // When
        TravelPlanDto result = travelPlanService.createTravelPlan(testTravelPlanDto, 1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Travel Plan", result.getTitle());
        assertEquals("Paris", result.getDestinationLocation());
        verify(userRepository).findById(1L);
        verify(travelPlanRepository).save(any(TravelPlan.class));
    }

    @Test
    void createTravelPlan_WithInvalidDateRange_ShouldThrowException() {
        // Given
        TravelPlanDto invalidDto = TravelPlanDto.builder()
                .title("Invalid Plan")
                .startDate(LocalDateTime.now().plusDays(7))
                .endDate(LocalDateTime.now().plusDays(1)) // End before start
                .build();

        // When & Then
        assertThrows(BusinessException.class,
                () -> travelPlanService.createTravelPlan(invalidDto, 1L));

        verify(userRepository, never()).findById(anyLong());
        verify(travelPlanRepository, never()).save(any(TravelPlan.class));
    }

    @Test
    void createTravelPlan_WithNonExistentUser_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> travelPlanService.createTravelPlan(testTravelPlanDto, 999L));

        verify(userRepository).findById(999L);
        verify(travelPlanRepository, never()).save(any(TravelPlan.class));
    }

    @Test
    void getTravelPlanDtosByUser_ShouldReturnDtoList() {
        // Given
        when(travelPlanRepository.findByUserId(1L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(testTravelPlan)));

        // When
        List<TravelPlanDto> result = travelPlanService.getTravelPlanDtosByUser(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Test Travel Plan", result.get(0).getTitle());
        verify(travelPlanRepository).findByUserId(1L, Pageable.unpaged());
    }

    @Test
    void updateTravelPlan_WithValidData_ShouldReturnUpdatedPlan() {
        // Given
        TravelPlanDto updateDto = TravelPlanDto.builder()
                .title("Updated Plan")
                .description("Updated Description")
                .destinationLocation("London")
                .estimatedBudget(BigDecimal.valueOf(6000.0))
                .build();

        when(travelPlanRepository.findById(1L)).thenReturn(Optional.of(testTravelPlan));
        when(travelPlanRepository.save(any(TravelPlan.class))).thenReturn(testTravelPlan);

        // When
        TravelPlanDto result = travelPlanService.updateTravelPlan(1L, 1L, updateDto);

        // Then
        assertNotNull(result);
        verify(travelPlanRepository).findById(1L);
        verify(travelPlanRepository).save(any(TravelPlan.class));
    }

    @Test
    void updateTravelPlan_WithInvalidDateRange_ShouldThrowException() {
        // Given
        TravelPlanDto invalidDto = TravelPlanDto.builder()
                .startDate(LocalDateTime.now().plusDays(7))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        // When & Then
        assertThrows(BusinessException.class,
                () -> travelPlanService.updateTravelPlan(1L, 1L, invalidDto));

        verify(travelPlanRepository, never()).findById(anyLong());
        verify(travelPlanRepository, never()).save(any(TravelPlan.class));
    }

    @Test
    void updateTravelPlan_WithNonExistentPlan_ShouldThrowException() {
        // Given
        when(travelPlanRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> travelPlanService.updateTravelPlan(999L, 1L, testTravelPlanDto));

        verify(travelPlanRepository).findById(999L);
        verify(travelPlanRepository, never()).save(any(TravelPlan.class));
    }

    @Test
    void updateTravelPlan_WithUnauthorizedUser_ShouldThrowException() {
        // Given
        when(travelPlanRepository.findById(1L)).thenReturn(Optional.of(testTravelPlan));

        // When & Then
        assertThrows(AccessDeniedException.class,
                () -> travelPlanService.updateTravelPlan(1L, 2L, testTravelPlanDto));

        verify(travelPlanRepository).findById(1L);
        verify(travelPlanRepository, never()).save(any(TravelPlan.class));
    }

    @Test
    void deleteTravelPlan_WithValidUser_ShouldDeletePlan() {
        // Given
        when(travelPlanRepository.findById(1L)).thenReturn(Optional.of(testTravelPlan));
        doNothing().when(travelPlanRepository).delete(testTravelPlan);

        // When
        travelPlanService.deleteTravelPlan(1L, 1L);

        // Then
        verify(travelPlanRepository).findById(1L);
        verify(travelPlanRepository).delete(testTravelPlan);
    }

    @Test
    void deleteTravelPlan_WithNonExistentPlan_ShouldThrowException() {
        // Given
        when(travelPlanRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class,
                () -> travelPlanService.deleteTravelPlan(999L, 1L));

        verify(travelPlanRepository).findById(999L);
        verify(travelPlanRepository, never()).delete(any(TravelPlan.class));
    }

    @Test
    void deleteTravelPlan_WithUnauthorizedUser_ShouldThrowException() {
        // Given
        when(travelPlanRepository.findById(1L)).thenReturn(Optional.of(testTravelPlan));

        // When & Then
        assertThrows(AccessDeniedException.class,
                () -> travelPlanService.deleteTravelPlan(1L, 2L));

        verify(travelPlanRepository).findById(1L);
        verify(travelPlanRepository, never()).delete(any(TravelPlan.class));
    }

    @Test
    void createTravelPlan_WithNullDates_ShouldCreatePlan() {
        // Given
        TravelPlanDto planWithoutDates = TravelPlanDto.builder()
                .title("Plan Without Dates")
                .description("Description")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(travelPlanRepository.save(any(TravelPlan.class))).thenReturn(testTravelPlan);

        // When
        TravelPlanDto result = travelPlanService.createTravelPlan(planWithoutDates, 1L);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(travelPlanRepository).save(any(TravelPlan.class));
    }

    @Test
    void updateTravelPlan_WithNullDates_ShouldUpdatePlan() {
        // Given
        TravelPlanDto updateWithoutDates = TravelPlanDto.builder()
                .title("Updated Title")
                .build();

        when(travelPlanRepository.findById(1L)).thenReturn(Optional.of(testTravelPlan));
        when(travelPlanRepository.save(any(TravelPlan.class))).thenReturn(testTravelPlan);

        // When
        TravelPlanDto result = travelPlanService.updateTravelPlan(1L, 1L, updateWithoutDates);

        // Then
        assertNotNull(result);
        verify(travelPlanRepository).findById(1L);
        verify(travelPlanRepository).save(any(TravelPlan.class));
    }
}
