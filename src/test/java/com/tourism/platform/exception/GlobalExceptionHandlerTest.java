package com.tourism.platform.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    @Test
    void shouldMapOptimisticLockToConflict() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("/shared-activities/1");
        ObjectOptimisticLockingFailureException ex =
                new ObjectOptimisticLockingFailureException("shared_activities", 1L);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
                handler.handleOptimisticLockException(ex, request);

        assertEquals(409, response.getStatusCode().value());
    }
}
