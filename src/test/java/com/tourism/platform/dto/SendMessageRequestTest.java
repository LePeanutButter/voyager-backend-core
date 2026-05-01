package com.tourism.platform.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SendMessageRequestTest {

    @Test
    void testSendMessageRequest() {
        SendMessageRequest request = new SendMessageRequest();
        
        // Test getters and setters
        request.setConnectionId(1L);
        request.setSenderId(100L);
        request.setContent("Hello, world!");
        
        assertEquals(1L, request.getConnectionId());
        assertEquals(100L, request.getSenderId());
        assertEquals("Hello, world!", request.getContent());
    }

    @Test
    void testSendMessageRequestConstructor() {
        // Test that the class can be instantiated
        SendMessageRequest request = new SendMessageRequest();
        assertNotNull(request);
    }

    @Test
    void allArgsConstructor_SetsFields() {
        SendMessageRequest request = new SendMessageRequest(5L, 9L, "hi");
        assertEquals(5L, request.getConnectionId());
        assertEquals(9L, request.getSenderId());
        assertEquals("hi", request.getContent());
    }
}
