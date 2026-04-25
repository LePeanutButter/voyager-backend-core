package com.tourism.platform.repository;

import com.tourism.platform.model.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    List<TravelPlan> findByUserId(Long userId);

    List<TravelPlan> findByUserIdIn(Set<Long> userIds);
}
