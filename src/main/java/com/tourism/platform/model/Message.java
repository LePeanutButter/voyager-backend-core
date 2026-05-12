package com.tourism.platform.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@EntityListeners(AuditingEntityListener.class)
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "connection_id", nullable = false)
    private Long connectionId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MessageStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    /**
     * Default no-arg constructor required by JPA.
     */
    public Message() {}

    /**
     * Create a new Message entity with required fields.
     *
     * @param connectionId id of the connection to which the message belongs
     * @param senderId     id of the sending user
     * @param recipientId  id of the receiving user
     * @param content      message content text
     * @param status       initial message status
     */
    public Message(Long connectionId, Long senderId, Long recipientId, String content, MessageStatus status) {
        this.connectionId = connectionId;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.content = content;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    /**
     * Get the id of the message.
     *
     * @return message id
     */
    public Long getConnectionId() {
        return connectionId;
    }

    /**
     * Set the connection id for this message.
     *
     * @param connectionId connection id to set
     */
    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }

    /**
     * Get the sender user id.
     *
     * @return sender user id
     */
    public Long getSenderId() {
        return senderId;
    }

    /**
     * Set the sender user id.
     *
     * @param senderId sender user id to set
     */
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    /**
     * Get the recipient user id.
     *
     * @return recipient user id
     */
    public Long getRecipientId() {
        return recipientId;
    }

    /**
     * Set the recipient user id.
     *
     * @param recipientId recipient user id to set
     */
    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    /**
     * Get the message content.
     *
     * @return message content text
     */
    public String getContent() {
        return content;
    }

    /**
     * Set the message content.
     *
     * @param content text to set as message content
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Get the message status.
     *
     * @return MessageStatus enum value
     */
    public MessageStatus getStatus() {
        return status;
    }

    /**
     * Set the message status.
     *
     * @param status new message status to set
     */
    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    /**
     * Get the created timestamp for the message.
     *
     * @return message creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Set the created timestamp (typically set by JPA auditing).
     *
     * @param createdAt creation timestamp to set
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Get the last updated timestamp for the message.
     *
     * @return last modification timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Set the last updated timestamp (typically set by JPA auditing).
     *
     * @param updatedAt updated timestamp to set
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    // Constructors, getters, setters
}