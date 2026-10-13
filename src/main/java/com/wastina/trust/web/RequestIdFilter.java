package com.wastina.trust.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns a correlation/request id to every request and echoes it back on the response.
 *
 * <p>An inbound {@code X-Request-Id} (or {@code X-Correlation-Id}) is honoured so a trace started
 * by an upstream gateway survives; otherwise a new UUID is generated. The value is exposed on the
 * response so a browser client can quote it when reporting a problem, and it is placed in the SLF4J
 * MDC so log lines can be correlated with a single request.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        MDC.put(MDC_KEY, requestId);
        request.setAttribute(MDC_KEY, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String inbound = request.getHeader(REQUEST_ID_HEADER);
        if (inbound == null || inbound.isBlank()) {
            inbound = request.getHeader(CORRELATION_ID_HEADER);
        }
        if (inbound == null || inbound.isBlank()) {
            return UUID.randomUUID().toString();
        }
        // Never echo an unbounded or control-character-bearing value back into a response header.
        String trimmed = inbound.trim();
        if (trimmed.length() > 128 || trimmed.chars().anyMatch(Character::isISOControl)) {
            return UUID.randomUUID().toString();
        }
        return trimmed;
    }

    /** Reads the id assigned to the request currently being handled, or {@code null}. */
    public static String currentRequestId(HttpServletRequest request) {
        Object value = request == null ? null : request.getAttribute(MDC_KEY);
        return value instanceof String text ? text : null;
    }
}