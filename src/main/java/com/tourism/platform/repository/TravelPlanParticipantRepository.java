package com.tourism.platform.repository;

import com.tourism.platform.model.TravelPlanParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelPlanParticipantRepository extends JpaRepository<TravelPlanParticipant, Long> {

    boolean existsByTravelPlanIdAndUserId(Long travelPlanId, Long userId);
}
