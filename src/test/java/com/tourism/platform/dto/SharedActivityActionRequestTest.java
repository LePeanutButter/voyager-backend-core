package com.tourism.platform.dto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedActivityActionRequestTest {

    @Test
    void testSharedActivityActionRequest() {
        SharedActivityActionRequest request = new SharedActivityActionRequest();
        
        // Test getters and setters
        request.setAction(SharedActivityActionRequest.SharedActivityAction.ACCEPT);
        
        assertEquals(SharedActivityActionRequest.SharedActivityAction.ACCEPT, request.getAction());
    }

    @Test
    void testSharedActivityActionEnum() {
        // Test enum values
        SharedActivityActionRequest.SharedActivityAction[] actions = SharedActivityActionRequest.SharedActivityAction.values();
        assertEquals(2, actions.length);
        assertTrue(java.util.Arrays.asList(actions).contains(SharedActivityActionRequest.SharedActivityAction.ACCEPT));
        assertTrue(java.util.Arrays.asList(actions).contains(SharedActivityActionRequest.SharedActivityAction.REJECT));
    }

    @Test
    void testSharedActivityActionRequestConstructor() {
        // Test that the class can be instantiated
        SharedActivityActionRequest request = new SharedActivityActionRequest();
        assertNotNull(request);
    }
}
