package com.tourism.platform.model;

/**
 * Enumeration for user account status
 * 
 * Defines the different states a user account can be in
 * for account management and security purposes.
 */
public enum UserStatus {
    /**
     * User account is active and fully functional
     */
    ACTIVE,
    
    /**
     * User account is temporarily suspended
     */
    SUSPENDED,
    
    /**
     * User account is pending verification
     */
    PENDING_VERIFICATION,
    
    /**
     * User account has been deactivated by user
     */
    DEACTIVATED,
    
    /**
     * User account has been banned by administrator
     */
    BANNED,
    
    /**
     * User account is under review
     */
    UNDER_REVIEW
}
