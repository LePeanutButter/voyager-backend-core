package com.tourism.platform.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Travel Plan entity representing user travel itineraries
 * 
 * This entity stores comprehensive travel plan information including
 * destinations, activities, budget, and scheduling details.
 */
@Entity
@Table(name = "travel_plans", indexes = {
    @Index(name = "idx_travel_plan_user", columnList = "user_id"),
    @Index(name = "idx_travel_plan_status", columnList = "status"),
    @Index(name = "idx_travel_plan_start_date", columnList = "start_date")
})
@EntityListeners(AuditingEntityListener.class)
public class TravelPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Column(nullable = false)
    @NotBlank(message = "Title is required")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TravelPlanStatus status = TravelPlanStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TravelType travelType = TravelType.LEISURE;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "estimated_budget")
    private BigDecimal estimatedBudget;

    @Column(name = "actual_cost")
    private BigDecimal actualCost;

    @Column(name = "number_of_travelers")
    private Integer numberOfTravelers = 1;

    @Column(name = "origin_location")
    private String originLocation;

    @Column(name = "destination_location")
    @NotBlank(message = "Destination location is required")
    private String destinationLocation;

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<TravelPlanActivity> activities = new HashSet<>();

    @OneToMany(mappedBy = "travelPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Reservation> reservations = new HashSet<>();

    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Column(name = "share_token")
    private String shareToken;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Default constructor for JPA
    public TravelPlan() {}

    // Constructors
    public TravelPlan(User user, String title, String destinationLocation) {
        this.user = user;
        this.title = title;
        this.destinationLocation = destinationLocation;
    }

    // Business logic methods
    public void addActivity(TravelPlanActivity activity) {
        activities.add(activity);
        activity.setTravelPlan(this);
    }

    public void removeActivity(TravelPlanActivity activity) {
        activities.remove(activity);
        activity.setTravelPlan(null);
    }

    public void addReservation(Reservation reservation) {
        reservations.add(reservation);
        reservation.setTravelPlan(this);
    }

    public void removeReservation(Reservation reservation) {
        reservations.remove(reservation);
        reservation.setTravelPlan(null);
    }

    public boolean isCompleted() {
        return status == TravelPlanStatus.COMPLETED;
    }

    public boolean isActive() {
        return status == TravelPlanStatus.ACTIVE;
    }

    public long getDurationInDays() {
        if (startDate != null && endDate != null) {
            return java.time.Duration.between(startDate, endDate).toDays();
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TravelPlanStatus getStatus() {
        return status;
    }

    public void setStatus(TravelPlanStatus status) {
        this.status = status;
    }

    public TravelType getTravelType() {
        return travelType;
    }

    public void setTravelType(TravelType travelType) {
        this.travelType = travelType;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getEstimatedBudget() {
        return estimatedBudget;
    }

    public void setEstimatedBudget(BigDecimal estimatedBudget) {
        this.estimatedBudget = estimatedBudget;
    }

    public BigDecimal getActualCost() {
        return actualCost;
    }

    public void setActualCost(BigDecimal actualCost) {
        this.actualCost = actualCost;
    }

    public Integer getNumberOfTravelers() {
        return numberOfTravelers;
    }

    public void setNumberOfTravelers(Integer numberOfTravelers) {
        this.numberOfTravelers = numberOfTravelers;
    }

    public String getOriginLocation() {
        return originLocation;
    }

    public void setOriginLocation(String originLocation) {
        this.originLocation = originLocation;
    }

    public String getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(String destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public Set<TravelPlanActivity> getActivities() {
        return activities;
    }

    public void setActivities(Set<TravelPlanActivity> activities) {
        this.activities = activities;
    }

    public Set<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(Set<Reservation> reservations) {
        this.reservations = reservations;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
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
