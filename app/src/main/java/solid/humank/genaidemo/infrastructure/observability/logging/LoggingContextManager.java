package solid.humank.genaidemo.infrastructure.observability.logging;

import java.util.Map;
import java.util.Optional;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Manages logging context by providing convenient methods to set and retrieve
 * contextual information in the MDC (Mapped Diagnostic Context).
 * This information is automatically included in structured log messages.
 */
@Component
public class LoggingContextManager {

    // MDC Keys
    public static final String CORRELATION_ID = "correlationId";
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String USER_ID = "userId";
    public static final String CUSTOMER_ID = "customerId";
    public static final String ORDER_ID = "orderId";
    public static final String OPERATION = "operation";
    public static final String COMPONENT = "component";

    /**
     * Set correlation ID in MDC
     */
    public void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            MDC.put(CORRELATION_ID, correlationId);
        }
    }

    /**
     * Set trace ID in MDC (for distributed tracing)
     */
    public void setTraceId(String traceId) {
        if (traceId != null && !traceId.trim().isEmpty()) {
            MDC.put(TRACE_ID, traceId);
        }
    }

    /**
     * Set span ID in MDC (for distributed tracing)
     */
    public void setSpanId(String spanId) {
        if (spanId != null && !spanId.trim().isEmpty()) {
            MDC.put(SPAN_ID, spanId);
        }
    }

    /**
     * Set user ID in MDC for user-specific operations
     */
    public void setUserId(String userId) {
        if (userId != null && !userId.trim().isEmpty()) {
            MDC.put(USER_ID, userId);
        }
    }

    /**
     * Set customer ID in MDC for customer-specific operations
     */
    public void setCustomerId(String customerId) {
        if (customerId != null && !customerId.trim().isEmpty()) {
            MDC.put(CUSTOMER_ID, customerId);
        }
    }

    /**
     * Set order ID in MDC for order-specific operations
     */
    public void setOrderId(String orderId) {
        if (orderId != null && !orderId.trim().isEmpty()) {
            MDC.put(ORDER_ID, orderId);
        }
    }

    /**
     * Set operation name in MDC
     */
    public void setOperation(String operation) {
        if (operation != null && !operation.trim().isEmpty()) {
            MDC.put(OPERATION, operation);
        }
    }

    /**
     * Set component name in MDC
     */
    public void setComponent(String component) {
        if (component != null && !component.trim().isEmpty()) {
            MDC.put(COMPONENT, component);
        }
    }

    /**
     * Set business context for domain operations
     */
    public void setBusinessContext(String customerId, String orderId, String operation) {
        setCustomerId(customerId);
        setOrderId(orderId);
        setOperation(operation);
    }

    /**
     * Set multiple MDC values at once
     */
    public void setContext(Map<String, String> contextMap) {
        contextMap.forEach((key, value) -> {
            if (value != null && !value.trim().isEmpty()) {
                MDC.put(key, value);
            }
        });
    }

    /**
     * Get correlation ID from MDC
     */
    public Optional<String> getCorrelationId() {
        return Optional.ofNullable(MDC.get(CORRELATION_ID));
    }

    /**
     * Get trace ID from MDC
     */
    public Optional<String> getTraceId() {
        return Optional.ofNullable(MDC.get(TRACE_ID));
    }

    /**
     * Get current MDC context as a map
     */
    public Map<String, String> getCurrentContext() {
        return MDC.getCopyOfContextMap();
    }

    /**
     * Clear specific MDC key
     */
    public void clearKey(String key) {
        MDC.remove(key);
    }

    /**
     * Clear all MDC context
     */
    public void clearAll() {
        MDC.clear();
    }

    /**
     * Execute a runnable with specific MDC context
     */
    public void executeWithContext(Map<String, String> context, Runnable runnable) {
        Map<String, String> originalContext = MDC.getCopyOfContextMap();
        try {
            setContext(context);
            runnable.run();
        } finally {
            MDC.clear();
            if (originalContext != null) {
                MDC.setContextMap(originalContext);
            }
        }
    }
}