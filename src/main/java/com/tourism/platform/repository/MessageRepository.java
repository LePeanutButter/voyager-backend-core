package com.tourism.platform.repository;

import com.tourism.platform.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConnectionIdOrderByCreatedAtDesc(Long connectionId);
    List<Message> findBySenderIdAndRecipientIdOrRecipientIdAndSenderIdOrderByCreatedAtDesc(Long user1Id, Long user2Id);
    Page<Message> findByConnectionIdOrderByCreatedAtDesc(Long connectionId, Pageable pageable);
}
