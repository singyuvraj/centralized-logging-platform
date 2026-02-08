package com.suljhaoo.backend.config;

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
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Puts a trace identifier into MDC for the duration of each request so that log events
 * can include trace.id when available. Uses X-Trace-Id or X-Request-Id header if
 * present; otherwise generates a UUID. Does not introduce distributed tracing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

  private static final String TRACE_ID_HEADER = "X-Trace-Id";
  private static final String REQUEST_ID_HEADER = "X-Request-Id";
  private static final String MDC_TRACE_ID = "traceId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String traceId =
          StringUtils.hasText(request.getHeader(TRACE_ID_HEADER))
              ? request.getHeader(TRACE_ID_HEADER)
              : (StringUtils.hasText(request.getHeader(REQUEST_ID_HEADER))
                  ? request.getHeader(REQUEST_ID_HEADER)
                  : UUID.randomUUID().toString());
      MDC.put(MDC_TRACE_ID, traceId);
      if (StringUtils.hasText(traceId)) {
        response.setHeader(TRACE_ID_HEADER, traceId);
      }
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_TRACE_ID);
    }
  }
}
