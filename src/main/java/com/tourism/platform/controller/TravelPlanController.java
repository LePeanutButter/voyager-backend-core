package com.tourism.platform.controller;

import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.PagedResponse;
import com.tourism.platform.dto.TravelPlanDto;
import com.tourism.platform.dto.TravelPlanActivityDto;
import com.tourism.platform.dto.ReservationDto;
import com.tourism.platform.dto.TravelerMatchDto;
import com.tourism.platform.model.TravelPlanStatus;
import com.tourism.platform.model.TravelType;
import com.tourism.platform.model.User;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.service.TravelPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * REST Controller for Travel Plan Management
 * 
 * This controller follows REST best practices and Richardson Maturity Model Level 3:
 * - Proper HTTP methods (GET, POST, PUT, DELETE)
 * - Resource-based URIs
 * - Self-descriptive responses
 * - Standard HTTP status codes
 * - Content negotiation
 */
@RestController
@RequestMapping("/travel-plans")
@RequiredArgsConstructor
@Tag(name = "Travel Planning", description = "APIs for managing travel plans and itineraries")
public class TravelPlanController {

    private final TravelPlanService travelPlanService;
    private final UserRepository userRepository;
    
    // Simple in-memory storage for testing
    private static final Map<Long, TravelPlanDto> travelPlans = new ConcurrentHashMap<>();
    
    // Constants for error messages
    private static final String TRAVEL_PLAN_NOT_FOUND_MSG = "Travel plan not found with ID: ";

