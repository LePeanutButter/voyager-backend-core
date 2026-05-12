package com.tourism.platform.controller;

import com.tourism.platform.config.OpenApiConfig;
import com.tourism.platform.dto.*;
import com.tourism.platform.model.Message;
import com.tourism.platform.model.User;
import com.tourism.platform.security.CustomUserDetailsService;
import com.tourism.platform.security.JwtTokenProvider;
import com.tourism.platform.service.SocialService;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * REST Controller for Social Features
 * 
 * This controller follows REST best practices and Richardson Maturity Model Level 3:
 * - Proper HTTP methods (GET, POST, PUT, DELETE)
 * - Resource-based URIs
 * - Self-descriptive responses
 * - Standard HTTP status codes
 * - Content negotiation
 */
@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
@Tag(name = "Social Features", description = "APIs for traveler social interactions")
@SecurityRequirement(name = OpenApiConfig.BEARER_JWT)
@Validated
public class SocialController {

    // Constants for duplicated literals
    private static final String RATING = "rating";
    private static final String COMMENT = "comment";
    private static final String AUTHOR = "author";
    private static final String CREATED_AT = "createdAt";
    private static final String PARTICIPANT = "participant";
    private static final String LAST_MESSAGE = "lastMessage";
    private static final String UNREAD_COUNT = "unreadCount";
    private static final String CONTENT = "content";
    private static final String POST_ID = "postId";
    private static final String JOHN_DOE = "John Doe";
    private static final String JANE_SMITH = "Jane Smith";
    private static final String MIKE_JOHNSON = "Mike Johnson";

    private final SocialService socialService;
    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    @Value("${app.social.demo-enabled:false}")
    private boolean demoEndpointsEnabled;

    /**
     * Resolves the current user id from {@link SecurityContextHolder} (authenticated, non-anonymous principal)
     * or, when the context is not populated (e.g. tests without the security filter), from a valid Bearer JWT.
     *
     * @throws AuthenticationCredentialsNotFoundException if the caller cannot be identified
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            Long fromPrincipal = resolveUserIdFromPrincipal(authentication.getPrincipal());
            if (fromPrincipal != null) {
                return fromPrincipal;
            }
        }
        Long fromJwt = tryResolveUserIdFromJwt(request);
        if (fromJwt != null) {
            return fromJwt;
        }
        throw new AuthenticationCredentialsNotFoundException("User is not authenticated");
    }

    private Long tryResolveUserIdFromJwt(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null || !tokenProvider.validateToken(token)) {
            return null;
        }
        return tokenProvider.getUserIdFromJWT(token);
    }

    /**
     * Maps the authentication principal to a user id: domain {@link User}, generic {@link UserDetails}
     * (reload by username), or raw {@link String} username/email via {@link CustomUserDetailsService}.
     */
    private Long resolveUserIdFromPrincipal(Object principal) {
        if (principal == null) {
            return null;
        }
        if (principal instanceof User domainUser) {
            return domainUser.getId();
        }
        if (principal instanceof UserDetails details) {
            try {
                UserDetails loaded = customUserDetailsService.loadUserByUsername(details.getUsername());
                if (loaded instanceof User u) {
                    return u.getId();
                }
            } catch (UsernameNotFoundException ignored) {
                return null;
            }
        }
        if (principal instanceof String username) {
            try {
                UserDetails loaded = customUserDetailsService.loadUserByUsername(username);
                if (loaded instanceof User u) {
                    return u.getId();
                }
            } catch (UsernameNotFoundException ignored) {
                return null;
            }
        }
        return null;
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private void enforceSelfOrPrivileged(Long requestedUserId, HttpServletRequest request) {
        Long currentUserId = getCurrentUserId(request);
        if (currentUserId.equals(requestedUserId)) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(java.util.stream.Collectors.toSet());
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_SUPER_ADMIN")) {
            return;
        }
        throw new AuthenticationCredentialsNotFoundException("Access denied for requested user resource");
    }

    private void ensureDemoEndpointEnabled() {
        if (!demoEndpointsEnabled) {
            throw new IllegalArgumentException("This demo endpoint is disabled in the current environment");
        }
    }

