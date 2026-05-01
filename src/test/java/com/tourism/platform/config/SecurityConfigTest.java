package com.tourism.platform.config;

import com.tourism.platform.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @InjectMocks
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() throws Exception {
        // Inject @Value field since there's no Spring context
        Field field = SecurityConfig.class.getDeclaredField("allowedOrigins");
        field.setAccessible(true);
        field.set(securityConfig, "http://localhost:3000,https://example.com");
    }

    // ── passwordEncoder ───────────────────────────────────────────────────────

    @Test
    void passwordEncoder_ShouldReturnBCryptPasswordEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoder_ShouldEncodeAndMatchPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String rawPassword = "mySecret123";

        String encoded = encoder.encode(rawPassword);

        assertThat(encoder.matches(rawPassword, encoded)).isTrue();
    }

    @Test
    void passwordEncoder_ShouldProduceDifferentHashesForSamePassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String rawPassword = "mySecret123";

        String encoded1 = encoder.encode(rawPassword);
        String encoded2 = encoder.encode(rawPassword);

        // BCrypt uses a random salt each time
        assertThat(encoded1).isNotEqualTo(encoded2);
    }

    @Test
    void passwordEncoder_ShouldNotMatchWrongPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String encoded = encoder.encode("correctPassword");

        assertThat(encoder.matches("wrongPassword", encoded)).isFalse();
    }

    // ── jwtAuthenticationFilter ───────────────────────────────────────────────

    @Test
    void jwtAuthenticationFilter_ShouldReturnNonNullInstance() {
        JwtAuthenticationFilter filter = securityConfig.jwtAuthenticationFilter();

        assertThat(filter).isNotNull();
    }

    @Test
    void jwtAuthenticationFilter_ShouldReturnNewInstanceEachTime() {
        JwtAuthenticationFilter filter1 = securityConfig.jwtAuthenticationFilter();
        JwtAuthenticationFilter filter2 = securityConfig.jwtAuthenticationFilter();

        // Bean is not scoped as singleton in this unit context, each call creates one
        assertThat(filter1).isNotSameAs(filter2);
    }

    // ── authenticationManager ─────────────────────────────────────────────────

    @Test
    void authenticationManager_ShouldReturnManagerFromConfig() throws Exception {
        AuthenticationConfiguration config = mock(AuthenticationConfiguration.class);
        AuthenticationManager mockManager = mock(AuthenticationManager.class);
        when(config.getAuthenticationManager()).thenReturn(mockManager);

        AuthenticationManager result = securityConfig.authenticationManager(config);

        assertThat(result).isSameAs(mockManager);
        verify(config).getAuthenticationManager();
    }

    // ── corsConfigurationSource ───────────────────────────────────────────────

    @Test
    void corsConfigurationSource_ShouldReturnUrlBasedSource() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);
    }

    @Test
    void corsConfigurationSource_ShouldContainConfigForAllPaths() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/social/connections");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config).isNotNull();
    }

    @Test
    void corsConfigurationSource_ShouldAllowConfiguredOrigins() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/any");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config.getAllowedOrigins())
                .containsExactlyInAnyOrder("http://localhost:3000", "https://example.com");
    }

    @Test
    void corsConfigurationSource_ShouldAllowRequiredHttpMethods() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/any");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    @Test
    void corsConfigurationSource_ShouldAllowRequiredHeaders() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/any");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config.getAllowedHeaders())
                .contains("Authorization", "Content-Type", "X-Trace-Id", "X-Request-Id");
    }

    @Test
    void corsConfigurationSource_ShouldExposeTraceHeaders() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/any");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config.getExposedHeaders())
                .containsExactlyInAnyOrder("X-Trace-Id", "X-Request-Id");
    }

    @Test
    void corsConfigurationSource_ShouldAllowCredentials() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/any");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config.getAllowCredentials()).isTrue();
    }

    @Test
    void corsConfigurationSource_ShouldHaveCorrectMaxAge() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/any");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    // ── parseAllowedOrigins (existing tests kept, duplicates removed) ─────────

    @Test
    void parseAllowedOrigins_WithValidOrigins_ShouldReturnList() {
        List<String> result = invokeParseAllowedOrigins("http://localhost:3000,https://example.com, http://test.com");

        assertThat(result).hasSize(3)
                .contains("http://localhost:3000", "https://example.com", "http://test.com");
    }

    @Test
    void parseAllowedOrigins_WithEmptyString_ShouldReturnEmptyList() {
        assertThat(invokeParseAllowedOrigins("")).isEmpty();
    }

    @Test
    void parseAllowedOrigins_WithExtraSpaces_ShouldTrimSpaces() {
        List<String> result = invokeParseAllowedOrigins(" http://localhost:3000 , https://example.com ");

        assertThat(result).containsExactlyInAnyOrder("http://localhost:3000", "https://example.com");
    }

    @Test
    void parseAllowedOrigins_WithConsecutiveCommas_ShouldFilterOutEmptyStrings() {
        List<String> result = invokeParseAllowedOrigins("http://localhost:3000,,https://example.com,");

        assertThat(result)
                .hasSize(2)
                .doesNotContain("");
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeParseAllowedOrigins(String originsProperty) {
        try {
            java.lang.reflect.Method method = SecurityConfig.class
                    .getDeclaredMethod("parseAllowedOrigins", String.class);
            method.setAccessible(true);
            return (List<String>) method.invoke(securityConfig, originsProperty);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke parseAllowedOrigins", e);
        }
    }
}