package com.tourism.platform.dto;

/**
 * Response DTO that aggregates compatibility scoring details for a matched
 * traveler. Contains overall score components and matched interests used by
 * the matching API to present compatibility information.
 */

import java.util.List;

public class CompatibilityMatchResponse {

    private Long userId;
    private double totalScore;
    private double destinationScore;
    private double dateProximityScore;
    private double interestScore;
    private List<String> matchedInterests;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public double getDestinationScore() {
        return destinationScore;
    }

    public void setDestinationScore(double destinationScore) {
        this.destinationScore = destinationScore;
    }

    public double getDateProximityScore() {
        return dateProximityScore;
    }

    public void setDateProximityScore(double dateProximityScore) {
        this.dateProximityScore = dateProximityScore;
    }

    public double getInterestScore() {
        return interestScore;
    }

    public void setInterestScore(double interestScore) {
        this.interestScore = interestScore;
    }

    public List<String> getMatchedInterests() {
        return matchedInterests;
    }

    public void setMatchedInterests(List<String> matchedInterests) {
        this.matchedInterests = matchedInterests;
    }
}