    // Traveler Connections
    @PostMapping("/connections")
    @Operation(summary = "Send connection request", description = "Sends a connection request to another traveler")
    /**
     * Send a connection request from the authenticated user to another traveler.
     *
     * @param request     DTO describing the recipient and optional message
     * @param httpRequest current HTTP request used to resolve authentication and build response path
     * @return ResponseEntity with ApiResponse containing the created ConnectionRequestDto and HTTP 201
     */
    public ResponseEntity<ApiResponse<ConnectionRequestDto>> sendConnectionRequest(
            @Valid @RequestBody SendConnectionRequestDto request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        ConnectionRequestDto connectionRequest = socialService.sendConnectionRequest(request, currentUserId);
        ApiResponse<ConnectionRequestDto> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Connection request sent successfully",
                connectionRequest,
                httpRequest.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/connections/{userId}")
    @Operation(summary = "Get user connections", description = "Retrieves all connections for a user")
    /**
     * Retrieve all accepted connections for the specified user.
     *
     * @param userId  id of the user whose connections are requested
     * @param request current HTTP request (used to build response path)
     * @return ResponseEntity with ApiResponse containing a list of TravelConnectionDto
     */
    public ResponseEntity<ApiResponse<List<TravelConnectionDto>>> getUserConnections(
            @Parameter(description = "User ID") @PathVariable Long userId,
            HttpServletRequest request) {
        enforceSelfOrPrivileged(userId, request);
        List<TravelConnectionDto> connections = socialService.getUserConnections(userId);
        ApiResponse<List<TravelConnectionDto>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Connections retrieved successfully",
                connections,
                request.getRequestURI()
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/connections/{requestId}/accept")
    @Operation(summary = "Accept connection request", description = "Accepts a pending connection request")
    public ResponseEntity<ApiResponse<ConnectionRequestDto>> acceptConnectionRequest(
            @Parameter(description = "Connection request ID") @PathVariable Long requestId,
            HttpServletRequest httpRequest) {
        /**
         * Accept a pending connection request on behalf of the authenticated user.
         *
         * @param requestId   id of the connection request to accept
         * @param httpRequest current HTTP request used to resolve authentication and build response path
         * @return ResponseEntity with ApiResponse containing the updated ConnectionRequestDto
         */
        
        Long currentUserId = getCurrentUserId(httpRequest);
        
        ConnectionRequestDto connectionRequest = socialService.acceptConnectionRequest(requestId, currentUserId);
        
        ApiResponse<ConnectionRequestDto> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Connection request accepted",
                connectionRequest,
                httpRequest.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/connections/{requestId}/reject")
    @Operation(summary = "Reject connection request", description = "Rejects a pending connection request")
    public ResponseEntity<ApiResponse<ConnectionRequestDto>> rejectConnectionRequest(
            @Parameter(description = "Connection request ID") @PathVariable Long requestId,
            HttpServletRequest httpRequest) {
        /**
         * Reject a pending connection request on behalf of the authenticated user.
         *
         * @param requestId   id of the connection request to reject
         * @param httpRequest current HTTP request used to resolve authentication and build response path
         * @return ResponseEntity with ApiResponse containing the updated ConnectionRequestDto
         */
        
        Long currentUserId = getCurrentUserId(httpRequest);
        
        ConnectionRequestDto connectionRequest = socialService.rejectConnectionRequest(requestId, currentUserId);
        
        ApiResponse<ConnectionRequestDto> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Connection request rejected",
                connectionRequest,
                httpRequest.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/connections/pending")
    @Operation(summary = "Get pending connection requests", description = "Retrieves all pending connection requests received by the user")
    public ResponseEntity<ApiResponse<List<ConnectionRequestDto>>> getPendingRequests(
            HttpServletRequest httpRequest) {
        /**
         * Retrieve pending connection requests received by the authenticated user.
         *
         * @param httpRequest current HTTP request used to resolve authentication and build response path
         * @return ResponseEntity with ApiResponse containing a list of pending ConnectionRequestDto
         */
        
        Long currentUserId = getCurrentUserId(httpRequest);
        
        List<ConnectionRequestDto> pendingRequests = socialService.getPendingRequestsForUser(currentUserId);
        
        ApiResponse<List<ConnectionRequestDto>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Pending requests retrieved successfully",
                pendingRequests,
                httpRequest.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/connections/sent")
    @Operation(summary = "Get sent connection requests", description = "Retrieves all pending connection requests sent by the user")
    public ResponseEntity<ApiResponse<List<ConnectionRequestDto>>> getSentRequests(
            HttpServletRequest httpRequest) {
        /**
         * Retrieve connection requests sent by the authenticated user.
         *
         * @param httpRequest current HTTP request used to resolve authentication and build response path
         * @return ResponseEntity with ApiResponse containing a list of sent ConnectionRequestDto
         */
        
        Long currentUserId = getCurrentUserId(httpRequest);
        
        List<ConnectionRequestDto> sentRequests = socialService.getSentRequestsForUser(currentUserId);
        
        ApiResponse<List<ConnectionRequestDto>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Sent requests retrieved successfully",
                sentRequests,
                httpRequest.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/connections/{connectionId}")
    @Operation(summary = "Remove connection", description = "Removes a connection between users")
    public ResponseEntity<ApiResponse<Void>> removeConnection(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            HttpServletRequest request) {

        /**
         * Remove an existing connection between the authenticated user and another user.
         *
         * @param connectionId id of the connection to remove
         * @param request      current HTTP request used to resolve authentication and build response path
         * @return ResponseEntity with ApiResponse<Void> indicating success
         */
        Long currentUserId = getCurrentUserId(request);
        socialService.deleteConnection(connectionId, currentUserId);
        ApiResponse<Void> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Connection removed successfully",
                request.getRequestURI()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/travelers/{id}/summary")
    @Operation(summary = "Get traveler summary profile", description = "Retrieves the public summary profile for a compatible traveler")
    public ResponseEntity<ApiResponse<TravelerSummaryDto>> getTravelerSummary(
            @Parameter(description = "Traveler ID") @PathVariable("id") Long travelerId,
            HttpServletRequest request) {

        /**
         * Retrieve a public summary profile for a traveler identified by id.
         *
         * @param travelerId id of the traveler
         * @param request    current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing TravelerSummaryDto
         */
        TravelerSummaryDto summary = socialService.getTravelerSummary(travelerId);
        ApiResponse<TravelerSummaryDto> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Traveler summary retrieved successfully",
                summary,
                request.getRequestURI()
        );
        return ResponseEntity.ok(response);
    }

    // Reviews and Ratings
    @PostMapping("/reviews")
    @Operation(summary = "Create review", description = "Creates a new review for a destination, service, or activity")
    /**
     * Create a new review for a specified target (destination, service or activity).
     *
     * This is a simplified placeholder implementation that returns a synthetic response.
     *
     * @param reviewData payload containing review fields
     * @param request    current HTTP request used to build response path
     * @return ResponseEntity with ApiResponse containing created review metadata and HTTP 201
     */
    public ResponseEntity<ApiResponse<Map<String, Object>>> createReview(
            @Valid @RequestBody Map<String, Object> reviewData,
            HttpServletRequest request) {
        ensureDemoEndpointEnabled();
        Map<String, Object> responseData = Map.of(
                "reviewId", 1L,
                "targetId", reviewData.get("targetId"),
                "targetType", reviewData.get("targetType"),
                RATING, reviewData.get(RATING),
                COMMENT, reviewData.get(COMMENT),
                "status", "PUBLISHED"
        );
        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Review created successfully",
                responseData,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/reviews/{targetType}/{targetId}")
    @Operation(summary = "Get reviews", description = "Retrieves reviews for a specific target")
    /**
     * Retrieve paginated reviews for a specific target type and id.
     *
     * @param targetType target type (destination, service, activity)
     * @param targetId   id of the target
     * @param page       page number (0-based)
     * @param size       page size
     * @param request    current HTTP request used to build response path
     * @return ResponseEntity with PagedResponse containing reviews
     */
    public ResponseEntity<PagedResponse<Map<String, Object>>> getReviews(
            @Parameter(description = "Target type (destination, service, activity)") @PathVariable String targetType,
            @Parameter(description = "Target ID") @PathVariable Long targetId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        ensureDemoEndpointEnabled();
        List<Map<String, Object>> reviews = List.of(
                Map.of("id", 1L, RATING, 5, COMMENT, "Great experience!", AUTHOR, JOHN_DOE),
                Map.of("id", 2L, RATING, 4, COMMENT, "Amazing place!", AUTHOR, JANE_SMITH),
                Map.of("id", 3L, RATING, 5, COMMENT, "Would recommend!", AUTHOR, MIKE_JOHNSON)
        );
        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_AT).descending());
        Page<Map<String, Object>> pageResult = new org.springframework.data.domain.PageImpl<>(
                reviews, pageable, reviews.size()
        );
        PagedResponse<Map<String, Object>> response = PagedResponse.fromPage(
                pageResult,
                "Reviews retrieved successfully",
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/reviews/{reviewId}")
    @Operation(summary = "Update review", description = "Updates an existing review")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateReview(
            @Parameter(description = "Review ID") @PathVariable Long reviewId,
            @Valid @RequestBody Map<String, Object> updateData,
            HttpServletRequest request) {
        ensureDemoEndpointEnabled();
        /**
         * Update an existing review. This placeholder returns an updated metadata map.
         *
         * @param reviewId   id of the review to update
         * @param updateData map with fields to update
         * @param request    current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing updated review metadata
         */
        Map<String, Object> responseData = Map.of(
                "reviewId", reviewId,
                RATING, updateData.get(RATING),
                COMMENT, updateData.get(COMMENT),
                "updatedAt", java.time.LocalDateTime.now()
        );
        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Review updated successfully",
                responseData,
                request.getRequestURI()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reviews/{reviewId}")
    @Operation(summary = "Delete review", description = "Deletes a review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @Parameter(description = "Review ID") @PathVariable Long reviewId,
            HttpServletRequest request) {
        ensureDemoEndpointEnabled();
        /**
         * Delete an existing review identified by id.
         *
         * @param reviewId id of the review to delete
         * @param request  current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse<Void> indicating deletion success
         */
        ApiResponse<Void> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Review deleted successfully",
                request.getRequestURI()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations/{userId}")
    @Operation(summary = "Get conversations", description = "Retrieves all conversations for a user")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConversations(
            @Parameter(description = "User ID") @PathVariable Long userId,
            HttpServletRequest request) {
        ensureDemoEndpointEnabled();
        enforceSelfOrPrivileged(userId, request);
        /**
         * Retrieve a list of conversation summaries for the specified user.
         *
         * @param userId  id of the user whose conversations are requested
         * @param request current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing a list of conversation summary maps
         */
        List<Map<String, Object>> conversations = List.of(
                Map.of("id", 1L, PARTICIPANT, JOHN_DOE, LAST_MESSAGE, "Hi there!", UNREAD_COUNT, 2),
                Map.of("id", 2L, PARTICIPANT, JANE_SMITH, LAST_MESSAGE, "See you soon!", UNREAD_COUNT, 0),
                Map.of("id", 3L, PARTICIPANT, MIKE_JOHNSON, LAST_MESSAGE, "Thanks!", UNREAD_COUNT, 1)
        );
        
        ApiResponse<List<Map<String, Object>>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Conversations retrieved successfully",
                conversations,
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    // Update getConversationMessages endpoint
    @GetMapping("/connections/{connectionId}/messages")
    @Operation(summary = "Get conversation messages", description = "Retrieves paginated messages from a specific connection")
    public ResponseEntity<PagedResponse<Message>> getConversationMessages(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "User ID (deprecated; ignored)") @RequestParam(required = false) Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {

        /**
         * Retrieve paginated messages for a conversation (connection) between users.
         *
         * @param connectionId id of the connection/conversation
         * @param userId       id of the requesting user
         * @param page         page number (0-based)
         * @param size         page size
         * @param request      current HTTP request used to build response path
         * @return ResponseEntity with PagedResponse containing Message objects
         */

        Long currentUserId = getCurrentUserId(request);
        if (userId != null && !userId.equals(currentUserId)) {
            throw new IllegalArgumentException("userId parameter does not match authenticated user");
        }
        Page<Message> messagesPage = socialService.getConversationMessagesPaginated(connectionId, currentUserId, page, size);

        PagedResponse<Message> response = PagedResponse.fromPage(
                messagesPage,
                "Messages retrieved successfully",
                HttpStatus.OK.value(),
                request.getRequestURI()
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/messages/{messageId}/read")
    @Operation(summary = "Mark message as read", description = "Marks a message as read")
    public ResponseEntity<ApiResponse<Void>> markMessageAsRead(
            @Parameter(description = "Message ID") @PathVariable Long messageId,
            HttpServletRequest request) {
        /**
         * Mark a message as read for the authenticated user.
         *
         * @param messageId id of the message to mark as read
         * @param request   current HTTP request used to resolve authentication and build response path
         * @return ResponseEntity with ApiResponse<Void> indicating success
         */
        Long currentUserId = getCurrentUserId(request);
        socialService.markMessageAsRead(messageId, currentUserId);
        ApiResponse<Void> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Message marked as read",
                request.getRequestURI()
        );
        return ResponseEntity.ok(response);
    }

    // Social Feed
    @GetMapping("/feed/{userId}")
    @Operation(summary = "Get social feed", description = "Retrieves the social feed for a user")
    public ResponseEntity<PagedResponse<Map<String, Object>>> getSocialFeed(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        ensureDemoEndpointEnabled();
        enforceSelfOrPrivileged(userId, request);
        /**
         * Retrieve a paginated social feed for the specified user.
         *
         * @param userId  id of the user whose feed is requested
         * @param page    page number (0-based)
         * @param size    page size
         * @param request current HTTP request used to build response path
         * @return ResponseEntity with PagedResponse containing feed post maps
         */
        List<Map<String, Object>> posts = List.of(
                Map.of("id", 1L, AUTHOR, JOHN_DOE, CONTENT, "Traveling to Paris!", "type", "POST"),
                Map.of("id", 2L, AUTHOR, JANE_SMITH, CONTENT, "Sharing photos from Rome", "type", "PHOTO"),
                Map.of("id", 3L, AUTHOR, MIKE_JOHNSON, CONTENT, "Tokyo is amazing!", "type", "POST")
        );
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_AT).descending());
        Page<Map<String, Object>> pageResult = new org.springframework.data.domain.PageImpl<>(
                posts, pageable, posts.size()
        );
        
        PagedResponse<Map<String, Object>> response = PagedResponse.fromPage(
                pageResult,
                "Feed retrieved successfully",
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/posts")
    @Operation(summary = "Create post", description = "Creates a new post in the social feed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPost(
            @Valid @RequestBody Map<String, Object> postData,
            HttpServletRequest request) {
        ensureDemoEndpointEnabled();
        /**
         * Create a new post in the social feed.
         *
         * @param postData map containing post fields such as content and type
         * @param request  current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing created post metadata and HTTP 201
         */
        Map<String, Object> responseData = Map.of(
                POST_ID, 1L,
                CONTENT, postData.get(CONTENT),
                "type", postData.getOrDefault("type", "POST"),
                CREATED_AT, java.time.LocalDateTime.now()
        );
        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Post created successfully",
                responseData,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/posts/{postId}/like")
    @Operation(summary = "Like post", description = "Likes a post in the social feed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> likePost(
            @Parameter(description = "Post ID") @PathVariable Long postId,
            HttpServletRequest request) {
        ensureDemoEndpointEnabled();
        /**
         * Like a post on behalf of the authenticated user.
         *
         * @param postId  id of the post to like
         * @param request current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing like metadata
         */
        Map<String, Object> responseData = Map.of(
                POST_ID, postId,
                "liked", true,
                "likeCount", 42,
                "likedAt", java.time.LocalDateTime.now()
        );
        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Post liked successfully",
                responseData,
                request.getRequestURI()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/posts/{postId}/like")
    @Operation(summary = "Unlike post", description = "Removes like from a post in the social feed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unlikePost(
            @Parameter(description = "Post ID") @PathVariable Long postId,
            HttpServletRequest request) {
        ensureDemoEndpointEnabled();
        /**
         * Remove a like from a post for the authenticated user.
         *
         * @param postId  id of the post to unlike
         * @param request current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing unlike metadata
         */
        Map<String, Object> responseData = Map.of(
                POST_ID, postId,
                "liked", false,
                "likeCount", 41,
                "unlikedAt", java.time.LocalDateTime.now()
        );
        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Post unliked successfully",
                responseData,
                request.getRequestURI()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/posts/{postId}/comments")
    @Operation(summary = "Comment on post", description = "Adds a comment to a post")
    public ResponseEntity<ApiResponse<Map<String, Object>>> commentOnPost(
            @Parameter(description = "Post ID") @PathVariable Long postId,
            @Valid @RequestBody Map<String, Object> commentData,
            HttpServletRequest request) {
        ensureDemoEndpointEnabled();
        /**
         * Add a comment to a post.
         *
         * @param postId      id of the post to comment on
         * @param commentData map containing comment content
         * @param request     current HTTP request used to build response path
         * @return ResponseEntity with ApiResponse containing created comment metadata and HTTP 201
         */
        Map<String, Object> responseData = Map.of(
                "commentId", 1L,
                POST_ID, postId,
                CONTENT, commentData.get(CONTENT),
                CREATED_AT, java.time.LocalDateTime.now()
        );
        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Comment added successfully",
                responseData,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "Get post comments", description = "Retrieves all comments for a post")
    public ResponseEntity<PagedResponse<Map<String, Object>>> getPostComments(
            @Parameter(description = "Post ID") @PathVariable Long postId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        ensureDemoEndpointEnabled();
        /**
         * Retrieve paginated comments for a specific post.
         *
         * @param postId  id of the post
         * @param page    page number (0-based)
         * @param size    page size
         * @param request current HTTP request used to build response path
         * @return ResponseEntity with PagedResponse containing comment maps
         */
        List<Map<String, Object>> comments = List.of(
                Map.of("id", 1L, AUTHOR, JOHN_DOE, CONTENT, "Great post!", CREATED_AT, java.time.LocalDateTime.now()),
                Map.of("id", 2L, AUTHOR, JANE_SMITH, CONTENT, "Thanks for sharing!", CREATED_AT, java.time.LocalDateTime.now())
        );
        Pageable pageable = PageRequest.of(page, size, Sort.by(CREATED_AT).descending());
        Page<Map<String, Object>> pageResult = new org.springframework.data.domain.PageImpl<>(
                comments, pageable, comments.size()
        );
        PagedResponse<Map<String, Object>> response = PagedResponse.fromPage(
                pageResult,
                "Comments retrieved successfully",
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        return ResponseEntity.ok(response);
    }

    // Update sendMessage endpoint
    @PostMapping("/messages")
    @Operation(summary = "Send message", description = "Sends a message to a connected user")
    public ResponseEntity<ApiResponse<Message>> sendMessage(
            @Valid @RequestBody SendMessageRequest messageRequest,
            HttpServletRequest httpRequest) {
        /**
         * Send a message over an existing connection.
         *
         * @param messageRequest DTO containing connection id, sender id, and message content
         * @param httpRequest    current HTTP request used to resolve authentication and build response path
         * @return ResponseEntity with ApiResponse containing the created Message and HTTP 201
         */
        Long currentUserId = getCurrentUserId(httpRequest);
        if (messageRequest.getSenderId() != null && !messageRequest.getSenderId().equals(currentUserId)) {
            throw new IllegalArgumentException("senderId does not match authenticated user");
        }
        Message message = socialService.sendMessage(messageRequest.getConnectionId(), currentUserId, messageRequest.getContent());
        ApiResponse<Message> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Message sent successfully",
                message,
                httpRequest.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


}
