package com.tourism.platform.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TravelerMatchDtoDataTest {

    @Test
    void travelerMatchData_builderAndCreateSimpleMatch() {
        TravelerMatchDto.TravelerMatchData data = new TravelerMatchDto.TravelerMatchData.Builder()
                .userId(1L)
                .username("u")
                .firstName("f")
                .lastName("l")
                .destinationLocation("Paris")
                .travelStartDate(LocalDateTime.of(2026, 1, 1, 0, 0))
                .travelEndDate(LocalDateTime.of(2026, 1, 10, 0, 0))
                .daysOverlap(5)
                .build();

        TravelerMatchDto dto = TravelerMatchDto.createSimpleMatch(data);
        assertEquals(1L, dto.getUserId());
        assertEquals("Paris", dto.getDestinationLocation());
        assertEquals(5, dto.getDaysOverlap());
    }

    @Test
    void createSimpleMatch_staticOverload() {
        LocalDateTime s = LocalDateTime.of(2026, 2, 1, 0, 0);
        LocalDateTime e = LocalDateTime.of(2026, 2, 5, 0, 0);
        TravelerMatchDto dto = TravelerMatchDto.createSimpleMatch(
                9L, "bob", "B", "Bson", "Rome", s, e, 3);
        assertEquals(9L, dto.getUserId());
        assertEquals("Rome", dto.getDestinationLocation());
        assertNotNull(dto.getTravelStartDate());
    }
}
