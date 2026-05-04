package com.tourism.platform.dto;

/**
 * DTO representing an action taken on a shared activity. Includes the chosen
 * enum action (e.g., ACCEPT or REJECT) used by controllers handling shared
 * activity workflows.
 */

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SharedActivityActionRequest {
    @NotNull(message = "action is required")
    private SharedActivityAction action;

    public enum SharedActivityAction {
        ACCEPT,
        REJECT
    }
}
