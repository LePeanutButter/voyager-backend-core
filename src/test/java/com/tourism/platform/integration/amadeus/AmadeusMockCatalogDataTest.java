package com.tourism.platform.integration.amadeus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class AmadeusMockCatalogDataTest {

    @Test
    void totalMockRecords_atLeast500() {
        AmadeusMockCatalogData data = new AmadeusMockCatalogData(new ObjectMapper());
        assertTrue(data.totalMockRecords() >= 500);
    }

    @Test
    void flightWindow_matchesReadmeExpectation() {
        assertEquals("2026-05-25", AmadeusMockCatalogData.MOCK_WINDOW_START);
        assertEquals("2027-01-31", AmadeusMockCatalogData.MOCK_WINDOW_END);
    }
}
