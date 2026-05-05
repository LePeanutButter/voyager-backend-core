package com.tourism.platform.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitingFilterTest {

    @Test
    void shouldReturn429WhenLimitExceededForProtectedEndpoint() throws ServletException, IOException {
        RateLimitingFilter filter = new RateLimitingFilter();

        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest allowedRequest = buildRequest();
            MockHttpServletResponse allowedResponse = new MockHttpServletResponse();
            filter.doFilter(allowedRequest, allowedResponse, new MockFilterChain());
            assertEquals(200, allowedResponse.getStatus());
        }

        MockHttpServletRequest blockedRequest = buildRequest();
        blockedRequest.setAttribute("traceId", "trace-1");
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        filter.doFilter(blockedRequest, blockedResponse, new MockFilterChain());

        assertEquals(429, blockedResponse.getStatus());
    }

    @Test
    void skipsNonProtectedEndpointsEvenWithManyCalls() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        for (int i = 0; i < 80; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/users/1");
            req.setRemoteAddr("10.0.0.2");
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(req, res, new MockFilterChain());
            assertEquals(200, res.getStatus());
        }
    }

    @Test
    void usesForwardedForWhenPresent() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = buildRequest();
            req.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletRequest blocked = buildRequest();
        blocked.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1");
        blocked.setAttribute("traceId", "t");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        filter.doFilter(blocked, blockedRes, new MockFilterChain());
        assertEquals(429, blockedRes.getStatus());
    }

    @Test
    void usesRemoteUserWhenAuthenticated() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = buildRequest();
            req.setRemoteUser("alice");
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletRequest blocked = buildRequest();
        blocked.setRemoteUser("alice");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        filter.doFilter(blocked, blockedRes, new MockFilterChain());
        assertEquals(429, blockedRes.getStatus());
    }

    @Test
    void rateLimitsGetMatchesEndpoint() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        for (int i = 0; i < 60; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/matches");
            req.setRemoteAddr("10.0.0.9");
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletRequest blocked = new MockHttpServletRequest("GET", "/api/v1/matches");
        blocked.setRemoteAddr("10.0.0.9");
        MockHttpServletResponse blockedRes = new MockHttpServletResponse();
        filter.doFilter(blocked, blockedRes, new MockFilterChain());
        assertEquals(429, blockedRes.getStatus());
    }

    private MockHttpServletRequest buildRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/activities/1/share");
        request.setRemoteAddr("10.0.0.1");
        return request;
    }
}
