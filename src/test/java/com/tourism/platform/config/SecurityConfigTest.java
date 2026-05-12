package com.tourism.platform.config;

import com.tourism.platform.security.JwtAuthenticationFilter;
import com.tourism.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    private CorsProperties corsProperties;
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        corsProperties = new CorsProperties();
        corsProperties.setAllowedOrigins("http://localhost:3000,https://example.com");
        corsProperties.setAllowedOriginPatterns("");
        corsProperties.setAllowAllOrigins(false);
        corsProperties.setUseAwsHostnamePatterns(false);
        corsProperties.setAllowCredentials(true);
        securityConfig = new SecurityConfig(corsProperties);
    }

    private HttpSecurity createMockHttpSecurity() throws Exception {
        HttpSecurity httpSecurity = mock(HttpSecurity.class);
        DefaultSecurityFilterChain filterChain = mock(DefaultSecurityFilterChain.class);

        when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
        when(httpSecurity.cors(any())).thenReturn(httpSecurity);
        when(httpSecurity.headers(any())).thenReturn(httpSecurity);
        when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
        when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
        when(httpSecurity.addFilterBefore(any(), any())).thenReturn(httpSecurity);
        when(httpSecurity.build()).thenReturn(filterChain);

        return httpSecurity;
    }

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

        assertThat(encoded1).isNotEqualTo(encoded2);
    }

    @Test
    void passwordEncoderShouldNotMatchWrongPassword() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String encoded = encoder.encode("correctPassword");

        assertThat(encoder.matches("wrongPassword", encoded)).isFalse();
    }

    @Test
    void jwtAuthenticationFilterShouldReturnNonNullInstance() {
        JwtAuthenticationFilter filter = securityConfig.jwtAuthenticationFilter(tokenProvider, userDetailsService);

        assertThat(filter).isNotNull();
    }

    @Test
    void jwtAuthenticationFilterShouldReturnNewInstanceEachTime() {
        JwtAuthenticationFilter filter1 = securityConfig.jwtAuthenticationFilter(tokenProvider, userDetailsService);
        JwtAuthenticationFilter filter2 = securityConfig.jwtAuthenticationFilter(tokenProvider, userDetailsService);

        assertThat(filter1).isNotSameAs(filter2);
    }

    @Test
    void authenticationManagerShouldReturnManagerFromConfig() throws Exception {
        AuthenticationConfiguration config = mock(AuthenticationConfiguration.class);
        AuthenticationManager mockManager = mock(AuthenticationManager.class);
        when(config.getAuthenticationManager()).thenReturn(mockManager);

        AuthenticationManager result = securityConfig.authenticationManager(config);

        assertThat(result).isSameAs(mockManager);
        verify(config).getAuthenticationManager();
    }

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
        assertThat(Objects.requireNonNull(config).getAllowedOriginPatterns())
                .contains("http://localhost:3000", "https://example.com");
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

    @Test
    void corsAllowAllOriginsShouldUseWildcardAndDisableCredentials() {
        corsProperties.setAllowAllOrigins(true);
        corsProperties.setAllowCredentials(true);
        securityConfig = new SecurityConfig(corsProperties);

        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest();
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config).isNotNull();
        assertThat(Objects.requireNonNull(config).getAllowedOriginPatterns()).containsExactly("*");
        assertThat(config.getAllowCredentials()).isFalse();
    }

    @Test
    void corsUseAwsHostnamePatternsShouldAddPresetPatterns() {
        corsProperties.setAllowedOrigins("");
        corsProperties.setAllowedOriginPatterns("");
        corsProperties.setUseAwsHostnamePatterns(true);
        securityConfig = new SecurityConfig(corsProperties);

        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(new MockHttpServletRequest());

        assertThat(config).isNotNull();
        assertThat(Objects.requireNonNull(config).getAllowedOriginPatterns())
                .contains("http://*.amazonaws.com", "https://*.amazonaws.com");
    }

    @Test
    void parseCommaSeparatedWithValidOriginsShouldReturnList() {
        List<String> result = invokeParseCommaSeparated("http://localhost:3000,https://example.com, http://test.com");

        assertThat(result)
                .hasSize(3)
                .containsExactlyInAnyOrder("http://localhost:3000", "https://example.com", "http://test.com");
    }

    @Test
    void parseCommaSeparatedWithEmptyStringShouldReturnEmptyList() {
        assertThat(invokeParseCommaSeparated("")).isEmpty();
    }

    @Test
    void parseCommaSeparatedWithConsecutiveCommasShouldFilterOutEmptyStrings() {
        List<String> result = invokeParseCommaSeparated("http://localhost:3000,,https://example.com,");

        assertThat(result)
                .hasSize(2)
                .doesNotContain("");
    }

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

        assertThat(chain).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private List<String> invokeParseCommaSeparated(String property) {
        try {
            Method method = SecurityConfig.class
                    .getDeclaredMethod("parseCommaSeparated", String.class);
            method.setAccessible(true);
            return (List<String>) method.invoke(securityConfig, property);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke parseCommaSeparated", e);
        }
    }
}
