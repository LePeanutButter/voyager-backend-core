package com.tourism.platform.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_interests", indexes = {
        @Index(name = "idx_user_interest_user", columnList = "user_id"),
        @Index(name = "idx_user_interest_value", columnList = "interest")
})
public class UserInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "interest", nullable = false)
    private String interest;

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getInterest() {
        return interest;
    }

    public void setInterest(String interest) {
        this.interest = interest;
    }
}
