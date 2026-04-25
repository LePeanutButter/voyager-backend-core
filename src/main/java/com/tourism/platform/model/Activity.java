package com.tourism.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "activities", indexes = {
        @Index(name = "idx_activity_owner", columnList = "owner_user_id"),
        @Index(name = "idx_activity_trip_context", columnList = "trip_context_id")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    @NotNull(message = "Activity owner is required")
    private User ownerUser;

    @Column(nullable = false, length = 120)
    @NotBlank(message = "Activity title is required")
    private String title;

    @Column(name = "trip_context_id", nullable = false)
    @NotNull(message = "Trip context is required")
    private Long tripContextId;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
