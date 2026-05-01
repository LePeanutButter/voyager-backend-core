package com.tourism.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelConnectionDto {
    private Long userId;
    private String username;
    private String firstName;
    private String lastName;
    private String status;
}
