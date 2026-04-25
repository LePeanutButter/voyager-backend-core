package com.tourism.platform.repository;

import com.tourism.platform.model.UserConnection;
import com.tourism.platform.model.UserConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserConnectionRepository extends JpaRepository<UserConnection, Long> {
    boolean existsByRequesterUserIdAndReceiverUserIdAndTripContextIdAndStatus(
            Long requesterUserId,
            Long receiverUserId,
            Long tripContextId,
            UserConnectionStatus status
    );

    boolean existsByReceiverUserIdAndRequesterUserIdAndTripContextIdAndStatus(
            Long receiverUserId,
            Long requesterUserId,
            Long tripContextId,
            UserConnectionStatus status
    );
}
