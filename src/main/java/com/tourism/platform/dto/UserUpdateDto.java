package com.tourism.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user update requests
 * 
 * This DTO contains fields that can be updated by users.
 * All fields are optional to allow partial updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User update request DTO")
public class UserUpdateDto {

    @Schema(description = "First name", example = "John")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Schema(description = "Phone number", example = "+1234567890")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    @Schema(description = "Profile image URL")
    private String profileImageUrl;

    @Schema(description = "User bio", example = "Travel enthusiast exploring the world")
    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;
}
