package com.tourism.platform.dto;

import com.tourism.platform.model.UserRole;
import com.tourism.platform.model.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Data Transfer Object for User responses
 * 
 * This DTO is used to transfer user data without exposing sensitive information
 * like passwords to external clients.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User response DTO")
public class UserDto {

    @Schema(description = "Unique user identifier")
    private Long id;

    @Schema(description = "Username")
    private String username;

    @Schema(description = "User email address")
    private String email;

    @Schema(description = "User first name")
    private String firstName;

    @Schema(description = "User last name")
    private String lastName;

    @Schema(description = "User phone number")
    private String phoneNumber;

    @Schema(description = "User role")
    private UserRole role;

    @Schema(description = "User account status")
    private UserStatus status;

    @Schema(description = "Profile image URL")
    private String profileImageUrl;

    @Schema(description = "User bio")
    private String bio;

    @Schema(description = "User travel interests")
    private Set<String> interests;

    @Schema(description = "User date of birth")
    private LocalDateTime dateOfBirth;

    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "JWT token for authenticated sessions")
    private String token;
}
