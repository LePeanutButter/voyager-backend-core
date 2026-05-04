package com.tourism.platform.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

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
     *
     * @return a {@link CorsConfigurationSource} exposing allowed origins, headers and methods
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parseAllowedOrigins(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Trace-Id", "X-Request-Id"));
        configuration.setExposedHeaders(List.of("X-Trace-Id", "X-Request-Id"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
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

    /**
     * Parse the configured allowed origins property into a list of origins.
     *
     * @param originsProperty comma-separated allowed origins property from configuration
     * @return list of trimmed, non-empty origin strings
     */
    private List<String> parseAllowedOrigins(String originsProperty) {
        return java.util.Arrays.stream(originsProperty.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }
}
