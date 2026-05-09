package com.tourism.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tourism.platform.observability.TraceRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;
    private static final int MAX_REQUESTS = 60;

    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<Long>> requestsByKey = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!isProtectedEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String limiterKey = resolveClientKey(request);
        long now = System.currentTimeMillis();
        ConcurrentLinkedQueue<Long> queue = requestsByKey.computeIfAbsent(limiterKey, ignored -> new ConcurrentLinkedQueue<>());
        pruneExpired(queue, now);

        if (queue.size() >= MAX_REQUESTS) {
            writeRateLimitedResponse(request, response);
            return;
        }

        queue.offer(now);
        filterChain.doFilter(request, response);
    }

    private boolean isProtectedEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method)
                && (path.endsWith("/users/login")
                        || path.endsWith("/users/register")
                        || path.endsWith("/auth/google/token"))) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && path.endsWith("/auth/google/callback")) {
            return true;
        }
        return ("POST".equalsIgnoreCase(method) && path.matches(".*/activities/\\d+/share$"))
                || ("PATCH".equalsIgnoreCase(method) && path.matches(".*/shared-activities/\\d+$"))
                || ("GET".equalsIgnoreCase(method) && path.endsWith("/matches"))
                || ("POST".equalsIgnoreCase(method) && path.endsWith("/compatibility/matches"));
    }

    private String resolveClientKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String user = authentication != null && authentication.isAuthenticated() ? authentication.getName() : null;
        if (user != null && !user.isBlank()) {
            return "user:" + user;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return "ip:" + forwardedFor.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private void pruneExpired(ConcurrentLinkedQueue<Long> queue, long now) {
        Long oldest = queue.peek();
        while (oldest != null && (now - oldest) > WINDOW_MILLIS) {
            queue.poll();
            oldest = queue.peek();
        }
    }

    private void writeRateLimitedResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");

        String traceId = (String) request.getAttribute(TraceRequestFilter.TRACE_ID);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC).toString());
        body.put("status", 429);
        body.put("error", "Too Many Requests");
        body.put("message", "Rate limit exceeded. Please retry later.");
        body.put("path", request.getRequestURI());
        body.put("traceId", traceId);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
