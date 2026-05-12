package com.tourism.platform.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private static HttpInputMessage emptyHttpInputMessage() {
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };
    }

    private WebRequest webRequest(String uri) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setRequestURI(uri);
        return new ServletWebRequest(servletRequest);
    }

    @Test
    void shouldMapOptimisticLockToConflict() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        WebRequest request = webRequest("/shared-activities/1");
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

    @Test
    void handleEntityNotFound_returns404() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleEntityNotFoundException(new EntityNotFoundException("x"), webRequest("/r"));
        assertEquals(404, res.getStatusCode().value());
        GlobalExceptionHandler.ErrorResponse body = res.getBody();
        assertNotNull(body);
        assertEquals("/r", body.getPath());
    }

    @Test
    void handleResourceNotFound_returns404() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleResourceNotFoundException(new ResourceNotFoundException("missing"), webRequest("/u/1"));
        assertEquals(404, res.getStatusCode().value());
        GlobalExceptionHandler.ErrorResponse body = res.getBody();
        assertNotNull(body);
        assertEquals("missing", body.getMessage());
    }

    @Test
    void handleIllegalArgument_returns400() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleIllegalArgumentException(new IllegalArgumentException("bad"), webRequest("/x"));
        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void handleBadRequest_returns400() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleBadRequestException(new BadRequestException("nope"), webRequest("/x"));
        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void handleBusiness_returns400() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleBusinessException(new BusinessException("biz"), webRequest("/x"));
        assertEquals(400, res.getStatusCode().value());
    }

    @Test
    void handleConflict_returns409() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleConflictException(new ConflictException("c"), webRequest("/x"));
        assertEquals(409, res.getStatusCode().value());
    }

    @Test
    void handleAuthentication_returns401() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleAuthenticationException(new BadCredentialsException("x"), webRequest("/x"));
        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void handleAccessDenied_returns403() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleAccessDeniedException(new AccessDeniedException("x"), webRequest("/x"));
        assertEquals(403, res.getStatusCode().value());
    }

    @Test
    void handleExternalService_returns502() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleExternalServiceException(new ExternalServiceException("ext"), webRequest("/x"));
        assertEquals(502, res.getStatusCode().value());
    }

    @Test
    void handleGlobal_returns500() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleGlobalException(new RuntimeException("boom"), webRequest("/x"));
        assertEquals(500, res.getStatusCode().value());
        GlobalExceptionHandler.ErrorResponse body = res.getBody();
        assertNotNull(body);
        assertEquals("An unexpected error occurred", body.getMessage());
    }

    @Test
    void handleMessageNotReadable_usesCauseDetailWhenPresent() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        Throwable cause = new IllegalArgumentException("invalid enum value");
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("cannot parse", cause, emptyHttpInputMessage());

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleRequestBodyNotReadable(ex, webRequest("/plans"));

        assertEquals(400, res.getStatusCode().value());
        GlobalExceptionHandler.ErrorResponse body = res.getBody();
        assertNotNull(body);
        assertEquals("invalid enum value", body.getMessage());
    }

    @Test
    void handleMessageNotReadable_fallsBackWhenCauseMessageIsBlank() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("cannot parse", new IllegalArgumentException("   "), emptyHttpInputMessage());

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleRequestBodyNotReadable(ex, webRequest("/plans"));

        assertEquals(400, res.getStatusCode().value());
        GlobalExceptionHandler.ErrorResponse body = res.getBody();
        assertNotNull(body);
        assertEquals("Malformed or unreadable JSON request body", body.getMessage());
    }

    @Test
    void handleMessageNotReadable_fallsBackWhenCauseHasNoDetail() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpMessageNotReadableException ex =
                new HttpMessageNotReadableException("cannot parse", new RuntimeException((String) null), emptyHttpInputMessage());

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleRequestBodyNotReadable(ex, webRequest("/plans"));

        assertEquals(400, res.getStatusCode().value());
        GlobalExceptionHandler.ErrorResponse body = res.getBody();
        assertNotNull(body);
        assertEquals("Malformed or unreadable JSON request body", body.getMessage());
    }

    @Test
    void buildResponse_usesDescriptionWhenNotServletWebRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        WebRequest genericRequest = mock(WebRequest.class);
        when(genericRequest.getDescription(false)).thenReturn("uri=/custom");

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleIllegalArgumentException(new IllegalArgumentException("bad"), genericRequest);

        GlobalExceptionHandler.ErrorResponse body = res.getBody();
        assertNotNull(body);
        assertEquals("uri=/custom", body.getPath());
    }

    @Test
    void handleValidation_usesDefaultMessageWhenNoFieldErrors() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ValidationHolder.Dto target = new ValidationHolder.Dto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "dto");
        Method endpoint = ValidationHolder.class.getDeclaredMethod("post", ValidationHolder.Dto.class);
        MethodParameter parameter = new MethodParameter(endpoint, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleValidationExceptions(ex, webRequest("/users"));

        assertEquals(400, res.getStatusCode().value());
        GlobalExceptionHandler.ErrorResponse body = res.getBody();
        assertNotNull(body);
        assertEquals("Validation failed", body.getMessage());
    }

    @Test
    void handleValidation_buildsFieldMessage() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ValidationHolder.Dto target = new ValidationHolder.Dto();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "dto");
        bindingResult.addError(new FieldError("dto", "email", "must not be blank"));
        Method endpoint = ValidationHolder.class.getDeclaredMethod("post", ValidationHolder.Dto.class);
        MethodParameter parameter = new MethodParameter(endpoint, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<GlobalExceptionHandler.ErrorResponse> res =
                handler.handleValidationExceptions(ex, webRequest("/users"));

        assertEquals(400, res.getStatusCode().value());
        GlobalExceptionHandler.ErrorResponse body = res.getBody();
        assertNotNull(body);
        assertTrue(body.getMessage().contains("email"));
    }

    static class ValidationHolder {
        static class Dto {
            @NotBlank
            private String email;

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }
        }

        public void post(@Valid Dto dto) {
            // test hook only
        }
    }

    @Test
    void errorResponse_settersAndGetters() {
        GlobalExceptionHandler.ErrorResponse er = new GlobalExceptionHandler.ErrorResponse();
        er.setTimestamp(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        er.setStatus(400);
        er.setError("Bad Request");
        er.setMessage("m");
        er.setPath("/p");
        er.setTraceId("t");
        assertEquals(400, er.getStatus());
        assertEquals("m", er.getMessage());
        assertEquals("/p", er.getPath());
        assertEquals("t", er.getTraceId());
        assertEquals("Bad Request", er.getError());
        assertNotNull(er.getTimestamp());
    }
}
