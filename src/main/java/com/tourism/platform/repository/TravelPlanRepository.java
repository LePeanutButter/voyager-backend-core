package com.tourism.platform.repository;

import com.tourism.platform.model.TravelPlan;
import com.tourism.platform.model.TravelPlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for TravelPlan entity operations
 * 
 * This interface provides methods for accessing and manipulating travel plan data
 * in the database using Spring Data JPA.
 */
@Repository
public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    /**
     * Find travel plans by user
     * 
     * @param userId the user ID to filter by
     * @param pageable pagination information
     * @return Page of travel plans for the specified user
     */
    Page<TravelPlan> findByUserId(Long userId, Pageable pageable);

    /**
     * Find travel plans by status
     * 
     * @param status the travel plan status to filter by
     * @param pageable pagination information
     * @return Page of travel plans with the specified status
     */
    Page<TravelPlan> findByStatus(TravelPlanStatus status, Pageable pageable);

    /**
     * Find travel plans by user and status
     * 
     * @param userId the user ID to filter by
     * @param status the travel plan status to filter by
     * @param pageable pagination information
     * @return Page of travel plans for the specified user and status
     */
    Page<TravelPlan> findByUserIdAndStatus(Long userId, TravelPlanStatus status, Pageable pageable);

    /**
     * Find travel plans by destination location (case-insensitive)
     * 
     * @param destinationLocation the destination location to search for
     * @param pageable pagination information
     * @return Page of travel plans matching the destination
     */
    @Query("SELECT tp FROM TravelPlan tp WHERE " +
           "LOWER(tp.destinationLocation) LIKE LOWER(CONCAT('%', :destinationLocation, '%'))")
    Page<TravelPlan> findByDestinationLocationContaining(@Param("destinationLocation") String destinationLocation, Pageable pageable);

    /**
     * Find compatible travelers based on destination and date overlap
     * 
     * This query finds travel plans from other users that:
     * 1. Have the same destination location
     * 2. Have overlapping dates with the reference travel plan
     * 3. Are in ACTIVE status
     * 4. Belong to different users (exclude the requesting user)
     * 
     * @param destinationLocation the destination location to match
     * @param startDate the start date of the reference travel plan
     * @param endDate the end date of the reference travel plan
     * @param excludeUserId the user ID to exclude (the requesting user)
     * @return List of compatible travel plans with other users
     */
    @Query("SELECT tp FROM TravelPlan tp WHERE " +
           "LOWER(tp.destinationLocation) = LOWER(:destinationLocation) AND " +
           "tp.status = 'ACTIVE' AND " +
           "tp.user.id != :excludeUserId AND " +
           "tp.startDate <= :endDate AND " +
           "tp.endDate >= :startDate")
    List<TravelPlan> findCompatibleTravelPlans(@Param("destinationLocation") String destinationLocation,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              @Param("excludeUserId") Long excludeUserId);

    /**
     * Find travel plans with dates overlapping a given date range
     * 
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     * @param pageable pagination information
     * @return Page of travel plans with overlapping dates
     */
    @Query("SELECT tp FROM TravelPlan tp WHERE " +
           "tp.startDate <= :endDate AND tp.endDate >= :startDate")
    Page<TravelPlan> findWithOverlappingDates(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate,
                                             Pageable pageable);

    /**
     * Find travel plans by destination and status
     * 
     * @param destinationLocation the destination location
     * @param status the travel plan status
     * @param pageable pagination information
     * @return Page of travel plans matching the criteria
     */
    Page<TravelPlan> findByDestinationLocationAndStatus(String destinationLocation, 
                                                       TravelPlanStatus status, 
                                                       Pageable pageable);

    /**
     * Count travel plans by user and status
     * 
     * @param userId the user ID
     * @param status the travel plan status
     * @return number of travel plans for the user with the specified status
     */
    long countByUserIdAndStatus(Long userId, TravelPlanStatus status);

    /**
     * Find travel plans starting after a specific date
     * 
     * @param date the date to filter by
     * @param pageable pagination information
     * @return Page of travel plans starting after the specified date
     */
    Page<TravelPlan> findByStartDateAfter(LocalDateTime date, Pageable pageable);
}
