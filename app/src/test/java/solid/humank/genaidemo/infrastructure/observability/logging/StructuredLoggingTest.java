package solid.humank.genaidemo.infrastructure.observability.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test class for structured logging functionality including MDC context
 * management
 * and correlation ID handling.
 */
@SpringBootTest
@ActiveProfiles("test")
class StructuredLoggingTest {

    private static final Logger logger = LoggerFactory.getLogger(StructuredLoggingTest.class);
    private LoggingContextManager loggingContextManager;

    @BeforeEach
    void setUp() {
        loggingContextManager = new LoggingContextManager();
        MDC.clear(); // Clean MDC before each test
    }

    @Test
    @DisplayName("Should set and retrieve correlation ID in MDC")
    void shouldSetAndRetrieveCorrelationId() {
        // Given
        String correlationId = UUID.randomUUID().toString();

        // When
        loggingContextManager.setCorrelationId(correlationId);

        // Then
        assertThat(loggingContextManager.getCorrelationId())
                .isPresent()
                .contains(correlationId);
        assertThat(MDC.get(LoggingContextManager.CORRELATION_ID))
                .isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Should set business context with customer and order IDs")
    void shouldSetBusinessContext() {
        // Given
        String customerId = "CUST-001";
        String orderId = "ORDER-123";
        String operation = "createOrder";

        // When
        loggingContextManager.setBusinessContext(customerId, orderId, operation);

        // Then
        assertThat(MDC.get(LoggingContextManager.CUSTOMER_ID)).isEqualTo(customerId);
        assertThat(MDC.get(LoggingContextManager.ORDER_ID)).isEqualTo(orderId);
        assertThat(MDC.get(LoggingContextManager.OPERATION)).isEqualTo(operation);
    }

    @Test
    @DisplayName("Should set multiple MDC values at once")
    void shouldSetMultipleMdcValues() {
        // Given
        Map<String, String> contextMap = Map.of(
                LoggingContextManager.USER_ID, "USER-001",
                LoggingContextManager.COMPONENT, "order-service",
                LoggingContextManager.OPERATION, "processPayment");

        // When
        loggingContextManager.setContext(contextMap);

        // Then
        assertThat(MDC.get(LoggingContextManager.USER_ID)).isEqualTo("USER-001");
        assertThat(MDC.get(LoggingContextManager.COMPONENT)).isEqualTo("order-service");
        assertThat(MDC.get(LoggingContextManager.OPERATION)).isEqualTo("processPayment");
    }

    @Test
    @DisplayName("Should execute runnable with specific context")
    void shouldExecuteWithContext() {
        // Given
        Map<String, String> context = Map.of(
                LoggingContextManager.CORRELATION_ID, "test-correlation-id",
                LoggingContextManager.OPERATION, "testOperation");

        // When
        loggingContextManager.executeWithContext(context, () -> {
            // Then - inside the context
            assertThat(MDC.get(LoggingContextManager.CORRELATION_ID))
                    .isEqualTo("test-correlation-id");
            assertThat(MDC.get(LoggingContextManager.OPERATION))
                    .isEqualTo("testOperation");

            logger.info("Test log message with context");
        });

        // Then - context should be cleared after execution
        assertThat(MDC.get(LoggingContextManager.CORRELATION_ID)).isNull();
        assertThat(MDC.get(LoggingContextManager.OPERATION)).isNull();
    }

    @Test
    @DisplayName("Should get current MDC context as map")
    void shouldGetCurrentContext() {
        // Given
        loggingContextManager.setCorrelationId("test-correlation");
        loggingContextManager.setOperation("testOperation");
        loggingContextManager.setComponent("test-component");

        // When
        Map<String, String> currentContext = loggingContextManager.getCurrentContext();

        // Then
        assertThat(currentContext)
                .containsEntry(LoggingContextManager.CORRELATION_ID, "test-correlation")
                .containsEntry(LoggingContextManager.OPERATION, "testOperation")
                .containsEntry(LoggingContextManager.COMPONENT, "test-component");
    }

    @Test
    @DisplayName("Should clear specific MDC key")
    void shouldClearSpecificKey() {
        // Given
        loggingContextManager.setCorrelationId("test-correlation");
        loggingContextManager.setOperation("testOperation");

        // When
        loggingContextManager.clearKey(LoggingContextManager.CORRELATION_ID);

        // Then
        assertThat(MDC.get(LoggingContextManager.CORRELATION_ID)).isNull();
        assertThat(MDC.get(LoggingContextManager.OPERATION)).isEqualTo("testOperation");
    }

    @Test
    @DisplayName("Should clear all MDC context")
    void shouldClearAllContext() {
        // Given
        loggingContextManager.setCorrelationId("test-correlation");
        loggingContextManager.setOperation("testOperation");
        loggingContextManager.setComponent("test-component");

        // When
        loggingContextManager.clearAll();

        // Then
        assertThat(MDC.get(LoggingContextManager.CORRELATION_ID)).isNull();
        assertThat(MDC.get(LoggingContextManager.OPERATION)).isNull();
        assertThat(MDC.get(LoggingContextManager.COMPONENT)).isNull();
    }

    @Test
    @DisplayName("Should handle null and empty values gracefully")
    void shouldHandleNullAndEmptyValues() {
        // When
        loggingContextManager.setCorrelationId(null);
        loggingContextManager.setOperation("");
        loggingContextManager.setComponent("   ");

        // Then
        assertThat(MDC.get(LoggingContextManager.CORRELATION_ID)).isNull();
        assertThat(MDC.get(LoggingContextManager.OPERATION)).isNull();
        assertThat(MDC.get(LoggingContextManager.COMPONENT)).isNull();
    }

    @Test
    @DisplayName("Should demonstrate structured logging with business context")
    void shouldDemonstrateStructuredLogging() {
        // Given
        String correlationId = UUID.randomUUID().toString();
        String customerId = "CUST-001";
        String orderId = "ORDER-123";

        // When
        loggingContextManager.setCorrelationId(correlationId);
        loggingContextManager.setBusinessContext(customerId, orderId, "createOrder");

        // Log messages that will include the MDC context
        logger.info("Starting order creation process");
        logger.debug("Validating customer: {}", customerId);
        logger.info("Order created successfully with ID: {}", orderId);

        // Then - verify context is available
        assertThat(loggingContextManager.getCorrelationId()).isPresent();
        assertThat(MDC.get(LoggingContextManager.CUSTOMER_ID)).isEqualTo(customerId);
        assertThat(MDC.get(LoggingContextManager.ORDER_ID)).isEqualTo(orderId);
    }
}