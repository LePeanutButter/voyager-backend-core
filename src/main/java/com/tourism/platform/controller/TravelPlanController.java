package com.tourism.platform.controller;

import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.PagedResponse;
import com.tourism.platform.dto.TravelPlanDto;
import com.tourism.platform.dto.TravelPlanActivityDto;
import com.tourism.platform.dto.CreateTravelPlanActivityRequestDto;
import com.tourism.platform.dto.UpdateTravelPlanActivityRequestDto;
import com.tourism.platform.dto.ReservationDto;
import com.tourism.platform.dto.TravelConnectionDto;
import com.tourism.platform.dto.TravelerMatchDto;
import com.tourism.platform.model.TravelPlanStatus;
import com.tourism.platform.model.TravelType;
import com.tourism.platform.repository.UserRepository;
import com.tourism.platform.service.TravelPlanActivityService;
import com.tourism.platform.service.TravelPlanService;
import com.tourism.platform.service.SocialService;
import com.tourism.platform.config.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.tourism.platform.model.User;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import java.util.List;


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
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Validated
public class TravelPlanController {

    // Constants for duplicated literals
    private static final String TRAVEL_PLANS_RETRIEVED_SUCCESSFULLY = "Travel plans retrieved successfully";
    private static final String CREATED_AT = "createdAt";

    private final TravelPlanActivityService travelPlanActivityService;
    private final SocialService socialService;
    private final UserRepository userRepository;
    private final TravelPlanService travelPlanService;

    // Constants for error messages
    private static final String TRAVEL_PLAN_NOT_FOUND_MSG = "Travel plan not found with ID: ";

