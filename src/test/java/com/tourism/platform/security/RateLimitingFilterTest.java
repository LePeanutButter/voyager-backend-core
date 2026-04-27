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

    private MockHttpServletRequest buildRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/activities/1/share");
        request.setRemoteAddr("10.0.0.1");
        return request;
    }
}
