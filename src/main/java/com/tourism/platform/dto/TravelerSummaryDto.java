package com.tourism.platform.dto;

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
