package com.tourism.platform.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_connections", indexes = {
        @Index(name = "idx_user_connection_requester", columnList = "requester_id"),
        @Index(name = "idx_user_connection_recipient", columnList = "recipient_id"),
        @Index(name = "idx_user_connection_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_connection_pair", columnNames = {"requester_id", "recipient_id"})
})
public class UserConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConnectionStatus status;

    public Long getId() {
        return id;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public ConnectionStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectionStatus status) {
        this.status = status;
    }
}
