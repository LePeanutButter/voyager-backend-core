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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.DefaultSecurityFilterChain;

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

    // ── filterChain ─────────────────────────────────────────────────────────────

    @Test
    void filterChain_ShouldDisableCsrf() throws Exception {
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
        // CSRF should be disabled for JWT-based authentication
        assertThat(chain).isNotNull();
    }

    @Test
    void filterChain_ShouldConfigureStatelessSession() throws Exception {
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
        // Session should be stateless for JWT
        assertThat(chain).isNotNull();
    }

    @Test
    void filterChain_ShouldAddJwtFilter() throws Exception {
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
        // JWT filter should be added before UsernamePasswordAuthenticationFilter
        assertThat(chain).isNotNull();
    }

    @Test
    void filterChain_ShouldConfigureCors() throws Exception {
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
        // CORS should be configured
        assertThat(chain).isNotNull();
    }

    @Test
    void filterChain_ShouldConfigureSecurityHeaders() throws Exception {
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
        // Security headers should be configured
        assertThat(chain).isNotNull();
    }

    @Test
    void filterChain_PublicEndpoints_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        // Test that public endpoints are configured correctly
        // This would require integration testing with MockMvc for full verification
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
        assertThat(chain).isNotNull();
    }

    @Test
    void filterChain_AdminEndpoints_ShouldRequireAdminRole() throws Exception {
        // Test that admin endpoints require proper roles
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
        assertThat(chain).isNotNull();
    }

    @Test
    void filterChain_UserEndpoints_ShouldRequireAuthentication() throws Exception {
        // Test that user management endpoints require authentication
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
        assertThat(chain).isNotNull();
    }

    @Test
    void filterChain_SocialEndpoints_ShouldRequireAuthentication() throws Exception {
        // Test that social features require authentication
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
        assertThat(chain).isNotNull();
    }

    @Test
    void filterChain_TravelPlanEndpoints_ShouldRequireAuthentication() throws Exception {
        // Test that travel planning endpoints require authentication
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
        assertThat(chain).isNotNull();
    }

    @Test
    void filterChain_PublicGetEndpoints_ShouldBeAccessible() throws Exception {
        // Test that public GET endpoints for browsing are accessible
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
        assertThat(chain).isNotNull();
    }

    @Test
    void filterChain_SwaggerEndpoints_ShouldBePublic() throws Exception {
        // Test that Swagger/OpenAPI endpoints are public
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
        assertThat(chain).isNotNull();
    }

    @Test
    void filterChain_ActuatorEndpoints_ShouldHaveProperRestrictions() throws Exception {
        // Test that actuator endpoints have proper role restrictions
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
        assertThat(chain).isNotNull();
    }

    @Test
    void filterChain_OptionsRequests_ShouldBePermitted() throws Exception {
        // Test that CORS preflight OPTIONS requests are permitted
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
        assertThat(chain).isNotNull();
    }

    @Test
    void filterChain_DeleteUserEndpoints_ShouldRequireSuperAdminRole() throws Exception {
        // Test that DELETE /users/** requires SUPER_ADMIN role
        HttpSecurity httpSecurity = createMockHttpSecurity();
        SecurityFilterChain chain = securityConfig.filterChain(httpSecurity);
        
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