package com.tourism.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for login response containing JWT token and user information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    
    private String token;
    @Builder.Default
    private String tokenType = "Bearer";
    private Long expiresIn;
    private UserDto user;
    
    public static LoginResponseDto fromUserDto(String token, Long expiresIn, UserDto userDto) {
        return LoginResponseDto.builder()
                .token(token)
                .expiresIn(expiresIn)
                .user(userDto)
                .build();
    }
}
