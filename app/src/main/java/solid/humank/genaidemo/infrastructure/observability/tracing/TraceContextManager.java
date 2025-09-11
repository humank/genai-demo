package solid.humank.genaidemo.infrastructure.observability.tracing;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;

/**
 * Manages trace context correlation between OpenTelemetry traces, logs, and
 * metrics
 * Ensures trace IDs and span IDs are properly propagated across the application
 */
@Component
public class TraceContextManager {

    private static final Logger log = LoggerFactory.getLogger(TraceContextManager.class);

    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String USER_ID_KEY = "userId";
    private static final String ORDER_ID_KEY = "orderId";
    private static final String CUSTOMER_ID_KEY = "customerId";

    /**
     * Sets the correlation ID and propagates it to the current span
     */
    public void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            MDC.put(CORRELATION_ID_KEY, correlationId);

            Span currentSpan = Span.current();
            if (currentSpan.getSpanContext().isValid()) {
                currentSpan.setAttribute("correlation.id", correlationId);
                log.debug("Set correlation ID: {} on span: {}", correlationId,
                        currentSpan.getSpanContext().getSpanId());
            }
        }
    }

    /**
     * Sets business context (user ID, order ID, etc.) and propagates to span
     */
    public void setBusinessContext(String userId, String orderId) {
        if (userId != null && !userId.trim().isEmpty()) {
            MDC.put(USER_ID_KEY, userId);
            Span.current().setAttribute("user.id", userId);
        }

        if (orderId != null && !orderId.trim().isEmpty()) {
            MDC.put(ORDER_ID_KEY, orderId);
            Span.current().setAttribute("order.id", orderId);
        }
    }

    /**
     * Sets customer context for customer-related operations
     */
    public void setCustomerContext(String customerId) {
        if (customerId != null && !customerId.trim().isEmpty()) {
            MDC.put(CUSTOMER_ID_KEY, customerId);
            Span.current().setAttribute("customer.id", customerId);
        }
    }

    /**
     * Updates MDC with current trace context from OpenTelemetry
     */
    public void updateMDCWithTraceContext() {
        Span currentSpan = Span.current();
        SpanContext spanContext = currentSpan.getSpanContext();

        if (spanContext.isValid()) {
            String traceId = spanContext.getTraceId();
            String spanId = spanContext.getSpanId();

            MDC.put(TRACE_ID_KEY, traceId);
            MDC.put(SPAN_ID_KEY, spanId);

            log.debug("Updated MDC with trace context - traceId: {}, spanId: {}", traceId, spanId);
        } else {
            // Clear trace context if no valid span
            MDC.remove(TRACE_ID_KEY);
            MDC.remove(SPAN_ID_KEY);
        }
    }

    /**
     * Gets the current trace ID from the active span
     */
    public Optional<String> getCurrentTraceId() {
        Span currentSpan = Span.current();
        SpanContext spanContext = currentSpan.getSpanContext();

        if (spanContext.isValid()) {
            return Optional.of(spanContext.getTraceId());
        }

        // Fallback to MDC if span is not available
        return Optional.ofNullable(MDC.get(TRACE_ID_KEY));
    }

    /**
     * Gets the current span ID from the active span
     */
    public Optional<String> getCurrentSpanId() {
        Span currentSpan = Span.current();
        SpanContext spanContext = currentSpan.getSpanContext();

        if (spanContext.isValid()) {
            return Optional.of(spanContext.getSpanId());
        }

        // Fallback to MDC if span is not available
        return Optional.ofNullable(MDC.get(SPAN_ID_KEY));
    }

    /**
     * Gets the current correlation ID
     */
    public Optional<String> getCurrentCorrelationId() {
        return Optional.ofNullable(MDC.get(CORRELATION_ID_KEY));
    }

    /**
     * Adds error information to the current span
     */
    public void recordError(Throwable throwable, String errorMessage) {
        Span currentSpan = Span.current();
        if (currentSpan.getSpanContext().isValid()) {
            currentSpan.recordException(throwable);
            currentSpan.setAttribute("error", true);
            currentSpan.setAttribute("error.message", errorMessage != null ? errorMessage : throwable.getMessage());
            currentSpan.setAttribute("error.type", throwable.getClass().getSimpleName());

            log.debug("Recorded error on span: {} - {}", currentSpan.getSpanContext().getSpanId(), errorMessage);
        }
    }

    /**
     * Adds business operation information to the current span
     */
    public void recordBusinessOperation(String operationType, String operationName, String entityId) {
        Span currentSpan = Span.current();
        if (currentSpan.getSpanContext().isValid()) {
            currentSpan.setAttribute("business.operation.type", operationType);
            currentSpan.setAttribute("business.operation.name", operationName);
            if (entityId != null) {
                currentSpan.setAttribute("business.entity.id", entityId);
            }
        }
    }

    /**
     * Clears all trace context from MDC
     */
    public void clearContext() {
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
        MDC.remove(CORRELATION_ID_KEY);
        MDC.remove(USER_ID_KEY);
        MDC.remove(ORDER_ID_KEY);
        MDC.remove(CUSTOMER_ID_KEY);

        log.debug("Cleared trace context from MDC");
    }

    /**
     * Creates a new trace context with the given correlation ID
     */
    public void initializeTraceContext(String correlationId) {
        updateMDCWithTraceContext();
        setCorrelationId(correlationId);
    }

    /**
     * Checks if the current span is being sampled
     */
    public boolean isCurrentSpanSampled() {
        Span currentSpan = Span.current();
        SpanContext spanContext = currentSpan.getSpanContext();

        return spanContext.isValid() &&
                spanContext.getTraceFlags().equals(TraceFlags.getSampled());
    }
}