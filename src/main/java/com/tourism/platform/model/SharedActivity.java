package com.tourism.platform.model;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "shared_activities", indexes = {
        @Index(name = "idx_shared_activity_receiver_status", columnList = "receiver_id, status"),
        @Index(name = "idx_shared_activity_activity", columnList = "activity_id")
})
@EntityListeners(AuditingEntityListener.class)
public class SharedActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private TravelPlanActivity activity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SharedActivityStatus status;

    @Column(name = "is_shared_plan", nullable = false)
    private boolean sharedPlan;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public TravelPlanActivity getActivity() {
        return activity;
    }

    public void setActivity(TravelPlanActivity activity) {
        this.activity = activity;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public SharedActivityStatus getStatus() {
        return status;
    }

    public void setStatus(SharedActivityStatus status) {
        this.status = status;
    }

    public boolean isSharedPlan() {
        return sharedPlan;
    }

    public void setSharedPlan(boolean sharedPlan) {
        this.sharedPlan = sharedPlan;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
