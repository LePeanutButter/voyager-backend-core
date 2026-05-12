package com.tourism.platform.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageEntityTest {

    @Test
    void gettersReflectSetValues() {
        Message m = new Message();
        m.setId(1L);
        m.setConnectionId(2L);
        m.setSenderId(3L);
        m.setRecipientId(4L);
        m.setContent("hello");
        m.setStatus(MessageStatus.READ);
        LocalDateTime t = LocalDateTime.of(2026, 5, 1, 12, 0);
        m.setCreatedAt(t);
        m.setUpdatedAt(t.plusHours(1));

        assertEquals(1L, m.getId());
        assertEquals(2L, m.getConnectionId());
        assertEquals(3L, m.getSenderId());
        assertEquals(4L, m.getRecipientId());
        assertEquals("hello", m.getContent());
        assertEquals(MessageStatus.READ, m.getStatus());
        assertEquals(t, m.getCreatedAt());
    }

    @Test
    void constructor_initializesFields() {
        Message m = new Message(10L, 20L, 30L, "x", MessageStatus.SENT);
        assertEquals(10L, m.getConnectionId());
        assertEquals(20L, m.getSenderId());
        assertEquals(30L, m.getRecipientId());
        assertEquals("x", m.getContent());
        assertEquals(MessageStatus.SENT, m.getStatus());
    }
}

