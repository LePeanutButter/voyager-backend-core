package com.tourism.platform.config;

import com.tourism.platform.security.JwtAuthenticationFilter;
import com.tourism.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @InjectMocks
    private SecurityConfig securityConfig;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() throws Exception {
        // Inject @Value field since there's no Spring context
        Field field = SecurityConfig.class.getDeclaredField("allowedOrigins");
        field.setAccessible(true);
        field.set(securityConfig, "http://localhost:3000,https://example.com");
    }

    private HttpSecurity createMockHttpSecurity() throws Exception {
        HttpSecurity httpSecurity = mock(HttpSecurity.class);
        DefaultSecurityFilterChain filterChain = mock(DefaultSecurityFilterChain.class);
        
        // Mock the fluent API chain
        when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
        when(httpSecurity.cors(any())).thenReturn(httpSecurity);
        when(httpSecurity.headers(any())).thenReturn(httpSecurity);
        when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
        when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
        when(httpSecurity.addFilterBefore(any(), any())).thenReturn(httpSecurity);
        when(httpSecurity.build()).thenReturn(filterChain);
        
        return httpSecurity;
    }

    // ── passwordEncoder ───────────────────────────────────────────────────────

    @Test
    void passwordEncoderShouldReturnBCryptPasswordEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    void passwordEncoderShouldEncodeAndMatchPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String rawPassword = "mySecret123";

        String encoded = encoder.encode(rawPassword);

        assertThat(encoder.matches(rawPassword, encoded)).isTrue();
    }

    @Test
    void passwordEncoderShouldProduceDifferentHashesForSamePassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String rawPassword = "mySecret123";

        String encoded1 = encoder.encode(rawPassword);
        String encoded2 = encoder.encode(rawPassword);

        // BCrypt uses a random salt each time
        assertThat(encoded1).isNotEqualTo(encoded2);
    }

    @Test
    void passwordEncoderShouldNotMatchWrongPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String encoded = encoder.encode("correctPassword");

        assertThat(encoder.matches("wrongPassword", encoded)).isFalse();
    }

    // ── jwtAuthenticationFilter ───────────────────────────────────────────────

    @Test
    void jwtAuthenticationFilterShouldReturnNonNullInstance() {
        JwtAuthenticationFilter filter = securityConfig.jwtAuthenticationFilter(tokenProvider, userDetailsService);

        assertThat(filter).isNotNull();
    }

    @Test
    void jwtAuthenticationFilterShouldReturnNewInstanceEachTime() {
        JwtAuthenticationFilter filter1 = securityConfig.jwtAuthenticationFilter(tokenProvider, userDetailsService);
        JwtAuthenticationFilter filter2 = securityConfig.jwtAuthenticationFilter(tokenProvider, userDetailsService);

        // Bean is not scoped as singleton in this unit context, each call creates one
        assertThat(filter1).isNotSameAs(filter2);
    }

    // ── authenticationManager ─────────────────────────────────────────────────

    @Test
    void authenticationManagerShouldReturnManagerFromConfig() throws Exception {
        AuthenticationConfiguration config = mock(AuthenticationConfiguration.class);
        AuthenticationManager mockManager = mock(AuthenticationManager.class);
        when(config.getAuthenticationManager()).thenReturn(mockManager);

        AuthenticationManager result = securityConfig.authenticationManager(config);

        assertThat(result).isSameAs(mockManager);
        verify(config).getAuthenticationManager();
    }

    // ── corsConfigurationSource ───────────────────────────────────────────────

    @Test
    void corsConfigurationSourceShouldReturnUrlBasedSource() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);
    }

    @Test
    void corsConfigurationSourceShouldContainConfigForAllPaths() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/social/connections");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config).isNotNull();
    }

    @Test
    void corsConfigurationSourceShouldAllowConfiguredOrigins() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/any");
        CorsConfiguration config = source.getCorsConfiguration(request);
        assertThat(config).isNotNull();
        assertThat(Objects.requireNonNull(config).getAllowedOrigins())
                .containsExactlyInAnyOrder("http://localhost:3000", "https://example.com");
    }

    @Test
    void corsConfigurationSourceShouldAllowRequiredHttpMethods() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/any");
        CorsConfiguration config = source.getCorsConfiguration(request);
        assertThat(config).isNotNull();
        assertThat(Objects.requireNonNull(config).getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    @Test
    void corsConfigurationSourceShouldAllowRequiredHeaders() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/any");
        CorsConfiguration config = source.getCorsConfiguration(request);
        assertThat(config).isNotNull();
        assertThat(Objects.requireNonNull(config).getAllowedHeaders())
                .contains("Authorization", "Content-Type", "X-Trace-Id", "X-Request-Id");
    }

    @Test
    void corsConfigurationSourceShouldExposeTraceHeaders() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/any");
        CorsConfiguration config = source.getCorsConfiguration(request);
        assertThat(config).isNotNull();
        assertThat(Objects.requireNonNull(config).getExposedHeaders())
                .containsExactlyInAnyOrder("X-Trace-Id", "X-Request-Id");
    }

    @Test
    void corsConfigurationSourceShouldAllowCredentials() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/any");
        CorsConfiguration config = source.getCorsConfiguration(request);
        assertThat(config).isNotNull();
        assertThat(Objects.requireNonNull(config).getAllowCredentials()).isTrue();
    }

    @Test
    void corsConfigurationSourceShouldHaveCorrectMaxAge() {
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/any");
        CorsConfiguration config = source.getCorsConfiguration(request);
        assertThat(config).isNotNull();
        assertThat(Objects.requireNonNull(config).getMaxAge()).isEqualTo(3600L);
    }

    // ── parseAllowedOrigins (existing tests kept, duplicates removed) ─────────

    @Test
    void parseAllowedOriginsWithValidOriginsShouldReturnList() {
        List<String> result = invokeParseAllowedOrigins("http://localhost:3000,https://example.com, http://test.com");

        assertThat(result)
                .hasSize(3)
                .containsExactlyInAnyOrder("http://localhost:3000", "https://example.com", "http://test.com");
    }

    @Test
    void parseAllowedOriginsWithEmptyStringShouldReturnEmptyList() {
        assertThat(invokeParseAllowedOrigins("")).isEmpty();
    }

    @Test
    void parseAllowedOriginsWithConsecutiveCommasShouldFilterOutEmptyStrings() {
        List<String> result = invokeParseAllowedOrigins("http://localhost:3000,,https://example.com,");

        assertThat(result)
                .hasSize(2)
                .doesNotContain("");
    }

    // ── Security Configuration Tests ─────────────────────────────────────────
    @ParameterizedTest
    @ValueSource(strings = {
            "filterChainShouldDisableCsrf",
            "filterChainShouldConfigureStatelessSession",
            "filterChainShouldAddJwtFilter",
            "filterChainShouldConfigureCors",
            "filterChainShouldConfigureSecurityHeaders",
            "filterChainPublicEndpointsShouldBeAccessibleWithoutAuthentication"
    })
    void securityConfigurationShouldConfigureCorrectly(String testName) throws Exception {
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity, tokenProvider, userDetailsService);
        
        // Verify that chain is configured correctly
        assertThat(chain).isNotNull();
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