package solid.humank.genaidemo.infrastructure.observability.tracing;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * HTTP filter for distributed tracing context propagation
 * Extracts trace context from incoming requests and ensures proper propagation
 */
@Component
@Order(1) // Execute before CorrelationIdFilter
@ConditionalOnProperty(name = "tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TracingFilter.class);

    private final Tracer tracer;
    private final TraceContextManager traceContextManager;

    public TracingFilter(Tracer tracer, TraceContextManager traceContextManager) {
        this.tracer = tracer;
        this.traceContextManager = traceContextManager;
    }

    // TextMapGetter for extracting trace context from HTTP headers
    private static final TextMapGetter<HttpServletRequest> HTTP_HEADERS_GETTER = new TextMapGetter<HttpServletRequest>() {
        @Override
        public Iterable<String> keys(HttpServletRequest carrier) {
            return carrier.getHeaderNames()::asIterator;
        }

        @Override
        public String get(HttpServletRequest carrier, String key) {
            return carrier.getHeader(key);
        }
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Skip tracing for static resources (but allow actuator endpoints for testing)
        String requestURI = request.getRequestURI();
        if (shouldSkipTracing(requestURI)) {
            // Still add correlation ID for skipped requests
            String correlationId = extractOrGenerateCorrelationId(request);
            response.setHeader("X-Correlation-ID", correlationId);
            filterChain.doFilter(request, response);
            return;
        }

        // Extract trace context from incoming request headers
        Context extractedContext = io.opentelemetry.api.GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), request, HTTP_HEADERS_GETTER);

        // Create span for HTTP request
        String spanName = String.format("%s %s", request.getMethod(), requestURI);
        Span span = tracer.spanBuilder(spanName)
                .setParent(extractedContext)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("http.method", request.getMethod())
                .setAttribute("http.url", request.getRequestURL().toString())
                .setAttribute("http.scheme", request.getScheme())
                .setAttribute("http.host", request.getServerName())
                .setAttribute("http.target", requestURI)
                .setAttribute("user_agent.original", request.getHeader("User-Agent"))
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            // Update MDC with trace context
            traceContextManager.updateMDCWithTraceContext();

            // Generate or extract correlation ID
            String correlationId = extractOrGenerateCorrelationId(request);
            traceContextManager.setCorrelationId(correlationId);

            // Add correlation ID to response headers
            response.setHeader("X-Correlation-ID", correlationId);
            response.setHeader("X-Trace-ID", span.getSpanContext().getTraceId());

            log.debug("Processing request {} {} with traceId: {}, correlationId: {}",
                    request.getMethod(), requestURI,
                    span.getSpanContext().getTraceId(), correlationId);

            // Process the request
            filterChain.doFilter(request, response);

            // Add response information to span
            span.setAttribute("http.status_code", response.getStatus());
            span.setAttribute("http.response.size", response.getBufferSize());

            // Mark span as successful if status is < 400
            if (response.getStatus() < 400) {
                span.setAttribute("success", true);
            } else {
                span.setAttribute("success", false);
                span.setAttribute("error", true);
            }

        } catch (Exception e) {
            // Record error in span
            traceContextManager.recordError(e, "HTTP request processing failed");
            span.setAttribute("http.status_code", 500);
            throw e;
        } finally {
            span.end();
            // Clear trace context after request processing
            traceContextManager.clearContext();
        }
    }

    /**
     * Extracts correlation ID from request headers or generates a new one
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        // Try to get correlation ID from various header names
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null) {
            correlationId = request.getHeader("X-Request-ID");
        }
        if (correlationId == null) {
            correlationId = request.getHeader("Correlation-ID");
        }

        // Generate new correlation ID if not present
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        return correlationId;
    }

    /**
     * Determines if tracing should be skipped for the given URI
     * Note: Actuator endpoints are traced for monitoring purposes
     */
    private boolean shouldSkipTracing(String requestURI) {
        return requestURI.startsWith("/static") ||
                requestURI.startsWith("/css") ||
                requestURI.startsWith("/js") ||
                requestURI.startsWith("/images") ||
                requestURI.startsWith("/favicon.ico") ||
                requestURI.startsWith("/swagger-ui") ||
                requestURI.startsWith("/v3/api-docs");
    }
}