package com.tourism.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ConnectionEntityTest {

    @Test
    void gettersAndSetters() {
        Connection c = new Connection(1L, 2L, ConnectionStatus.PENDING);
        c.setId(9L);
        c.setMessage("hi");

        assertEquals(9L, c.getId());
        assertEquals(1L, c.getRequesterId());
        assertEquals(2L, c.getRecipientId());
        assertEquals(ConnectionStatus.PENDING, c.getStatus());
        assertEquals("hi", c.getMessage());

        LocalDateTime now = LocalDateTime.now();
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        assertEquals(now, c.getCreatedAt());
        assertEquals(now, c.getUpdatedAt());
    }
}
