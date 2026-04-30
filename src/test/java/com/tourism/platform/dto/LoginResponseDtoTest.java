package com.tourism.platform.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginResponseDtoTest {

    @Test
    void testLoginResponseDto() {
        LoginResponseDto dto = new LoginResponseDto();
        
        // Test getters and setters
        dto.setToken("test-token");
        dto.setTokenType("Bearer");
        dto.setExpiresIn(3600L);
        
        assertEquals("test-token", dto.getToken());
        assertEquals("Bearer", dto.getTokenType());
        assertEquals(3600L, dto.getExpiresIn());
    }

    @Test
    void testLoginResponseDtoConstructor() {
        // Test that the class can be instantiated
        LoginResponseDto dto = new LoginResponseDto();
        assertNotNull(dto);
    }

    @Test
    void fromUserDto_FillsTokenAndUser() {
        UserDto user = UserDto.builder()
                .id(3L)
                .username("alice")
                .email("a@x.com")
                .build();

        LoginResponseDto dto = LoginResponseDto.fromUserDto("tok", 7200L, user);

        assertEquals("tok", dto.getToken());
        assertEquals("Bearer", dto.getTokenType());
        assertEquals(7200L, dto.getExpiresIn());
        assertSame(user, dto.getUser());
    }
}
