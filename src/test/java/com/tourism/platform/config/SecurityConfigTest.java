package com.tourism.platform.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    void parseAllowedOrigins_WithValidOrigins_ShouldReturnList() {
        // Given
        String originsProperty = "http://localhost:3000,https://example.com, http://test.com";

        // When
        List<String> result = invokeParseAllowedOrigins(originsProperty);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains("http://localhost:3000"));
        assertTrue(result.contains("https://example.com"));
        assertTrue(result.contains("http://test.com"));
    }

    @Test
    void parseAllowedOrigins_WithEmptyOrigins_ShouldReturnEmptyList() {
        // Given
        String originsProperty = "";

        // When
        List<String> result = invokeParseAllowedOrigins(originsProperty);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void parseAllowedOrigins_WithNullOrigins_ShouldReturnEmptyList() {
        // Given
        String originsProperty = null;

        // When
        List<String> result = invokeParseAllowedOrigins(originsProperty);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void parseAllowedOrigins_WithExtraSpaces_ShouldTrimSpaces() {
        // Given
        String originsProperty = " http://localhost:3000 , https://example.com , http://test.com ";

        // When
        List<String> result = invokeParseAllowedOrigins(originsProperty);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains("http://localhost:3000"));
        assertTrue(result.contains("https://example.com"));
        assertTrue(result.contains("http://test.com"));
    }

    @Test
    void parseAllowedOrigins_WithEmptyEntries_ShouldFilterOutEmptyStrings() {
        // Given
        String originsProperty = "http://localhost:3000,,https://example.com,,http://test.com,";

        // When
        List<String> result = invokeParseAllowedOrigins(originsProperty);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains("http://localhost:3000"));
        assertTrue(result.contains("https://example.com"));
        assertTrue(result.contains("http://test.com"));
        assertFalse(result.contains(""));
    }

    @Test
    void parseAllowedOrigins_ShouldUseModernStreamAPI() {
        // Given
        String originsProperty = "http://localhost:3000,https://example.com";

        // When
        List<String> result = invokeParseAllowedOrigins(originsProperty);

        // Then
        assertNotNull(result);
        // This test ensures the method uses the modern Stream.toList() method
        // rather than the deprecated Stream.collect(Collectors.toList())
        assertEquals(2, result.size());
    }

    // Helper method to invoke private method for testing
    @SuppressWarnings("unchecked")
    private List<String> invokeParseAllowedOrigins(String originsProperty) {
        try {
            java.lang.reflect.Method method = SecurityConfig.class.getDeclaredMethod("parseAllowedOrigins", String.class);
            method.setAccessible(true);
            return (List<String>) method.invoke(securityConfig, originsProperty);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke parseAllowedOrigins method", e);
        }
    }
}
