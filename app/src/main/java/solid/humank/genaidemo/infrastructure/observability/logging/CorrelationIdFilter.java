package solid.humank.genaidemo.infrastructure.observability.logging;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet filter that ensures every HTTP request has a correlation ID for
 * tracing requests across the system. The correlation ID is added to the MDC
 * (Mapped
 * Diagnostic Context) and automatically included in all log messages.
 * 
 * Note: When tracing is enabled, TracingFilter handles correlation ID
 * management.
 * This filter only runs when tracing is disabled.
 */
@Component
@Order(2) // Execute after TracingFilter when tracing is enabled
@ConditionalOnProperty(name = "tracing.enabled", havingValue = "false")
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Get correlation ID from header or generate new one
            String correlationId = getOrGenerateCorrelationId(request);

            // Add to MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            // Add to response header for client tracking
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue with the request
            filterChain.doFilter(request, response);

        } finally {
            // Always clean up MDC to prevent memory leaks
            MDC.clear();
        }
    }

    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        // Try to get correlation ID from request header
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.trim().isEmpty()) {
            // Generate new correlation ID if not provided
            correlationId = UUID.randomUUID().toString();
        }

        return correlationId;
    }
}