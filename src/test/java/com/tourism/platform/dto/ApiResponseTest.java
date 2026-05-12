package com.tourism.platform.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTest {

    @Test
    void success_withData() {
        ApiResponse<String> r = ApiResponse.success(200, "ok", "payload", "/p");
        assertEquals(200, r.getStatus());
        assertEquals("ok", r.getMessage());
        assertEquals("payload", r.getData());
        assertEquals("/p", r.getPath());
        assertNotNull(r.getTimestamp());
    }

    @Test
    void success_withoutData() {
        ApiResponse<Void> r = ApiResponse.success(204, "no content", "/p");
        assertEquals(204, r.getStatus());
        assertNull(r.getData());
    }

    @Test
    void error_withValidationErrors() {
        var err = ApiResponse.ValidationError.builder().field("f").message("m").build();
        ApiResponse<Void> r = ApiResponse.error(400, "bad", "/p", List.of(err));
        assertEquals(400, r.getStatus());
        assertEquals(1, r.getErrors().size());
        assertEquals("f", r.getErrors().get(0).getField());
    }

    @Test
    void error_withoutErrors() {
        ApiResponse<Void> r = ApiResponse.error(500, "err", "/p");
        assertNull(r.getErrors());
    }
}
