package com.tourism.platform.integration.amadeus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AmadeusIntegrationConfigTest {

    @Test
    void normalizeBaseUrl_trimsTrailingSlashes() {
        assertEquals("https://test.api.amadeus.com", AmadeusIntegrationConfig.normalizeBaseUrl("https://test.api.amadeus.com///"));
    }
}
