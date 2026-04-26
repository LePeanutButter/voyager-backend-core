package com.tourism.platform.repository;

import com.tourism.platform.model.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    Optional<Connection> findByIdAndRequesterIdOrRecipientId(Long connectionId, Long userId);
    List<Connection> findByRequesterIdOrRecipientId(Long userId);
    void deleteByIdAndRequesterIdOrRecipientId(Long connectionId, Long userId);
}