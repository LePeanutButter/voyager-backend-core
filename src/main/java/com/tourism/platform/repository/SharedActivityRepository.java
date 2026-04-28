package com.tourism.platform.repository;

import com.tourism.platform.model.SharedActivity;
import com.tourism.platform.model.SharedActivityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SharedActivityRepository extends JpaRepository<SharedActivity, Long> {

    boolean existsByActivityIdAndReceiverIdAndStatus(Long activityId, Long receiverId, SharedActivityStatus status);

    Optional<SharedActivity> findTopByActivityIdAndReceiverIdOrderByCreatedAtDesc(Long activityId, Long receiverId);
}
