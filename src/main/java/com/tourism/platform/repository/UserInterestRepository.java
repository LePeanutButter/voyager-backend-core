package com.tourism.platform.repository;

import com.tourism.platform.model.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface UserInterestRepository extends JpaRepository<UserInterest, Long> {

    @Query("SELECT ui.interest FROM UserInterest ui WHERE ui.user.id = :userId")
    List<String> findInterestValuesByUserId(@Param("userId") Long userId);

    @Query("SELECT ui.user.id, ui.interest FROM UserInterest ui WHERE ui.user.id IN :userIds")
    List<Object[]> findUserInterestsByUserIds(@Param("userIds") Set<Long> userIds);
}
