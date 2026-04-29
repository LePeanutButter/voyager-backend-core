package com.tourism.platform.controller;

import com.tourism.platform.dto.*;
import com.tourism.platform.model.Message;
import com.tourism.platform.model.User;
import com.tourism.platform.security.JwtTokenProvider;
import com.tourism.platform.service.SocialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
@Validated
public class SocialController {

    private final SocialService socialService;
    private final JwtTokenProvider tokenProvider;

    /** Prefer Spring Security principal (JWT filter loads {@link User} by username); fallback to userId claim if present. */
    private Long getCurrentUserId(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        String token = extractTokenFromRequest(request);
        if (token != null && tokenProvider.validateToken(token)) {
            Long userId = tokenProvider.getUserIdFromJWT(token);
            if (userId != null) {
                return userId;
            }
        }
        throw new IllegalArgumentException("Invalid or missing authentication token");
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // Traveler Connections
    @PostMapping("/connections")
    @Operation(summary = "Send connection request", description = "Sends a connection request to another traveler")
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
    public ResponseEntity<ApiResponse<List<TravelConnectionDto>>> getUserConnections(
            @Parameter(description = "User ID") @PathVariable Long userId,
            HttpServletRequest request) {

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

        // TODO: Get current user ID from security context
        Long currentUserId = 1L; // Placeholder - should get from authentication

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> createReview(
            @Valid @RequestBody Map<String, Object> reviewData,
            HttpServletRequest request) {
        
        Map<String, Object> responseData = Map.of(
                "reviewId", 1L,
                "targetId", reviewData.get("targetId"),
                "targetType", reviewData.get("targetType"),
                "rating", reviewData.get("rating"),
                "comment", reviewData.get("comment"),
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
    public ResponseEntity<PagedResponse<Map<String, Object>>> getReviews(
            @Parameter(description = "Target type (destination, service, activity)") @PathVariable String targetType,
            @Parameter(description = "Target ID") @PathVariable Long targetId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        List<Map<String, Object>> reviews = List.of(
                Map.of("id", 1L, "rating", 5, "comment", "Great experience!", "author", "John Doe"),
                Map.of("id", 2L, "rating", 4, "comment", "Amazing place!", "author", "Jane Smith"),
                Map.of("id", 3L, "rating", 5, "comment", "Would recommend!", "author", "Mike Johnson")
        );
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
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
        
        Map<String, Object> responseData = Map.of(
                "reviewId", reviewId,
                "rating", updateData.get("rating"),
                "comment", updateData.get("comment"),
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
        
        List<Map<String, Object>> conversations = List.of(
                Map.of("id", 1L, "participant", "John Doe", "lastMessage", "Hi there!", "unreadCount", 2),
                Map.of("id", 2L, "participant", "Jane Smith", "lastMessage", "See you soon!", "unreadCount", 0),
                Map.of("id", 3L, "participant", "Mike Johnson", "lastMessage", "Thanks!", "unreadCount", 1)
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
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size,
            HttpServletRequest request) {

        Page<Message> messagesPage = socialService.getConversationMessagesPaginated(connectionId, userId, page, size);

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

        // TODO: Get current user ID from security context
        Long currentUserId = 1L; // Placeholder - should get from authentication

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
        
        List<Map<String, Object>> posts = List.of(
                Map.of("id", 1L, "author", "John Doe", "content", "Traveling to Paris!", "type", "POST"),
                Map.of("id", 2L, "author", "Jane Smith", "content", "Sharing photos from Rome", "type", "PHOTO"),
                Map.of("id", 3L, "author", "Mike Johnson", "content", "Tokyo is amazing!", "type", "POST")
        );
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
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
        
        Map<String, Object> responseData = Map.of(
                "postId", 1L,
                "content", postData.get("content"),
                "type", postData.getOrDefault("type", "POST"),
                "createdAt", java.time.LocalDateTime.now()
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
        
        Map<String, Object> responseData = Map.of(
                "postId", postId,
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
        
        Map<String, Object> responseData = Map.of(
                "postId", postId,
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
        
        Map<String, Object> responseData = Map.of(
                "commentId", 1L,
                "postId", postId,
                "content", commentData.get("content"),
                "createdAt", java.time.LocalDateTime.now()
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
        
        List<Map<String, Object>> comments = List.of(
                Map.of("id", 1L, "author", "John Doe", "content", "Great post!", "createdAt", java.time.LocalDateTime.now()),
                Map.of("id", 2L, "author", "Jane Smith", "content", "Thanks for sharing!", "createdAt", java.time.LocalDateTime.now())
        );
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
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

        Message message = socialService.sendMessage(
                messageRequest.getConnectionId(),
                messageRequest.getSenderId(),
                messageRequest.getContent()
        );

        ApiResponse<Message> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Message sent successfully",
                message,
                httpRequest.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


}
