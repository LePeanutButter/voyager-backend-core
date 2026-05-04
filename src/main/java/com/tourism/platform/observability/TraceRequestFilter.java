package com.tourism.platform.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceRequestFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";
    private static final String TAG_METHOD = "method";
    private static final String TAG_PATH = "path";
    private static final String TAG_STATUS = "status";

    private static final Logger log = LoggerFactory.getLogger(TraceRequestFilter.class);
    private final MeterRegistry meterRegistry;

    public TraceRequestFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        long startNanos = System.nanoTime();
        String path = request.getRequestURI();
        String method = request.getMethod();

        MDC.put(TRACE_ID, traceId);
        MDC.put(REQUEST_ID, traceId);
        request.setAttribute(TRACE_ID, traceId);
        request.setAttribute(REQUEST_ID, traceId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            MDC.put(USER_ID, authentication.getName());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            int status = response.getStatus();
            String statusLabel = status >= 400 ? "ERROR" : "SUCCESS";

            meterRegistry.counter("http_requests_total_custom",
                    TAG_METHOD, method,
                    TAG_PATH, path,
                    TAG_STATUS, String.valueOf(status)).increment();
            Timer.builder("http_request_duration_custom")
                    .tag(TAG_METHOD, method)
                    .tag(TAG_PATH, path)
                    .register(meterRegistry)
                    .record(durationMs, TimeUnit.MILLISECONDS);
            if (status >= 400) {
                meterRegistry.counter("http_request_errors_total_custom",
                        TAG_METHOD, method,
                        TAG_PATH, path).increment();
            }

            log.info("event=request_completed method={} path={} status={} durationMs={} endpoint={} statusLabel={}",
                    method, path, status, durationMs, path, statusLabel);
            MDC.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String incoming = request.getHeader("X-Trace-Id");
        if (incoming == null || incoming.isBlank()) {
            incoming = request.getHeader("X-Request-Id");
        }
        return (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;
    }
}
