package com.tourism.platform.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TraceRequestFilterTest {

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void completesChain_GeneratesTraceAndClearsMdc() throws ServletException, IOException {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TraceRequestFilter filter = new TraceRequestFilter(registry);
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/travel-plans");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        assertNotNull(req.getAttribute(TraceRequestFilter.TRACE_ID));
        assertNotNull(req.getAttribute(TraceRequestFilter.REQUEST_ID));
    }

    @Test
    void usesIncomingTraceHeader_WhenPresent() throws ServletException, IOException {
        TraceRequestFilter filter = new TraceRequestFilter(new SimpleMeterRegistry());
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/health");
        req.addHeader("X-Trace-Id", "fixed-id");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertEquals("fixed-id", req.getAttribute(TraceRequestFilter.TRACE_ID));
        verify(chain).doFilter(req, res);
    }

    @Test
    void fallsBackToRequestIdHeader() throws ServletException, IOException {
        TraceRequestFilter filter = new TraceRequestFilter(new SimpleMeterRegistry());
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/x");
        req.addHeader("X-Request-Id", "rid");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, mock(FilterChain.class));

        assertEquals("rid", req.getAttribute(TraceRequestFilter.TRACE_ID));
    }

    @Test
    void runsWithAuthenticatedPrincipal() throws ServletException, IOException {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        User.builder().username("alice").password("").roles("USER").build(),
                        null));

        TraceRequestFilter filter = new TraceRequestFilter(new SimpleMeterRegistry());
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/y");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, mock(FilterChain.class));

        SecurityContextHolder.clearContext();
    }

    @Test
    void incrementsErrorCounter_On4xxResponse() throws ServletException, IOException {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TraceRequestFilter filter = new TraceRequestFilter(registry);
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/fail");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = (request, response) ->
                ((MockHttpServletResponse) response).setStatus(404);

        filter.doFilter(req, res, chain);

        assertTrue(registry.counter("http_request_errors_total_custom", "method", "GET", "path", "/fail").count() >= 1);
    }
}
