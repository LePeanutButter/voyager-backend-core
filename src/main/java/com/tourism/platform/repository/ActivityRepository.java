package com.tourism.platform.repository;

import com.tourism.platform.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    boolean existsByOwnerUserIdAndTripContextId(Long ownerUserId, Long tripContextId);
}
