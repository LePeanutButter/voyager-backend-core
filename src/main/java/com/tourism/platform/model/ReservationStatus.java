package com.tourism.platform.model;

/**
 * Enumeration for reservation status
 * 
 * Defines the different states a reservation can be in
 * throughout its lifecycle.
 */
public enum ReservationStatus {
    /**
     * Reservation is pending confirmation
     */
    PENDING,
    
    /**
     * Reservation has been confirmed
     */
    CONFIRMED,
    
    /**
     * Reservation has been cancelled
     */
    CANCELLED,
    
    /**
     * Reservation has been completed
     */
    COMPLETED,
    
    /**
     * Reservation has been modified
     */
    MODIFIED,
    
    /**
     * Reservation is on hold
     */
    ON_HOLD,
    
    /**
     * Reservation has been rejected
     */
    REJECTED,
    
    /**
     * Reservation is waiting for payment
     */
    AWAITING_PAYMENT
}
