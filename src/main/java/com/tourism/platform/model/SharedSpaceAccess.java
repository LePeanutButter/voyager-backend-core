package com.tourism.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shared_space_access")
public class SharedSpaceAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "connection_id", nullable = false)
    private Long connectionId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "space_type", nullable = false)
    private String spaceType;

    @Column(name = "access_granted_at")
    private LocalDateTime accessGrantedAt;

    // Constructors
    public SharedSpaceAccess() {}

    public SharedSpaceAccess(Long connectionId, Long userId, String spaceType) {
        this.connectionId = connectionId;
        this.userId = userId;
        this.spaceType = spaceType;
        this.accessGrantedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSpaceType() {
        return spaceType;
    }

    public void setSpaceType(String spaceType) {
        this.spaceType = spaceType;
    }

    public LocalDateTime getAccessGrantedAt() {
        return accessGrantedAt;
    }

    public void setAccessGrantedAt(LocalDateTime accessGrantedAt) {
        this.accessGrantedAt = accessGrantedAt;
    }
}