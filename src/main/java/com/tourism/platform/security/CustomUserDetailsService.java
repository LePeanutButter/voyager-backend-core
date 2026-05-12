package com.tourism.platform.security;

import com.tourism.platform.model.User;
import com.tourism.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom implementation of UserDetailsService for Spring Security
 * 
 * This service loads user-specific data from the database and provides
 * it to Spring Security for authentication and authorization.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        log.debug("Loading user by username or email: {}", usernameOrEmail);
        
        // Try to find user by username first, then by email
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> {
                    log.warn("User not found with username or email: {}", usernameOrEmail);
                    return new UsernameNotFoundException("User not found with username or email: " + usernameOrEmail);
                });

        log.debug("User found: {}", user.getUsername());
        return user;
    }

    /**
     * Load user by ID (additional helper method)
     * 
     * @param userId the user ID
     * @return UserDetails object
     * @throws UsernameNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(@NonNull Long userId) throws UsernameNotFoundException {
        log.debug("Loading user by ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", userId);
                    return new UsernameNotFoundException("User not found with ID: " + userId);
                });

        log.debug("User found: {}", user.getUsername());
        return user;
    }
}
