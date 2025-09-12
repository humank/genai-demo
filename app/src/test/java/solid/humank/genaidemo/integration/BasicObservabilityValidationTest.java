package solid.humank.genaidemo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import solid.humank.genaidemo.testutils.annotations.IntegrationTest;

/**
 * Basic Observability Validation Test
 * Tests logging, metrics, and basic observability features
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.security.user.name=test",
        "spring.security.user.password=test",
        "spring.security.user.roles=USER"
})
@ActiveProfiles("test")
@IntegrationTest
@org.springframework.context.annotation.Import({
        solid.humank.genaidemo.config.TestHttpClientConfiguration.class,
        solid.humank.genaidemo.config.TestWebMvcConfiguration.class
})
public class BasicObservabilityValidationTest {

    private static final Logger logger = LoggerFactory.getLogger(BasicObservabilityValidationTest.class);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    public void shouldValidateLoggingWithMDC() {
        // Test structured logging with MDC
        MDC.put("correlationId", "test-correlation-123");
        MDC.put("testId", "observability-test");

        logger.info("Testing structured logging with MDC");
        logger.warn("Testing warning level logging");
        logger.error("Testing error level logging");

        // Verify MDC context
        assertThat(MDC.get("correlationId")).isEqualTo("test-correlation-123");
        assertThat(MDC.get("testId")).isEqualTo("observability-test");

        MDC.clear();
    }

    @Test
    public void shouldValidateMetricsCollection() {
        // Test custom metrics creation
        Counter testCounter = Counter.builder("test.operations")
                .description("Test operations counter")
                .tag("test", "observability")
                .register(meterRegistry);

        Timer testTimer = Timer.builder("test.operation.duration")
                .description("Test operation duration")
                .tag("test", "observability")
                .register(meterRegistry);

        // Increment counter and record timer
        testCounter.increment();
        try {
            testTimer.recordCallable(() -> {
                Thread.sleep(10);
                return "test-result";
            });
        } catch (Exception e) {
            // Handle timer recording exception
            logger.warn("Timer recording failed", e);
        }

        // Verify metrics are recorded
        assertThat(testCounter.count()).isEqualTo(1.0);
        assertThat(testTimer.count()).isEqualTo(1);
    }

    @Test
    public void shouldValidateHealthIndicators() {
        // Test health endpoint with details
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    public void shouldValidateMetricsEndpoint() {
        // Test metrics endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/metrics", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jvm.memory.used");
        assertThat(response.getBody()).contains("http.server.requests");
    }

    @Test
    public void shouldValidatePrometheusMetrics() {
        // Test prometheus metrics format
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/prometheus", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("# HELP");
        assertThat(response.getBody()).contains("# TYPE");
        assertThat(response.getBody()).contains("jvm_memory_used_bytes");
    }

    @Test
    public void shouldValidateLoggersEndpoint() {
        // Test loggers endpoint for log level management
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/loggers", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("levels");
    }

    @Test
    public void shouldValidateEnvironmentEndpoint() {
        // Test environment endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/env", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("activeProfiles");
    }

    @Test
    public void shouldValidateConfigPropsEndpoint() {
        // Test configuration properties endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/configprops", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}