    @PostMapping
    @Operation(summary = "Create a new travel plan",
            description = "Creates a travel plan for the authenticated user from the request body.")
    /**
     * Create a new travel plan for the authenticated user.
     *
     * @param travelPlanDto DTO containing travel plan details
     * @param request       current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing the created TravelPlanDto and HTTP 201
     */
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
    /**
     * Retrieve a travel plan by id for the authenticated user.
     *
     * @param id      travel plan id
     * @param request current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing the TravelPlanDto
     */
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
        /**
         * Retrieve all travel plans that belong to the authenticated user.
         *
         * @param request current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing list of TravelPlanDto
         */
        public ResponseEntity<ApiResponse<List<TravelPlanDto>>> getMyTravelPlans(HttpServletRequest request) {
        User user = getAuthenticatedUser();
        List<TravelPlanDto> plans = travelPlanService.getTravelPlanDtosByUser(user.getId());

        ApiResponse<List<TravelPlanDto>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                TRAVEL_PLANS_RETRIEVED_SUCCESSFULLY,
                plans,
                request.getRequestURI()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get travel plans by user", description = "Retrieves all travel plans for a specific user")
    /**
     * Retrieve paginated travel plans for a given user id.
     *
     * @param userId  id of the user whose travel plans are requested
     * @param page    page number (0-based)
     * @param size    page size
     * @param request current HTTP request used to build response path
     * @return ResponseEntity with PagedResponse containing TravelPlanDto
     */
    public ResponseEntity<PagedResponse<TravelPlanDto>> getTravelPlansByUser(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        List<TravelPlanDto> plans = travelPlanService.getTravelPlanDtosByUser(userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_AT).descending());
        Page<TravelPlanDto> pageResult = new org.springframework.data.domain.PageImpl<>(
                plans, pageable, plans.size()
        );

        PagedResponse<TravelPlanDto> response = PagedResponse.fromPage(
                pageResult,
                TRAVEL_PLANS_RETRIEVED_SUCCESSFULLY,
                HttpStatus.OK.value(),
                request.getRequestURI()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update travel plan", description = "Updates an existing travel plan")
    /**
     * Update an existing travel plan belonging to the authenticated user.
     *
     * @param id            id of the travel plan to update
     * @param travelPlanDto DTO containing updated travel plan fields
     * @param request       current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing the updated TravelPlanDto
     */
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
    /**
     * Delete a travel plan owned by the authenticated user.
     *
     * @param id      id of the travel plan to delete
     * @param request current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse<Void> indicating deletion success
     */
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
    /**
     * Add a new activity to an existing travel plan.
     *
     * @param id          travel plan id
     * @param activityDto DTO with activity creation data
     * @param request     current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing created TravelPlanActivityDto and HTTP 201
     */
    public ResponseEntity<ApiResponse<TravelPlanActivityDto>> addActivity(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            @Valid @RequestBody CreateTravelPlanActivityRequestDto activityDto,
            HttpServletRequest request) {
        TravelPlanActivityDto createdActivity = travelPlanActivityService.createActivity(id, activityDto);

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
    /**
     * Retrieve activities for the specified travel plan.
     *
     * @param id      travel plan id
     * @param request current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing list of TravelPlanActivityDto
     */
    public ResponseEntity<ApiResponse<List<TravelPlanActivityDto>>> getActivities(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            HttpServletRequest request) {
        List<TravelPlanActivityDto> activities = travelPlanActivityService.getActivities(id);

        ApiResponse<List<TravelPlanActivityDto>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Activities retrieved successfully",
                activities,
                request.getRequestURI()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/activities/{activityId}")
    @Operation(summary = "Update activity in travel plan", description = "Updates an existing activity in an existing travel plan")
    /**
     * Update an activity inside a travel plan.
     *
     * @param id          travel plan id
     * @param activityId  activity id to update
     * @param activityDto DTO containing updated activity fields
     * @param request     current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing the updated TravelPlanActivityDto
     */
    public ResponseEntity<ApiResponse<TravelPlanActivityDto>> updateActivity(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            @Parameter(description = "Activity ID") @PathVariable Long activityId,
            @Valid @RequestBody UpdateTravelPlanActivityRequestDto activityDto,
            HttpServletRequest request) {

        TravelPlanActivityDto updatedActivity = travelPlanActivityService.updateActivity(id, activityId, activityDto);

        ApiResponse<TravelPlanActivityDto> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Activity updated successfully",
                updatedActivity,
                request.getRequestURI()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/connections")
    @Operation(summary = "Get accepted connections by travel plan", description = "Retrieves accepted traveler connections in the context of a travel plan")
    /**
     * Retrieve accepted traveler connections associated with a travel plan.
     *
     * @param id      travel plan id
     * @param status  connection status filter (defaults to ACCEPTED)
     * @param request current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing list of TravelConnectionDto
     */
    public ResponseEntity<ApiResponse<List<TravelConnectionDto>>> getTravelPlanConnections(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            @Parameter(description = "Connection status filter") @RequestParam(defaultValue = "ACCEPTED") String status,
            HttpServletRequest request) {

        List<TravelConnectionDto> connections = "ACCEPTED".equalsIgnoreCase(status)
                ? socialService.getAcceptedConnectionsByTravelPlan(id)
                : List.of();

        ApiResponse<List<TravelConnectionDto>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                connections.isEmpty() ? "No accepted connections found for this travel plan" : "Connections retrieved successfully",
                connections,
                request.getRequestURI()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/reservations")
    @Operation(summary = "Add reservation to travel plan", description = "Adds a new reservation to an existing travel plan")
    /**
     * Add a reservation to a travel plan (placeholder implementation).
     *
     * @param id             travel plan id
     * @param reservationDto reservation DTO with reservation details
     * @param request        current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing created ReservationDto and HTTP 201
     */
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
    /**
     * Retrieve reservations associated with a travel plan (placeholder implementation).
     *
     * @param id      travel plan id
     * @param request current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing list of ReservationDto
     */
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
    /**
     * Generate a shareable link for a travel plan (placeholder implementation).
     *
     * @param id      travel plan id
     * @param request current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing the share URL
     */
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
    /**
     * Retrieve a travel plan by its public share token (placeholder implementation).
     *
     * @param shareToken public share token
     * @param request    current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing the shared TravelPlanDto
     */
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
    /**
     * Retrieve paginated travel plans filtered by status.
     *
     * @param status  travel plan status
     * @param page    page number (0-based)
     * @param size    page size
     * @param request current HTTP request used to build response path
     * @return ResponseEntity with PagedResponse containing TravelPlanDto
     */
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

        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_AT).descending());
        Page<TravelPlanDto> pageResult = new org.springframework.data.domain.PageImpl<>(
                plans, pageable, plans.size()
        );

        PagedResponse<TravelPlanDto> response = PagedResponse.fromPage(
                pageResult,
                TRAVEL_PLANS_RETRIEVED_SUCCESSFULLY,
                HttpStatus.OK.value(),
                request.getRequestURI()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get travel plans by type", description = "Retrieves travel plans filtered by type")
    /**
     * Retrieve paginated travel plans filtered by type.
     *
     * @param type    travel type
     * @param page    page number (0-based)
     * @param size    page size
     * @param request current HTTP request used to build response path
     * @return ResponseEntity with PagedResponse containing TravelPlanDto
     */
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

        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_AT).descending());
        Page<TravelPlanDto> pageResult = new org.springframework.data.domain.PageImpl<>(
                plans, pageable, plans.size()
        );

        PagedResponse<TravelPlanDto> response = PagedResponse.fromPage(
                pageResult,
                TRAVEL_PLANS_RETRIEVED_SUCCESSFULLY,
                HttpStatus.OK.value(),
                request.getRequestURI()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update travel plan status", description = "Updates the status of a travel plan")
    /**
     * Update the status of a travel plan for the authenticated user.
     *
     * @param id      travel plan id
     * @param status  new status to set
     * @param request current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing the updated TravelPlanDto
     */
    public ResponseEntity<ApiResponse<TravelPlanDto>> updateTravelPlanStatus(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            @Parameter(description = "New status") @RequestParam TravelPlanStatus status,
            HttpServletRequest request) {

        User user = getAuthenticatedUser();
        TravelPlanDto updatedPlan = travelPlanService.updateTravelPlanStatus(id, user.getId(), status);

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
    /**
     * Find compatible travelers for the specified travel plan.
     *
     * @param id      travel plan id
     * @param request current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing list of TravelerMatchDto
     */
    public ResponseEntity<ApiResponse<List<TravelerMatchDto>>> findCompatibleTravelers(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            HttpServletRequest request) {

        User user = getAuthenticatedUser();
        List<TravelerMatchDto> compatibleTravelers = travelPlanService.findCompatibleTravelers(id, user.getId());

        String message = compatibleTravelers.isEmpty()
                ? "No compatible travelers found for this travel plan"
                : "Compatible travelers found successfully";

        ApiResponse<List<TravelerMatchDto>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                message,
                compatibleTravelers,
                request.getRequestURI()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/activities/{activityId}")
    @Operation(summary = "Delete activity from travel plan", description = "Removes an activity from an existing travel plan")
    /**
     * Delete an activity from a travel plan.
     *
     * @param id         travel plan id
     * @param activityId activity id to delete
     * @param request    current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse<Void> indicating deletion success
     */
    public ResponseEntity<ApiResponse<Void>> deleteActivity(
            @Parameter(description = "Travel plan ID") @PathVariable Long id,
            @Parameter(description = "Activity ID") @PathVariable Long activityId,
            HttpServletRequest request) {

        travelPlanActivityService.deleteActivity(id, activityId);

        ApiResponse<Void> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Activity deleted successfully",
                request.getRequestURI()
        );

        return ResponseEntity.ok(response);
    }

    private User getAuthenticatedUser() {
        /**
         * Resolve the currently authenticated User from the security context.
         *
         * @return User entity of the authenticated principal
         * @throws EntityNotFoundException when the user cannot be found in repository
         */
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
