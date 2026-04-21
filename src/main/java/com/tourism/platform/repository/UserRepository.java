package com.tourism.platform.repository;

import com.tourism.platform.model.User;
import com.tourism.platform.model.UserRole;
import com.tourism.platform.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity operations
 * 
 * This interface provides methods for accessing and manipulating user data
 * in the database using Spring Data JPA.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     * 
     * @param username the username to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     * 
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username or email
     * 
     * @param username the username to search for
     * @param email the email to search for
     * @return Optional containing the user if found
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Check if username exists
     * 
     * @param username the username to check
     * @return true if username exists, false otherwise
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     * 
     * @param email the email to check
     * @return true if email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Find users by role
     * 
     * @param role the user role to filter by
     * @param pageable pagination information
     * @return Page of users with the specified role
     */
    Page<User> findByRole(UserRole role, Pageable pageable);

    /**
     * Find users by status
     * 
     * @param status the user status to filter by
     * @param pageable pagination information
     * @return Page of users with the specified status
     */
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    /**
     * Find users by role and status
     * 
     * @param role the user role to filter by
     * @param status the user status to filter by
     * @param pageable pagination information
     * @return Page of users with the specified role and status
     */
    Page<User> findByRoleAndStatus(UserRole role, UserStatus status, Pageable pageable);

    /**
     * Search users by first name or last name (case-insensitive)
     * 
     * @param searchTerm the search term to match against names
     * @param pageable pagination information
     * @return Page of users matching the search criteria
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> searchByName(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Count users by role
     * 
     * @param role the user role to count
     * @return number of users with the specified role
     */
    long countByRole(UserRole role);

    /**
     * Count users by status
     * 
     * @param status the user status to count
     * @return number of users with the specified status
     */
    long countByStatus(UserStatus status);

    /**
     * Find users created after a specific date
     * 
     * @param date the date to filter by
     * @param pageable pagination information
     * @return Page of users created after the specified date
     */
    Page<User> findByCreatedAtAfter(java.time.LocalDateTime date, Pageable pageable);
}
