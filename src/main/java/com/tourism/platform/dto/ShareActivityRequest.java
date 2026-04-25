package com.tourism.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShareActivityRequest {
    @NotNull(message = "receiverUserId is required")
    private Long receiverUserId;
}
