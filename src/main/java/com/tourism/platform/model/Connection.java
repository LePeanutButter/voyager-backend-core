package com.tourism.platform.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "connections")
public class Connection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConnectionStatus status;

    @Column(name = "message", length = 500)
    private String message;

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
    public Connection() {}

    /**
     * Create a new Connection instance with the minimal required fields.
     *
     * @param requesterId id of the user who initiated the connection
     * @param recipientId id of the user receiving the connection request
     * @param status      initial connection status
     */
    public Connection(Long requesterId, Long recipientId, ConnectionStatus status) {
        this.requesterId = requesterId;
        this.recipientId = recipientId;
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
     * Get the connection requester id.
     *
     * @return requester user id
     */
    public Long getRequesterId() {
        return requesterId;
    }

    /**
     * Set the connection requester id.
     *
     * @param requesterId requester user id to set
     */
    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    /**
     * Get the recipient user id for this connection.
     *
     * @return recipient user id
     */
    public Long getRecipientId() {
        return recipientId;
    }

    /**
     * Set the recipient user id for this connection.
     *
     * @param recipientId recipient user id to set
     */
    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    /**
     * Get the current connection status.
     *
     * @return connection status enum
     */
    public ConnectionStatus getStatus() {
        return status;
    }

    /**
     * Set the connection status.
     *
     * @param status new connection status
     */
    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }

    /**
     * Get the optional message attached to the connection request.
     *
     * @return message text or null
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the optional message attached to the connection request.
     *
     * @param message text message to set (may be null)
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Get the created timestamp assigned by auditing.
     *
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Set the created timestamp (used by JPA auditing, typically not set manually).
     *
     * @param createdAt creation timestamp to set
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Get the last modification timestamp assigned by auditing.
     *
     * @return last updated timestamp
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Set the last modification timestamp (used by JPA auditing, typically not set manually).
     *
     * @param updatedAt updated timestamp to set
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}