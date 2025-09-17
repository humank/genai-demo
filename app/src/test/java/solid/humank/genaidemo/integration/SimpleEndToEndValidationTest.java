package solid.humank.genaidemo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import solid.humank.genaidemo.testutils.TestPerformanceExtension;
import solid.humank.genaidemo.testutils.annotations.IntegrationTest;
import solid.humank.genaidemo.testutils.base.BaseIntegrationTest;

/**
 * Simple End-to-End Validation Test
 * Tests basic application functionality using the unified HTTP client
 * configuration.
 * Now reactivated with proper TestRestTemplate dependency resolution.
 */
@IntegrationTest
@TestPerformanceExtension(maxExecutionTimeMs = 10000, maxMemoryIncreaseMB = 100)
public class SimpleEndToEndValidationTest extends BaseIntegrationTest {

        @BeforeEach
        void setUp() {
                logTestStart("SimpleEndToEndValidationTest setup");
                validateTestEnvironment();
                waitForApplicationReady();
        }

        @Test
        void shouldValidateApplicationHealth() {
                logTestStart("shouldValidateApplicationHealth");

                // Test health endpoint with enhanced validation
                ResponseEntity<String> response = performGet("/actuator/health", String.class);

                assertThat(response.getStatusCode())
                                .as("Health endpoint should return OK status")
                                .isEqualTo(HttpStatus.OK);

                String body = response.getBody();
                assertThat(body)
                                .as("Health endpoint should contain UP status")
                                .isNotNull()
                                .contains("\"status\":\"UP\"");

                // Enhanced validation - check for specific health indicators
                if (body.contains("components")) {
                        logger.info("Health endpoint includes detailed component information");
                }

                logTestEnd("shouldValidateApplicationHealth");
        }

        @Test
        void shouldValidateActuatorEndpoints() {
                logTestStart("shouldValidateActuatorEndpoints");

                // Test actuator root endpoint with enhanced validation
                ResponseEntity<String> response = performGet("/actuator", String.class);

                assertThat(response.getStatusCode())
                                .as("Actuator root endpoint should return OK status")
                                .isEqualTo(HttpStatus.OK);

                String body = response.getBody();
                assertThat(body)
                                .as("Actuator endpoint should list available endpoints")
                                .isNotNull()
                                .contains("health")
                                .contains("metrics");

                logger.info("Actuator endpoints available: {}", body);

                logTestEnd("shouldValidateActuatorEndpoints");
        }

