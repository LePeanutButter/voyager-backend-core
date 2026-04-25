package com.tourism.platform.dto;

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
