package com.tourism.platform.repository;

import com.tourism.platform.model.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    @Query("SELECT ui.interest FROM UserInterest ui WHERE ui.user.id = :userId")
    List<String> findInterestValuesByUserId(@Param("userId") Long userId);
}
