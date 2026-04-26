package com.tourism.platform.repository;

import com.tourism.platform.model.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {
    Optional<Connection> findByIdAndRequesterId(Long connectionId, Long requesterId);
    Optional<Connection> findByIdAndRecipientId(Long connectionId, Long recipientId);
    List<Connection> findByRequesterId(Long requesterId);
    List<Connection> findByRecipientId(Long recipientId);
    void deleteByIdAndRequesterId(Long connectionId, Long requesterId);
    void deleteByIdAndRecipientId(Long connectionId, Long recipientId);
}