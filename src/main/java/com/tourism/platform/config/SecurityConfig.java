package com.tourism.platform.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.tourism.platform.security.JwtAuthenticationFilter;
import com.tourism.platform.security.JwtTokenProvider;

/**
 * Security Configuration for the Tourism Platform
 * 
 * This class configures Spring Security for JWT-based authentication,
 * CORS settings, and endpoint security rules.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // Constants for duplicated literals
    private static final String USERS_ENDPOINT = "/users/**";

    /**
     * Patterns that match common AWS frontends (ALB, CloudFront, S3 static website, execute-api)
     * without opening CORS to arbitrary non-AWS domains.
     */
    private static final List<String> AWS_HOSTNAME_LAB_PATTERNS = List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "http://*.amazonaws.com",
            "https://*.amazonaws.com"
    );

    private final CorsProperties corsProperties;

    public SecurityConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    /**
     * Password encoder bean for hashing and verifying user passwords.
     *
     * @return a {@link PasswordEncoder} instance suitable for encoding passwords
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expose the {@link AuthenticationManager} from the provided configuration.
     *
     * @param config Spring {@link AuthenticationConfiguration} used to obtain the manager
     * @return the resolved AuthenticationManager
     * @throws Exception if the authentication manager cannot be created
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Create the JWT authentication filter responsible for extracting and validating
     * JWT tokens from incoming requests.
     *
     * @param tokenProvider       provider responsible for token validation and claims extraction
     * @param userDetailsService  service used to load user details for authentication context
     * @return configured {@link JwtAuthenticationFilter} instance
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                                           UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(tokenProvider, userDetailsService);
    }

    /**
     * Build the CORS configuration used by the application.
     * <p>
     * When {@link CorsProperties#isAllowAllOrigins()} is true, wildcard origins are used only with
     * {@code allowCredentials(false)} (required by the CORS model for {@code *}); production should keep
     * {@code allow-all-origins} disabled and rely on explicit origin patterns.
     *
     * @return a {@link CorsConfigurationSource} exposing allowed origins, headers and methods
     */
    @Bean
    @SuppressWarnings("java:S5122") // reviewed: wildcard branch pairs * with credentials off; else explicit patterns
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Trace-Id", "X-Request-Id"));
        configuration.setExposedHeaders(List.of("X-Trace-Id", "X-Request-Id"));
        configuration.setMaxAge(3600L);

        if (corsProperties.isAllowAllOrigins()) {
            configuration.setAllowedOriginPatterns(List.of("*"));
            configuration.setAllowCredentials(false);
        } else {
            Set<String> patterns = new LinkedHashSet<>();
            patterns.addAll(parseCommaSeparated(corsProperties.getAllowedOriginPatterns()));
            if (corsProperties.isUseAwsHostnamePatterns()) {
                patterns.addAll(AWS_HOSTNAME_LAB_PATTERNS);
            }
            // Exact URLs from allowed-origins also work as origin patterns (same matching for normal hosts).
            patterns.addAll(parseCommaSeparated(corsProperties.getAllowedOrigins()));
            if (!patterns.isEmpty()) {
                configuration.setAllowedOriginPatterns(new ArrayList<>(patterns));
                configuration.setAllowCredentials(corsProperties.isAllowCredentials());
            }
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // With server.servlet.context-path=/api/v1, Spring matches CORS paths relative to the app context.
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Configure the main {@link SecurityFilterChain} for HTTP security.
     *
     * @param http               {@link HttpSecurity} builder provided by Spring Security
     * @param tokenProvider      component used to validate JWT tokens
     * @param userDetailsService service used to load user details for authentication
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if an error occurs while building the security chain
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtTokenProvider tokenProvider,
                                           UserDetailsService userDetailsService) throws Exception {
        http
            // Disable CSRF as we're using JWT
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers
                    .contentTypeOptions(contentType -> {})
                    .frameOptions(frame -> frame.sameOrigin())
                    .xssProtection(Customizer.withDefaults()))
            
            // Configure session management to stateless
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // CORS preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Public endpoints - MOST SPECIFIC FIRST
                .requestMatchers(HttpMethod.POST, "/users").permitAll()
                .requestMatchers("/users/register").permitAll()
                .requestMatchers("/users/login").permitAll()
                .requestMatchers("/users/check-username").permitAll()
                .requestMatchers("/users/check-email").permitAll()
                .requestMatchers("/auth/**").permitAll()
                
                // Swagger/OpenAPI endpoints
                .requestMatchers("/api-docs/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                
                // Actuator endpoints (restricted)
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/actuator/metrics").hasRole("ADMIN")
                
                // Public GET endpoints for browsing (paths are servlet-relative; context-path is /api/v1)
                .requestMatchers(HttpMethod.GET, "/destinations/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/activities/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/services/**").permitAll()
                
                // Travel catalog (Amadeus) — JWT required (protects quotas and hides provider keys)
                .requestMatchers("/catalog/**").authenticated()

                // Travel planning endpoints (authenticated)
                .requestMatchers("/travel-plans/**").authenticated()
                
                // Social features (SocialController lives under /social)
                .requestMatchers("/social/**").authenticated()

                // User management endpoints
                .requestMatchers(HttpMethod.GET, USERS_ENDPOINT).authenticated()
                .requestMatchers(HttpMethod.PUT, USERS_ENDPOINT).authenticated()
                .requestMatchers(HttpMethod.DELETE, USERS_ENDPOINT).hasRole("SUPER_ADMIN")
                
                // Admin endpoints
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            .addFilterBefore(jwtAuthenticationFilter(tokenProvider, userDetailsService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private List<String> parseCommaSeparated(String property) {
        if (property == null || property.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(property.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
