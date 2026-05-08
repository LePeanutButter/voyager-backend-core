package com.tourism.platform.dto;

/**
 * DTO describing a travel connection between two users. Contains minimal
 * relationship metadata used by connection-management endpoints.
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelConnectionDto {
    private Long id;
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String status;
}
