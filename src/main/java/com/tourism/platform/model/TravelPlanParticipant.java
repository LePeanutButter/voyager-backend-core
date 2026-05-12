package com.tourism.platform.model;

import jakarta.persistence.*;

@Entity
@Table(name = "travel_plan_participants", indexes = {
        @Index(name = "idx_tpp_travel_plan", columnList = "travel_plan_id"),
        @Index(name = "idx_tpp_user", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_tpp_travel_plan_user", columnNames = {"travel_plan_id", "user_id"})
})
public class TravelPlanParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_plan_id", nullable = false)
    private TravelPlan travelPlan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Long getId() {
        return id;
    }

    public TravelPlan getTravelPlan() {
        return travelPlan;
    }

    public void setTravelPlan(TravelPlan travelPlan) {
        this.travelPlan = travelPlan;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
