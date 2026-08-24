package org.zerock.puppyrun.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        long startedAt = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            logRequestCompletion(request, response, startedAt);
            MDC.remove(TRACE_ID);
        }
    }

    private void logRequestCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            long startedAt
    ) {
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        int status = response.getStatus();
        String contentType = request.getContentType() == null ? "-" : request.getContentType();

        if (status >= 400) {
            log.warn(
                    "HTTP request completed. method={}, uri={}, status={}, contentType={}, durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status,
                    contentType,
                    durationMs
            );
            return;
        }

        log.info(
                "HTTP request completed. method={}, uri={}, status={}, contentType={}, durationMs={}",
                request.getMethod(),
                request.getRequestURI(),
                status,
                contentType,
                durationMs
        );
    }
}