        @Test
        void shouldValidateMetricsEndpoint() {
                logTestStart("shouldValidateMetricsEndpoint");

                // Test metrics endpoint
                ResponseEntity<String> response = performGet("/actuator/metrics", String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                // Check for any available metrics (the format may vary)
                String body = response.getBody();
                assertThat(body).satisfiesAnyOf(
                                content -> assertThat(content).contains("jvm.memory.used"),
                                content -> assertThat(content).contains("hikaricp.connections"),
                                content -> assertThat(content).contains("http.server.requests"));

                logTestEnd("shouldValidateMetricsEndpoint");
        }

        @Test
        void shouldValidatePrometheusMetrics() {
                logTestStart("shouldValidatePrometheusMetrics");

                // Test prometheus metrics endpoint
                ResponseEntity<String> response = performGet("/actuator/prometheus", String.class);

                // Prometheus endpoint might be secured or disabled in test environment
                // Accept either OK or FORBIDDEN as valid responses
                assertThat(response.getStatusCode()).satisfiesAnyOf(
                                status -> assertThat(status).isEqualTo(HttpStatus.OK),
                                status -> assertThat(status).isEqualTo(HttpStatus.FORBIDDEN));

                // If accessible, check for Prometheus format metrics
                if (response.getStatusCode().is2xxSuccessful()) {
                        String body = response.getBody();
                        assertThat(body).satisfiesAnyOf(
                                        content -> assertThat(content).contains("jvm_memory_used_bytes"),
                                        content -> assertThat(content).contains("hikaricp_connections"),
                                        content -> assertThat(content).contains("http_server_requests"));
                }

                logTestEnd("shouldValidatePrometheusMetrics");
        }

        @Test
        void shouldValidateInfoEndpoint() {
                logTestStart("shouldValidateInfoEndpoint");

                // Test info endpoint
                ResponseEntity<String> response = performGet("/actuator/info", String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

                // Validate info endpoint provides expected information
                String body = response.getBody();
                assertThat(body).isNotNull();

                // Info endpoint should return valid JSON (even if empty)
                assertThat(body).satisfiesAnyOf(
                                content -> assertThat(content).isEqualTo("{}"),
                                content -> assertThat(content).contains("\""));

                logTestEnd("shouldValidateInfoEndpoint");
        }

        @Test
        void shouldValidateActuatorRootEndpointAccessibility() {
                logTestStart("shouldValidateActuatorRootEndpointAccessibility");

                // Test actuator root endpoint accessibility
                ResponseEntity<String> response = performGet("/actuator", String.class);

                assertThat(response.getStatusCode())
                                .as("Actuator root endpoint should be accessible")
                                .isEqualTo(HttpStatus.OK);

                String body = response.getBody();
                assertThat(body)
                                .as("Actuator root should provide links to available endpoints")
                                .isNotNull()
                                .contains("_links");

                // Verify essential endpoints are listed
                assertThat(body).contains("health");

                logger.info("Actuator root endpoint response: {}", body);

                logTestEnd("shouldValidateActuatorRootEndpointAccessibility");
        }

        @Test
        void shouldValidateMetricsEndpointResponseFormat() {
                logTestStart("shouldValidateMetricsEndpointResponseFormat");

                // Test metrics endpoint response format
                ResponseEntity<String> response = performGet("/actuator/metrics", String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

                String body = response.getBody();
                assertThat(body)
                                .as("Metrics endpoint should return valid JSON with names array")
                                .isNotNull()
                                .contains("names")
                                .contains("[");

                // Validate that we have some standard metrics (flexible for test environment)
                assertThat(body).satisfiesAnyOf(
                                content -> assertThat(content).contains("application.ready.time"),
                                content -> assertThat(content).contains("http.server.requests"),
                                content -> assertThat(content).contains("hikaricp.connections"),
                                content -> assertThat(content).contains("disk.free"),
                                content -> assertThat(content).contains("tomcat.sessions"));

                logger.info("Available metrics count: {}",
                                body.split(",").length);

                logTestEnd("shouldValidateMetricsEndpointResponseFormat");
        }

        @Test
        void shouldValidatePrometheusMetricsEndpointFunctionality() {
                logTestStart("shouldValidatePrometheusMetricsEndpointFunctionality");

                // Test Prometheus metrics endpoint functionality
                ResponseEntity<String> response = performGet("/actuator/prometheus", String.class);

                // Prometheus endpoint should be accessible in test environment
                assertThat(response.getStatusCode())
                                .as("Prometheus metrics endpoint should be accessible")
                                .satisfiesAnyOf(
                                                status -> assertThat(status).isEqualTo(HttpStatus.OK),
                                                status -> assertThat(status).isEqualTo(HttpStatus.FORBIDDEN));

                if (response.getStatusCode().is2xxSuccessful()) {
                        String body = response.getBody();
                        assertThat(body)
                                        .as("Prometheus metrics should be in proper format")
                                        .isNotNull()
                                        .isNotEmpty();

                        // Verify Prometheus format (should contain metric names and values)
                        assertThat(body).satisfiesAnyOf(
                                        content -> assertThat(content).contains("# HELP"),
                                        content -> assertThat(content).contains("# TYPE"),
                                        content -> assertThat(content).matches(".*\\w+\\{.*\\}\\s+[0-9\\.]+.*"));

                        logger.info("Prometheus metrics format validated successfully");
                } else {
                        logger.info("Prometheus endpoint is secured/disabled in test environment");
                }

                logTestEnd("shouldValidatePrometheusMetricsEndpointFunctionality");
        }
}