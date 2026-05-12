// src/main/java/com/tourism/platform/repository/SharedSpaceAccessRepository.java
package com.tourism.platform.repository;

import com.tourism.platform.model.SharedSpaceAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SharedSpaceAccessRepository extends JpaRepository<SharedSpaceAccess, Long> {
    List<SharedSpaceAccess> findByConnectionId(Long connectionId);
    void deleteByConnectionId(Long connectionId);
}