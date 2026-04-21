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
 * Reservation entity representing bookings and reservations
 * 
 * This entity stores information about various types of reservations
 * including hotels, flights, tours, and other travel services.
 */
@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_reservation_user", columnList = "user_id"),
    @Index(name = "idx_reservation_travel_plan", columnList = "travel_plan_id"),
    @Index(name = "idx_reservation_status", columnList = "status"),
    @Index(name = "idx_reservation_type", columnList = "type")
})
@EntityListeners(AuditingEntityListener.class)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_plan_id")
    private TravelPlan travelPlan;

    @Column(nullable = false)
    @NotBlank(message = "Reservation name is required")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationType type = ReservationType.HOTEL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "confirmation_number")
    private String confirmationNumber;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "location")
    private String location;

    @Column(name = "service_provider")
    private String serviceProvider;

    @Column(name = "total_cost")
    private BigDecimal totalCost;

    @Column(name = "deposit_paid")
    private BigDecimal depositPaid;

    @Column(name = "is_paid")
    private Boolean isPaid = false;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "cancellation_policy")
    private String cancellationPolicy;

    @Column(name = "special_requests")
    private String specialRequests;

    @Column(name = "contact_info")
    private String contactInfo;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Default constructor for JPA
    public Reservation() {}

    // Constructors
    public Reservation(User user, String name, ReservationType type) {
        this.user = user;
        this.name = name;
        this.type = type;
    }

    // Business logic methods
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return startDate != null && endDate != null && 
               now.isAfter(startDate) && now.isBefore(endDate);
    }

    public boolean isUpcoming() {
        return startDate != null && startDate.isAfter(LocalDateTime.now());
    }

    public boolean isCompleted() {
        return endDate != null && endDate.isBefore(LocalDateTime.now());
    }

    public boolean isCancelled() {
        return status == ReservationStatus.CANCELLED;
    }

    public boolean isConfirmed() {
        return status == ReservationStatus.CONFIRMED;
    }

    public long getDurationInDays() {
        if (startDate != null && endDate != null) {
            return java.time.Duration.between(startDate, endDate).toDays();
        }
        return 0;
    }

    public BigDecimal getRemainingBalance() {
        if (totalCost != null && depositPaid != null) {
            return totalCost.subtract(depositPaid);
        }
        return totalCost != null ? totalCost : BigDecimal.ZERO;
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

    public ReservationType getType() {
        return type;
    }

    public void setType(ReservationType type) {
        this.type = type;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(String serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getDepositPaid() {
        return depositPaid;
    }

    public void setDepositPaid(BigDecimal depositPaid) {
        this.depositPaid = depositPaid;
    }

    public Boolean getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(Boolean isPaid) {
        this.isPaid = isPaid;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCancellationPolicy() {
        return cancellationPolicy;
    }

    public void setCancellationPolicy(String cancellationPolicy) {
        this.cancellationPolicy = cancellationPolicy;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
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
