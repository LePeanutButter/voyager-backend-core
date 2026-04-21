package com.tourism.platform.model;

/**
 * Enumeration for travel plan status
 * 
 * Defines the different states a travel plan can be in
 * throughout its lifecycle.
 */
public enum TravelPlanStatus {
    /**
     * Plan is being created and not yet finalized
     */
    DRAFT,
    
    /**
     * Plan is active and currently being executed
     */
    ACTIVE,
    
    /**
     * Plan has been completed successfully
     */
    COMPLETED,
    
    /**
     * Plan has been cancelled
     */
    CANCELLED,
    
    /**
     * Plan is on hold temporarily
     */
    ON_HOLD,
    
    /**
     * Plan is archived and no longer active
     */
    ARCHIVED
}
