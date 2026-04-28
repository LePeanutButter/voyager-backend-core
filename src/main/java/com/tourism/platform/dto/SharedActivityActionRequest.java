package com.tourism.platform.dto;

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
