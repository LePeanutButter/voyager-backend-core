package com.tourism.platform.dto;

/**
 * Request DTO used when making a decision on a shared activity (accept/reject).
 * Carries the selected action and any optional metadata required by the API.
 */

import com.tourism.platform.model.SharedActivityDecisionAction;
import jakarta.validation.constraints.NotNull;

public class SharedActivityDecisionRequest {

    @NotNull(message = "action is required")
    private SharedActivityDecisionAction action;

    public SharedActivityDecisionAction getAction() {
        return action;
    }

    public void setAction(SharedActivityDecisionAction action) {
        this.action = action;
    }
}
