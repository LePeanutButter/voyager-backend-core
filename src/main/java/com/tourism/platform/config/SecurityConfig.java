package com.tourism.platform.config;

import com.tourism.platform.security.CustomUserDetailsService;
import com.tourism.platform.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.stream.Collectors;

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

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }


    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Password encoder bean for hashing passwords
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication provider bean that uses our custom UserDetailsService
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication manager bean for JWT authentication
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * JWT authentication filter bean
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parseAllowedOrigins(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "authorization", "content-type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // With server.servlet.context-path=/api/v1, Spring matches CORS paths relative to the app context.
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Main security filter chain configuration
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF as we're using JWT
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
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
                
                // Public GET endpoints for browsing
                .requestMatchers(HttpMethod.GET, "/api/v1/destinations/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/activities/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/services/**").permitAll()
                
                // User management endpoints - AUTHENTICATED
                .requestMatchers(HttpMethod.GET, "/users").authenticated()
                .requestMatchers(HttpMethod.GET, "/users/*").authenticated()
                .requestMatchers(HttpMethod.PUT, "/users/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/users/**").hasRole("SUPER_ADMIN")
                
                // Travel planning endpoints (authenticated)
                .requestMatchers("/travel-plans/**").authenticated()
                .requestMatchers("/reservations/**").authenticated()
                
                // Social features endpoints (authenticated)
                .requestMatchers("/connections/**").authenticated()
                .requestMatchers("/reviews/**").authenticated()
                .requestMatchers("/messages/**").authenticated()
                
                // Admin endpoints
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Add JWT filter
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private List<String> parseAllowedOrigins(String originsProperty) {
        return java.util.Arrays.stream(originsProperty.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .collect(Collectors.toList());
    }
}
