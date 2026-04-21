package com.tourism.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object for user login requests
 * 
 * This DTO contains credentials required for user authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User login request DTO")
public class UserLoginDto {

    @Schema(description = "Username or email", example = "john_doe")
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @Schema(description = "Password", example = "SecurePass123!")
    @NotBlank(message = "Password is required")
    private String password;
}
