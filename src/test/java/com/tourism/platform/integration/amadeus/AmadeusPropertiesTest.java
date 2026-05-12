package com.tourism.platform.integration.amadeus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AmadeusPropertiesTest {

    @Test
    void defaults_matchApplicationYamlExpectations() {
        AmadeusProperties p = new AmadeusProperties();
        assertTrue(p.isEnabled());
        assertTrue(p.isMockMode());
        assertEquals("https://test.api.amadeus.com", p.getApiHost());
        assertEquals("", p.getClientId());
        assertEquals("", p.getClientSecret());
        assertEquals(5_000, p.getConnectTimeoutMillis());
        assertEquals(20_000, p.getReadTimeoutMillis());
    }

    @Test
    void setters_roundTrip() {
        AmadeusProperties p = new AmadeusProperties();
        p.setEnabled(false);
        p.setMockMode(false);
        p.setApiHost("https://prod.example");
        p.setClientId("a");
        p.setClientSecret("b");
        p.setTokenPath("/custom/token");
        p.setConnectTimeoutMillis(1000);
        p.setReadTimeoutMillis(2000);

        assertFalse(p.isEnabled());
        assertFalse(p.isMockMode());
        assertEquals("https://prod.example", p.getApiHost());
        assertEquals("a", p.getClientId());
        assertEquals("b", p.getClientSecret());
        assertEquals("/custom/token", p.getTokenPath());
        assertEquals(1000, p.getConnectTimeoutMillis());
        assertEquals(2000, p.getReadTimeoutMillis());
    }
}
