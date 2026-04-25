package com.tourism.platform.repository;

import com.tourism.platform.model.TravelPlan;
import com.tourism.platform.model.TravelPlanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    List<TravelPlan> findByUserId(Long userId);

    List<TravelPlan> findByUserIdIn(Set<Long> userIds);

    Page<TravelPlan> findByUserId(Long userId, Pageable pageable);

    Page<TravelPlan> findByUserIdAndStatus(Long userId, TravelPlanStatus status, Pageable pageable);

    @Query("""
            SELECT tp
            FROM TravelPlan tp
            WHERE tp.destinationLocation = :destination
              AND tp.user.id <> :requestingUserId
              AND tp.startDate <= :endDate
              AND tp.endDate >= :startDate
            """)
    List<TravelPlan> findCompatibleTravelPlans(@Param("destination") String destination,
                                               @Param("startDate") java.time.LocalDateTime startDate,
                                               @Param("endDate") java.time.LocalDateTime endDate,
                                               @Param("requestingUserId") Long requestingUserId);
}
