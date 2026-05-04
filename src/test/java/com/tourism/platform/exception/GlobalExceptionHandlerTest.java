package com.tourism.platform.exception;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    @Test
    void shouldMapOptimisticLockToConflict() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI("/shared-activities/1");
        WebRequest request = new ServletWebRequest(servletRequest);
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("shared_activities", 1L);
        try {
            MDC.put("traceId", "test-trace");

            ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                    handler.handleOptimisticLockException(ex, request);

            assertEquals(409, response.getStatusCode().value());
            GlobalExceptionHandler.ErrorResponse responseBody = response.getBody();
            assertNotNull(responseBody);
            assertEquals("test-trace", responseBody.getTraceId());
        } finally {
            MDC.clear();
        }
    }
}
