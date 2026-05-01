package com.tourism.platform.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShareActivityRequestTest {

    @Test
    void testShareActivityRequest() {
        ShareActivityRequest request = new ShareActivityRequest();
        
        // Test getters and setters
        request.setReceiverId(200L);
        
        assertEquals(200L, request.getReceiverId());
    }

    @Test
    void testShareActivityRequestConstructor() {
        // Test that the class can be instantiated
        ShareActivityRequest request = new ShareActivityRequest();
        assertNotNull(request);
    }
}
