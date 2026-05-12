package com.tourism.platform.dto;

/**
 * Request DTO used when registering a new user. Contains required fields such
 * as username, password and email along with optional profile details.
 */

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user registration requests
 * 
 * This DTO contains all necessary fields for user registration
 * with appropriate validation annotations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User registration request DTO")
public class UserRegistrationDto {

    @Schema(description = "Username", example = "john_doe")
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Schema(description = "Email address", example = "john.doe@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "Password", example = "SecurePass123!")
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @Schema(description = "First name", example = "John")
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Schema(description = "Phone number", example = "+1234567890")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;
}
