package com.tourism.platform.repository;

import com.tourism.platform.model.SharedActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SharedActivityRepository extends JpaRepository<SharedActivity, Long> {
}
