package com.tourism.platform.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private static final String TEST_SECRET = "testSecretKeyThatIsLongEnoughForHS512AlgorithmAndMeetsRequirements";
    private static final int JWT_EXPIRATION_IN_MS = 3_600_000; // 1 hour
    private static final int JWT_REFRESH_EXPIRATION_IN_MS = 7_200_000; // 2 hours

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(Objects.requireNonNull(jwtTokenProvider), "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(Objects.requireNonNull(jwtTokenProvider), "jwtExpirationInMs", JWT_EXPIRATION_IN_MS);
        ReflectionTestUtils.setField(Objects.requireNonNull(jwtTokenProvider), "jwtRefreshExpirationInMs", JWT_REFRESH_EXPIRATION_IN_MS);
    }

    @Test
    void generateTokenFromUsername_ShouldGenerateValidToken() {
        // Given
        String username = "testuser";

        // When
        String token = jwtTokenProvider.generateTokenFromUsername(username);

        // Then
        assertNotNull(token);
        assertEquals(username, jwtTokenProvider.getUsernameFromJWT(token));
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void generateTokenFromUsernameAndUserId_ShouldGenerateValidTokenWithUserId() {
        // Given
        String username = "testuser";
        Long userId = 123L;

        // When
        String token = jwtTokenProvider.generateTokenFromUsernameAndUserId(username, userId);

        // Then
        assertNotNull(token);
        assertEquals(username, jwtTokenProvider.getUsernameFromJWT(token));
        assertEquals(userId, jwtTokenProvider.getUserIdFromJWT(token));
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void generateRefreshToken_ShouldGenerateValidRefreshToken() {
        // Given
        String username = "testuser";

        // When
        String refreshToken = jwtTokenProvider.generateRefreshToken(username);

        // Then
        assertNotNull(refreshToken);
        assertEquals(username, jwtTokenProvider.getUsernameFromJWT(refreshToken));
        assertTrue(jwtTokenProvider.validateToken(refreshToken));
    }

    @Test
    void getUsernameFromJWT_WithValidToken_ShouldReturnUsername() {
        // Given
        String username = "testuser";
        String token = jwtTokenProvider.generateTokenFromUsername(username);

        // When
        String extractedUsername = jwtTokenProvider.getUsernameFromJWT(token);

        // Then
        assertEquals(username, extractedUsername);
    }

    @Test
    void getUserIdFromJWT_WithValidToken_ShouldReturnUserId() {
        // Given
        String username = "testuser";
        Long userId = 123L;
        String token = jwtTokenProvider.generateTokenFromUsernameAndUserId(username, userId);

        // When
        Long extractedUserId = jwtTokenProvider.getUserIdFromJWT(token);

        // Then
        assertEquals(userId, extractedUserId);
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        // Given
        String token = jwtTokenProvider.generateTokenFromUsername("testuser");

        // When
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Then
        assertTrue(isValid);
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnFalse() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_WithExpiredToken_ShouldReturnFalse() {
        // Given
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortLivedProvider, "jwtSecret", TEST_SECRET);
        ReflectionTestUtils.setField(shortLivedProvider, "jwtExpirationInMs", -1000); // Expired
        String expiredToken = shortLivedProvider.generateTokenFromUsername("testuser");

        // When
        boolean isValid = jwtTokenProvider.validateToken(expiredToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void validateToken_WithMalformedToken_ShouldReturnFalse() {
        // Given
        String malformedToken = "not.a.valid.jwt";

        // When
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void getTokenExpiration_WithValidToken_ShouldReturnExpirationDate() {
        // Given
        String token = jwtTokenProvider.generateTokenFromUsername("testuser");
        Date beforeExpiration = new Date(System.currentTimeMillis() + JWT_EXPIRATION_IN_MS - 1000);

        // When
        Date expirationDate = jwtTokenProvider.getTokenExpiration(token);
        Date afterExpiration = new Date(System.currentTimeMillis() + JWT_EXPIRATION_IN_MS + 1000);

        // Then
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(beforeExpiration));
        assertTrue(expirationDate.before(afterExpiration));
    }

    @Test
    void getUsernameFromJWT_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "invalid.token";

        // When & Then
        assertThrows(JwtException.class, () -> jwtTokenProvider.getUsernameFromJWT(invalidToken));
    }

    @Test
    void getUserIdFromJWT_WithTokenWithoutUserId_ShouldReturnNull() {
        // Given
        String token = jwtTokenProvider.generateTokenFromUsername("testuser"); // No userId in this token

        // When
        Long userId = jwtTokenProvider.getUserIdFromJWT(token);

        // Then
        assertNull(userId);
    }

    @Test
    void getTokenExpiration_WithInvalidToken_ShouldThrowException() {
        // Given
        String invalidToken = "invalid.token";

        // When & Then
        assertThrows(JwtException.class, () -> jwtTokenProvider.getTokenExpiration(invalidToken));
    }

    @Test
    void generateTokenFromUsername_ShouldCreateTokenWithCorrectExpiration() {
        // Given
        String username = "testuser";
        long beforeGeneration = System.currentTimeMillis();

        // When
        String token = jwtTokenProvider.generateTokenFromUsername(username);

        // Then
        Date expiration = jwtTokenProvider.getTokenExpiration(token);
        long afterGeneration = System.currentTimeMillis();
        
        assertNotNull(expiration);
        long expectedMinExpiration = beforeGeneration + JWT_EXPIRATION_IN_MS;
        long expectedMaxExpiration = afterGeneration + JWT_EXPIRATION_IN_MS;
        
        assertTrue(expiration.getTime() >= expectedMinExpiration - 1000); // Allow 1s tolerance
        assertTrue(expiration.getTime() <= expectedMaxExpiration + 1000); // Allow 1s tolerance
    }

    @Test
    void generateRefreshToken_ShouldCreateTokenWithRefreshExpiration() {
        // Given
        String username = "testuser";
        long beforeGeneration = System.currentTimeMillis();

        // When
        String refreshToken = jwtTokenProvider.generateRefreshToken(username);

        // Then
        Date expiration = jwtTokenProvider.getTokenExpiration(refreshToken);
        long afterGeneration = System.currentTimeMillis();
        
        assertNotNull(expiration);
        long expectedMinExpiration = beforeGeneration + JWT_REFRESH_EXPIRATION_IN_MS;
        long expectedMaxExpiration = afterGeneration + JWT_REFRESH_EXPIRATION_IN_MS;
        
        assertTrue(expiration.getTime() >= expectedMinExpiration - 1000); // Allow 1s tolerance
        assertTrue(expiration.getTime() <= expectedMaxExpiration + 1000); // Allow 1s tolerance
    }
}
