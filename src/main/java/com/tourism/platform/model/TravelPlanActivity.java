package com.tourism.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Travel Plan Activity entity representing individual activities within a travel plan
 * 
 * This entity stores specific activities, tours, or events that are part of
 * a travel itinerary, including scheduling and cost information.
 */
@Entity
@Table(name = "travel_plan_activities", indexes = {
    @Index(name = "idx_activity_travel_plan", columnList = "travel_plan_id"),
    @Index(name = "idx_activity_start_time", columnList = "start_time")
})
@EntityListeners(AuditingEntityListener.class)
public class TravelPlanActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_plan_id", nullable = false)
    @NotNull(message = "Travel plan is required")
    private TravelPlan travelPlan;

    @Column(nullable = false)
    @NotBlank(message = "Activity name is required")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type = ActivityType.SIGHTSEEING;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "location")
    private String location;

    @Column(name = "estimated_cost")
    private BigDecimal estimatedCost;

    @Column(name = "actual_cost")
    private BigDecimal actualCost;

    @Column(name = "booking_reference")
    private String bookingReference;

    @Column(name = "is_confirmed")
    private Boolean isConfirmed = false;

    @Column(name = "notes")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Default constructor for JPA
    public TravelPlanActivity() {}

    // Constructors
    public TravelPlanActivity(TravelPlan travelPlan, String name, LocalDateTime startTime, LocalDateTime endTime) {
        this.travelPlan = travelPlan;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Business logic methods
    public boolean isCompleted() {
        return endTime != null && endTime.isBefore(LocalDateTime.now());
    }

    public boolean isUpcoming() {
        return startTime != null && startTime.isAfter(LocalDateTime.now());
    }

    public boolean isInProgress() {
        LocalDateTime now = LocalDateTime.now();
        return startTime != null && endTime != null && 
               now.isAfter(startTime) && now.isBefore(endTime);
    }

    public long getDurationInMinutes() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMinutes();
        }
        return 0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TravelPlan getTravelPlan() {
        return travelPlan;
    }

    public void setTravelPlan(TravelPlan travelPlan) {
        this.travelPlan = travelPlan;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public BigDecimal getActualCost() {
        return actualCost;
    }

    public void setActualCost(BigDecimal actualCost) {
        this.actualCost = actualCost;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
    }

    public Boolean getIsConfirmed() {
        return isConfirmed;
    }

    public void setIsConfirmed(Boolean isConfirmed) {
        this.isConfirmed = isConfirmed;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