    @PostMapping
    @Operation(summary = "Create a new travel plan", description = "Creates a new travel plan with the provided details")
    public ResponseEntity<ApiResponse<TravelPlanDto>> createTravelPlan(
            @Valid @RequestBody TravelPlanDto travelPlanDto,
            HttpServletRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        TravelPlanDto createdPlan = travelPlanService.createTravelPlan(travelPlanDto, user.getId());
        
        ApiResponse<TravelPlanDto> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Travel plan created successfully",
                createdPlan,
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get travel plan by ID", description = "Retrieves a specific travel plan by its ID")
    public ResponseEntity<ApiResponse<TravelPlanDto>> getTravelPlan(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            HttpServletRequest request) {

        User user = getAuthenticatedUser();
        TravelPlanDto travelPlan = travelPlanService.getTravelPlanDtosByUser(user.getId()).stream()
                .filter(plan -> id.equals(plan.getId()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(TRAVEL_PLAN_NOT_FOUND_MSG + id));
        
        ApiResponse<TravelPlanDto> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Travel plan retrieved successfully",
                travelPlan,
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Get authenticated user travel plans", description = "Retrieves all travel plans for the authenticated user")
    public ResponseEntity<ApiResponse<List<TravelPlanDto>>> getMyTravelPlans(HttpServletRequest request) {
        User user = getAuthenticatedUser();
        List<TravelPlanDto> plans = travelPlanService.getTravelPlanDtosByUser(user.getId());

        ApiResponse<List<TravelPlanDto>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Travel plans retrieved successfully",
                plans,
                request.getRequestURI()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get travel plans by user", description = "Retrieves all travel plans for a specific user")
    public ResponseEntity<PagedResponse<TravelPlanDto>> getTravelPlansByUser(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        List<TravelPlanDto> plans = travelPlanService.getTravelPlanDtosByUser(userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TravelPlanDto> pageResult = new org.springframework.data.domain.PageImpl<>(
                plans, pageable, plans.size()
        );
        
        PagedResponse<TravelPlanDto> response = PagedResponse.fromPage(
                pageResult,
                "Travel plans retrieved successfully",
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update travel plan", description = "Updates an existing travel plan")
    public ResponseEntity<ApiResponse<TravelPlanDto>> updateTravelPlan(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            @Valid @RequestBody TravelPlanDto travelPlanDto,
            HttpServletRequest request) {

        User user = getAuthenticatedUser();
        TravelPlanDto updatedPlan = travelPlanService.updateTravelPlan(id, user.getId(), travelPlanDto);
        
        ApiResponse<TravelPlanDto> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Travel plan updated successfully",
                updatedPlan,
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete travel plan", description = "Deletes a travel plan")
    public ResponseEntity<ApiResponse<Void>> deleteTravelPlan(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            HttpServletRequest request) {

        User user = getAuthenticatedUser();
        travelPlanService.deleteTravelPlan(id, user.getId());

        ApiResponse<Void> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Travel plan deleted successfully",
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/activities")
    @Operation(summary = "Add activity to travel plan", description = "Adds a new activity to an existing travel plan")
    public ResponseEntity<ApiResponse<TravelPlanActivityDto>> addActivity(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            @Valid @RequestBody TravelPlanActivityDto activityDto,
            HttpServletRequest request) {
        
        // Placeholder implementation
        TravelPlanActivityDto createdActivity = TravelPlanActivityDto.builder()
                .id(1L)
                .name(activityDto.getName())
                .description(activityDto.getDescription())
                .type(activityDto.getType())
                .startTime(activityDto.getStartTime())
                .endTime(activityDto.getEndTime())
                .location(activityDto.getLocation())
                .estimatedCost(activityDto.getEstimatedCost())
                .isConfirmed(false)
                .build();
        
        ApiResponse<TravelPlanActivityDto> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Activity added successfully",
                createdActivity,
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/activities")
    @Operation(summary = "Get travel plan activities", description = "Retrieves all activities for a travel plan")
    public ResponseEntity<ApiResponse<List<TravelPlanActivityDto>>> getActivities(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            HttpServletRequest request) {
        
        // Placeholder implementation
        List<TravelPlanActivityDto> activities = List.of(
                TravelPlanActivityDto.builder().id(1L).name("City Tour").build(),
                TravelPlanActivityDto.builder().id(2L).name("Museum Visit").build(),
                TravelPlanActivityDto.builder().id(3L).name("Dinner at Restaurant").build()
        );
        
        ApiResponse<List<TravelPlanActivityDto>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Activities retrieved successfully",
                activities,
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reservations")
    @Operation(summary = "Add reservation to travel plan", description = "Adds a new reservation to an existing travel plan")
    public ResponseEntity<ApiResponse<ReservationDto>> addReservation(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            @Valid @RequestBody ReservationDto reservationDto,
            HttpServletRequest request) {
        
        // Placeholder implementation
        ReservationDto createdReservation = ReservationDto.builder()
                .id(1L)
                .name(reservationDto.getName())
                .description(reservationDto.getDescription())
                .type(reservationDto.getType())
                .status(com.tourism.platform.model.ReservationStatus.PENDING)
                .startDate(reservationDto.getStartDate())
                .endDate(reservationDto.getEndDate())
                .location(reservationDto.getLocation())
                .totalCost(reservationDto.getTotalCost())
                .isPaid(false)
                .build();
        
        ApiResponse<ReservationDto> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Reservation added successfully",
                createdReservation,
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/reservations")
    @Operation(summary = "Get travel plan reservations", description = "Retrieves all reservations for a travel plan")
    public ResponseEntity<ApiResponse<List<ReservationDto>>> getReservations(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            HttpServletRequest request) {
        
        // Placeholder implementation
        List<ReservationDto> reservations = List.of(
                ReservationDto.builder().id(1L).name("Hotel Booking").build(),
                ReservationDto.builder().id(2L).name("Flight Reservation").build(),
                ReservationDto.builder().id(3L).name("Car Rental").build()
        );
        
        ApiResponse<List<ReservationDto>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Reservations retrieved successfully",
                reservations,
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/share")
    @Operation(summary = "Share travel plan", description = "Generates a shareable link for the travel plan")
    public ResponseEntity<ApiResponse<String>> shareTravelPlan(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            HttpServletRequest request) {
        
        // Placeholder implementation
        String shareToken = "abc123xyz789";
        String shareUrl = "https://tourism-platform.com/shared/" + shareToken;
        
        ApiResponse<String> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Share link generated successfully",
                shareUrl,
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shared/{shareToken}")
    @Operation(summary = "Get shared travel plan", description = "Retrieves a travel plan using its share token")
    public ResponseEntity<ApiResponse<TravelPlanDto>> getSharedTravelPlan(
            @Parameter(description = "Share token") @PathVariable String shareToken,
            HttpServletRequest request) {
        
        // Placeholder implementation
        TravelPlanDto travelPlan = TravelPlanDto.builder()
                .id(1L)
                .title("Shared Summer Vacation")
                .description("A wonderful shared trip")
                .status(TravelPlanStatus.ACTIVE)
                .travelType(TravelType.LEISURE)
                .isPublic(true)
                .shareToken(shareToken)
                .build();
        
        ApiResponse<TravelPlanDto> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Shared travel plan retrieved successfully",
                travelPlan,
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get travel plans by status", description = "Retrieves travel plans filtered by status")
    public ResponseEntity<PagedResponse<TravelPlanDto>> getTravelPlansByStatus(
            @Parameter(description = "Travel plan status") @PathVariable TravelPlanStatus status,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        // Placeholder implementation
        List<TravelPlanDto> plans = List.of(
                TravelPlanDto.builder().id(1L).title("Active Plan").status(status).build(),
                TravelPlanDto.builder().id(2L).title("Another Plan").status(status).build()
        );
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TravelPlanDto> pageResult = new org.springframework.data.domain.PageImpl<>(
                plans, pageable, plans.size()
        );
        
        PagedResponse<TravelPlanDto> response = PagedResponse.fromPage(
                pageResult,
                "Travel plans retrieved successfully",
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get travel plans by type", description = "Retrieves travel plans filtered by type")
    public ResponseEntity<PagedResponse<TravelPlanDto>> getTravelPlansByType(
            @Parameter(description = "Travel type") @PathVariable TravelType type,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        // Placeholder implementation
        List<TravelPlanDto> plans = List.of(
                TravelPlanDto.builder().id(1L).title("Leisure Trip").travelType(type).build(),
                TravelPlanDto.builder().id(2L).title("Another Trip").travelType(type).build()
        );
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<TravelPlanDto> pageResult = new org.springframework.data.domain.PageImpl<>(
                plans, pageable, plans.size()
        );
        
        PagedResponse<TravelPlanDto> response = PagedResponse.fromPage(
                pageResult,
                "Travel plans retrieved successfully",
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update travel plan status", description = "Updates the status of a travel plan")
    public ResponseEntity<ApiResponse<TravelPlanDto>> updateTravelPlanStatus(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam TravelPlanStatus status,
            HttpServletRequest request) {
        
        // Get travel plan from memory storage
        TravelPlanDto existingPlan = travelPlans.get(id);
        
        if (existingPlan == null) {
            ApiResponse<TravelPlanDto> response = ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    TRAVEL_PLAN_NOT_FOUND_MSG + id,
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        // Update status
        TravelPlanDto updatedPlan = TravelPlanDto.builder()
                .id(existingPlan.getId())
                .title(existingPlan.getTitle())
                .description(existingPlan.getDescription())
                .status(status)
                .travelType(existingPlan.getTravelType())
                .startDate(existingPlan.getStartDate())
                .endDate(existingPlan.getEndDate())
                .estimatedBudget(existingPlan.getEstimatedBudget())
                .numberOfTravelers(existingPlan.getNumberOfTravelers())
                .originLocation(existingPlan.getOriginLocation())
                .destinationLocation(existingPlan.getDestinationLocation())
                .isPublic(existingPlan.getIsPublic())
                .build();
        
        // Update in memory
        travelPlans.put(id, updatedPlan);
        
        ApiResponse<TravelPlanDto> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Travel plan status updated successfully",
                updatedPlan,
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/compatible-travelers")
    @Operation(summary = "Find compatible travelers", description = "Finds travelers with similar destinations and compatible dates")
    public ResponseEntity<ApiResponse<List<TravelerMatchDto>>> findCompatibleTravelers(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            HttpServletRequest request) {
        
        // Get the reference travel plan from memory
        TravelPlanDto referencePlan = travelPlans.get(id);
        
        if (referencePlan == null) {
            ApiResponse<List<TravelerMatchDto>> response = ApiResponse.error(
                    HttpStatus.NOT_FOUND.value(),
                    TRAVEL_PLAN_NOT_FOUND_MSG + id,
                    request.getRequestURI()
            );
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        
        // Find compatible travelers in memory
        List<TravelerMatchDto> compatibleTravelers = new ArrayList<>();
        
        for (Map.Entry<Long, TravelPlanDto> entry : travelPlans.entrySet()) {
            Long otherPlanId = entry.getKey();
            TravelPlanDto otherPlan = entry.getValue();
            
            // Skip the same plan and non-ACTIVE plans
            if (otherPlanId.equals(id) || otherPlan.getStatus() != TravelPlanStatus.ACTIVE) {
                continue;
            }
            
            // Check destination match (case-insensitive) and date overlap
            if (referencePlan.getDestinationLocation() != null && 
                otherPlan.getDestinationLocation() != null &&
                referencePlan.getDestinationLocation().equalsIgnoreCase(otherPlan.getDestinationLocation()) &&
                isDateOverlap(referencePlan.getStartDate(), referencePlan.getEndDate(), 
                             otherPlan.getStartDate(), otherPlan.getEndDate())) {
                
                // Calculate overlapping days
                long overlappingDays = calculateOverlappingDays(
                    referencePlan.getStartDate(), referencePlan.getEndDate(),
                    otherPlan.getStartDate(), otherPlan.getEndDate()
                );
                
                // Calculate compatibility score
                double compatibilityScore = (double) overlappingDays / 
                    Math.min(
                        getDaysBetween(referencePlan.getStartDate(), referencePlan.getEndDate()),
                        getDaysBetween(otherPlan.getStartDate(), otherPlan.getEndDate())
                    ) * 100;
                
                // Create match DTO (simplified user info)
                TravelerMatchDto match = TravelerMatchDto.builder()
                    .userId(otherPlanId) // Using plan ID as traveler ID for demo
                    .username("traveler" + otherPlanId)
                    .firstName("Traveler")
                    .lastName("User " + otherPlanId)
                    .travelPlanId(otherPlanId)
                    .travelPlanTitle(otherPlan.getTitle())
                    .destinationLocation(otherPlan.getDestinationLocation())
                    .travelStartDate(otherPlan.getStartDate())
                    .travelEndDate(otherPlan.getEndDate())
                    .numberOfTravelers(otherPlan.getNumberOfTravelers())
                    .daysOverlap((int) overlappingDays)
                    .compatibilityScore(Math.round(compatibilityScore * 10.0) / 10.0)
                    .build();
                
                compatibleTravelers.add(match);
            }
        }
        
        if (compatibleTravelers.isEmpty()) {
            ApiResponse<List<TravelerMatchDto>> response = ApiResponse.success(
                    HttpStatus.OK.value(),
                    "No compatible travelers found for this travel plan",
                    compatibleTravelers,
                    request.getRequestURI()
            );
            return ResponseEntity.ok(response);
        }
        
        ApiResponse<List<TravelerMatchDto>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Compatible travelers found successfully",
                compatibleTravelers,
                request.getRequestURI()
        );
        return ResponseEntity.ok(response);
    }
    
    // Helper methods for date calculations
    private boolean isDateOverlap(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
    
    private long calculateOverlappingDays(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        LocalDateTime overlapStart = start1.isAfter(start2) ? start1 : start2;
        LocalDateTime overlapEnd = end1.isBefore(end2) ? end1 : end2;
        return ChronoUnit.DAYS.between(overlapStart, overlapEnd);
    }
    
    private long getDaysBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
