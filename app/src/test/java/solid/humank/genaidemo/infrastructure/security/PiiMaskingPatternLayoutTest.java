package solid.humank.genaidemo.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;

/**
 * Unit tests for PII masking in log messages
 * Tests requirement 11.1: WHEN logs contain sensitive data THEN the system
 * SHALL mask or encrypt PII information
 */
class PiiMaskingPatternLayoutTest {

    private PiiMaskingPatternLayout layout;
    private LoggerContext loggerContext;
    private Logger logger;

    @BeforeEach
    void setUp() {
        layout = new PiiMaskingPatternLayout();
        layout.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");

        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        layout.setContext(loggerContext);
        layout.start();

        logger = loggerContext.getLogger("test");
    }

    @Test
    void shouldMaskEmailAddresses() {
        // Given
        LoggingEvent event = createLoggingEvent("User email: john.doe@example.com registered successfully");

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("jo***@example.com");
        assertThat(result).doesNotContain("john.doe@example.com");
    }

    @Test
    void shouldMaskPhoneNumbers() {
        // Given
        LoggingEvent event = createLoggingEvent("Customer phone: 555-123-4567 updated");

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("555-***-****");
        assertThat(result).doesNotContain("555-123-4567");
    }

    @Test
    void shouldMaskCreditCardNumbers() {
        // Given
        LoggingEvent event = createLoggingEvent("Payment with card: 4532-1234-5678-9012 processed");

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("****-****-****-9012");
        assertThat(result).doesNotContain("4532-1234-5678-9012");
    }

    @Test
    void shouldMaskSsnNumbers() {
        // Given
        LoggingEvent event = createLoggingEvent("SSN: 123-45-6789 verified");

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("***-**-****");
        assertThat(result).doesNotContain("123-45-6789");
    }

    @Test
    void shouldMaskAddresses() {
        // Given
        LoggingEvent event = createLoggingEvent("Address: 123 Main Street updated");

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("123 *** *** ***");
        assertThat(result).doesNotContain("123 Main Street");
    }

    @Test
    void shouldMaskSensitiveJsonFields() {
        // Given
        String jsonLog = "{\"username\":\"john\",\"password\":\"secret123\",\"creditCard\":\"4532123456789012\"}";
        LoggingEvent event = createLoggingEvent(jsonLog);

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("\"password\":\"***MASKED***\"");
        assertThat(result).contains("\"creditCard\":\"***MASKED***\"");
        assertThat(result).doesNotContain("secret123");
        assertThat(result).doesNotContain("4532123456789012");
    }

    @Test
    void shouldHandleMultiplePiiTypesInSameMessage() {
        // Given
        LoggingEvent event = createLoggingEvent(
                "User john.doe@example.com with phone 555-123-4567 and SSN 123-45-6789");

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("jo***@example.com");
        assertThat(result).contains("555-***-****");
        assertThat(result).contains("***-**-****");
        assertThat(result).doesNotContain("john.doe@example.com");
        assertThat(result).doesNotContain("555-123-4567");
        assertThat(result).doesNotContain("123-45-6789");
    }

    @Test
    void shouldHandleNullAndEmptyMessages() {
        // Given
        LoggingEvent nullEvent = createLoggingEvent(null);
        LoggingEvent emptyEvent = createLoggingEvent("");

        // When
        String nullResult = layout.doLayout(nullEvent);
        String emptyResult = layout.doLayout(emptyEvent);

        // Then
        assertThat(nullResult).isNotNull();
        assertThat(emptyResult).isNotNull();
    }

    @Test
    void shouldNotMaskNonSensitiveData() {
        // Given
        LoggingEvent event = createLoggingEvent("Order ID: ORD-12345 processed successfully");

        // When
        String result = layout.doLayout(event);

        // Then
        assertThat(result).contains("ORD-12345");
    }

    private LoggingEvent createLoggingEvent(String message) {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(loggerContext);
        event.setLoggerName("test");
        event.setLevel(Level.INFO);
        event.setMessage(message);
        event.setTimeStamp(System.currentTimeMillis());
        return event;
    }
}