package com.tourism.platform.dto;

/**
 * Summary DTO exposing aggregated traveler information such as counts of
 * connections, active plans and recent activity used on profile summary
 * endpoints.
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TravelerSummaryDto {
    private Long userId;
    private String displayName;
    private String bioShort;
    private String profileImageUrl;
    private String interests;
}
