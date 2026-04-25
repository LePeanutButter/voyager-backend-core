package com.tourism.platform.repository;

import com.tourism.platform.model.ConnectionStatus;
import com.tourism.platform.model.UserConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserConnectionRepository extends JpaRepository<UserConnection, Long> {

    @Query("""
            SELECT COUNT(uc) > 0
            FROM UserConnection uc
            WHERE uc.status = :status
              AND ((uc.requester.id = :firstUserId AND uc.recipient.id = :secondUserId)
                OR (uc.requester.id = :secondUserId AND uc.recipient.id = :firstUserId))
            """)
    boolean existsConnectionBetweenUsersWithStatus(
            @Param("firstUserId") Long firstUserId,
            @Param("secondUserId") Long secondUserId,
            @Param("status") ConnectionStatus status
    );
}
