package com.tourism.platform.integration.amadeus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AmadeusIntegrationConfigTest {

    @Test
    void normalizeBaseUrl_trimsTrailingSlashes() {
        assertEquals("https://test.api.amadeus.com", AmadeusIntegrationConfig.normalizeBaseUrl("https://test.api.amadeus.com///"));
    }

    @Test
    void normalizeBaseUrl_nullOrBlank_returnsDefaultHost() {
        assertEquals("https://test.api.amadeus.com", AmadeusIntegrationConfig.normalizeBaseUrl(null));
        assertEquals("https://test.api.amadeus.com", AmadeusIntegrationConfig.normalizeBaseUrl(""));
        assertEquals("https://test.api.amadeus.com", AmadeusIntegrationConfig.normalizeBaseUrl("   "));
    }

    @Test
    void normalizeBaseUrl_trimsLeadingAndTrailingWhitespace() {
        assertEquals("https://api.example.com", AmadeusIntegrationConfig.normalizeBaseUrl("  https://api.example.com  "));
    }
}
