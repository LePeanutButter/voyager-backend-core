package com.tourism.platform.repository;

import com.tourism.platform.model.TravelPlanActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelPlanActivityRepository extends JpaRepository<TravelPlanActivity, Long> {
    List<TravelPlanActivity> findByTravelPlanIdOrderByStartTimeAsc(Long travelPlanId);
    Optional<TravelPlanActivity> findByIdAndTravelPlanId(Long activityId, Long travelPlanId);
}
