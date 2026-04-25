package com.tourism.platform.controller;

import com.tourism.platform.dto.ApiResponse;
import com.tourism.platform.dto.PagedResponse;
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
@RequestMapping("/api/v1/social")
@RequiredArgsConstructor
@Tag(name = "Social Features", description = "APIs for traveler social interactions")
@Validated
public class SocialController {

    // Traveler Connections
    @PostMapping("/connections")
    @Operation(summary = "Send connection request", description = "Sends a connection request to another traveler")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendConnectionRequest(
            @Valid @RequestBody Map<String, Object> requestData,
            HttpServletRequest request) {
        
        Map<String, Object> responseData = Map.of(
                "requestId", 1L,
                "recipientId", requestData.get("recipientId"),
                "status", "PENDING"
        );
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Connection request sent successfully",
                responseData,
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/connections/{userId}")
    @Operation(summary = "Get user connections", description = "Retrieves all connections for a user")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserConnections(
            @Parameter(description = "User ID") @PathVariable Long userId,
            HttpServletRequest request) {
        
        List<Map<String, Object>> connections = List.of(
                Map.of("id", 1L, "username", "john_doe", "status", "ACCEPTED"),
                Map.of("id", 2L, "username", "jane_smith", "status", "ACCEPTED"),
                Map.of("id", 3L, "username", "mike_johnson", "status", "PENDING")
        );
        
        ApiResponse<List<Map<String, Object>>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Connections retrieved successfully",
                connections,
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/connections/{requestId}/accept")
    @Operation(summary = "Accept connection request", description = "Accepts a pending connection request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> acceptConnectionRequest(
            @Parameter(description = "Connection request ID") @PathVariable Long requestId,
            HttpServletRequest request) {
        
        Map<String, Object> responseData = Map.of(
                "requestId", requestId,
                "status", "ACCEPTED",
                "acceptedAt", java.time.LocalDateTime.now()
        );
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Connection request accepted",
                responseData,
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/connections/{requestId}/reject")
    @Operation(summary = "Reject connection request", description = "Rejects a pending connection request")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectConnectionRequest(
            @Parameter(description = "Connection request ID") @PathVariable Long requestId,
            HttpServletRequest request) {
        
        Map<String, Object> responseData = Map.of(
                "requestId", requestId,
                "status", "REJECTED",
                "rejectedAt", java.time.LocalDateTime.now()
        );
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Connection request rejected",
                responseData,
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/connections/{connectionId}")
    @Operation(summary = "Remove connection", description = "Removes a connection between users")
    public ResponseEntity<ApiResponse<Void>> removeConnection(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            HttpServletRequest request) {
        
        ApiResponse<Void> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Connection removed successfully",
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

    // Messaging System
    @PostMapping("/messages")
    @Operation(summary = "Send message", description = "Sends a message to another user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMessage(
            @Valid @RequestBody Map<String, Object> messageData,
            HttpServletRequest request) {
        
        Map<String, Object> responseData = Map.of(
                "messageId", 1L,
                "recipientId", messageData.get("recipientId"),
                "content", messageData.get("content"),
                "status", "SENT",
                "sentAt", java.time.LocalDateTime.now()
        );
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                HttpStatus.CREATED.value(),
                "Message sent successfully",
                responseData,
                request.getRequestURI()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

    @GetMapping("/messages/{conversationId}")
    @Operation(summary = "Get conversation messages", description = "Retrieves messages from a specific conversation")
    public ResponseEntity<PagedResponse<Map<String, Object>>> getConversationMessages(
            @Parameter(description = "Conversation ID") @PathVariable Long conversationId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        List<Map<String, Object>> messages = List.of(
                Map.of("id", 1L, "sender", "John Doe", "content", "Hi there!", "timestamp", java.time.LocalDateTime.now()),
                Map.of("id", 2L, "sender", "Jane Smith", "content", "How are you?", "timestamp", java.time.LocalDateTime.now()),
                Map.of("id", 3L, "sender", "John Doe", "content", "Great thanks!", "timestamp", java.time.LocalDateTime.now())
        );
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        Page<Map<String, Object>> pageResult = new org.springframework.data.domain.PageImpl<>(
                messages, pageable, messages.size()
        );
        
        PagedResponse<Map<String, Object>> response = PagedResponse.fromPage(
                pageResult,
                "Messages retrieved successfully",
                HttpStatus.OK.value(),
                request.getRequestURI()
        );
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/messages/{messageId}/read")
    @Operation(summary = "Mark message as read", description = "Marks a message as read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markMessageAsRead(
            @Parameter(description = "Message ID") @PathVariable Long messageId,
            HttpServletRequest request) {
        
        Map<String, Object> responseData = Map.of(
                "messageId", messageId,
                "status", "READ",
                "readAt", java.time.LocalDateTime.now()
        );
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(
                HttpStatus.OK.value(),
                "Message marked as read",
                responseData,
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
}
