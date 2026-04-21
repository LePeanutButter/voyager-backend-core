package com.tourism.platform.model;

/**
 * Enumeration for user roles in the Tourism Intelligent Platform
 * 
 * Defines the different types of users and their permission levels
 * within the system.
 */
public enum UserRole {
    /**
     * Regular traveler user with basic permissions
     */
    TRAVELER,
    
    /**
     * Service provider (hotels, restaurants, tour guides, etc.)
     */
    SERVICE_PROVIDER,
    
    /**
     * Local tourism guide with enhanced permissions
     */
    GUIDE,
    
    /**
     * Administrative user with system management permissions
     */
    ADMIN,
    
    /**
     * Super administrator with full system access
     */
    SUPER_ADMIN
}
