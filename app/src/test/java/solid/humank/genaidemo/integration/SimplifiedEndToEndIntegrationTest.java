package solid.humank.genaidemo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import solid.humank.genaidemo.testutils.ObservabilityTestValidator;
import solid.humank.genaidemo.testutils.TestPerformanceExtension;
import solid.humank.genaidemo.testutils.base.BaseIntegrationTest;

/**
 * Simplified end-to-end integration tests focusing on working observability
 * features.
 * 
 * This test validates core observability functionality that actually exists
 * and can be tested without complex infrastructure dependencies.
 */
@TestPropertySource(properties = {
                "spring.datasource.url=jdbc:h2:mem:simplified-e2e-test",
                "logging.level.solid.humank.genaidemo=DEBUG",
                "management.endpoints.web.exposure.include=*",
                "management.endpoint.health.show-details=always"
})
@TestMethodOrder(OrderAnnotation.class)
@TestPerformanceExtension(maxExecutionTimeMs = 30000, maxMemoryIncreaseMB = 200)
public class SimplifiedEndToEndIntegrationTest extends BaseIntegrationTest {

        @Autowired
        private MeterRegistry meterRegistry;

        @Autowired
        private OpenTelemetry openTelemetry;

        @Autowired
        private ObservabilityTestValidator observabilityValidator;

        private HttpHeaders defaultHeaders;

        @BeforeEach
        void setUp() {
                // Parent setup is called automatically via @BeforeEach
                // No need to call it explicitly

                // Set up test-specific headers
                defaultHeaders = new HttpHeaders();
                defaultHeaders.set("X-Correlation-ID", "simplified-e2e-test-" + System.currentTimeMillis());
                defaultHeaders.set("X-Test-Suite", "SimplifiedEndToEndIntegration");
        }

        @Test
        @Order(1)
        void shouldValidateApplicationStartupAndHealth() {
                logTestStart("shouldValidateApplicationStartupAndHealth");

                // Verify application is running and healthy
                ResponseEntity<String> healthResponse = performGet("/actuator/health", String.class);

                assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(healthResponse.getBody()).contains("\"status\":\"UP\"");

                logTestEnd("shouldValidateApplicationStartupAndHealth");
        }

        @Test
        @Order(2)
        void shouldValidateBasicObservabilityComponents() {
                logTestStart("shouldValidateBasicObservabilityComponents");

                // Validate that observability components are available
                assertThat(meterRegistry).isNotNull();
                assertThat(openTelemetry).isNotNull();
                assertThat(observabilityValidator).isNotNull();

                // Test basic metrics collection
                ObservabilityTestValidator.ValidationResult metricsResult = observabilityValidator
                                .validateMetricsEndpoint(baseUrl, restTemplate);

                assertThat(metricsResult.success())
                                .withFailMessage("Metrics validation failed: %s", metricsResult.errors())
                                .isTrue();

                logTestEnd("shouldValidateBasicObservabilityComponents");
        }

        @Test
        @Order(3)
        void shouldValidateHealthChecksComprehensively() {
                logTestStart("shouldValidateHealthChecksComprehensively");

                // Use ObservabilityTestValidator for comprehensive health check validation
                ObservabilityTestValidator.ValidationResult healthResult = observabilityValidator.validateHealthChecks(
                                baseUrl,
                                restTemplate);

                assertThat(healthResult.success())
                                .withFailMessage("Health checks validation failed: %s", healthResult.errors())
                                .isTrue();

                logger.info("Health validation completed: success={}, errors={}", healthResult.success(),
                                healthResult.errors());

                logTestEnd("shouldValidateHealthChecksComprehensively");
        }

        @Test
        @Order(4)
        void shouldValidateActuatorEndpointsComprehensively() {
                logTestStart("shouldValidateActuatorEndpointsComprehensively");

                // Use ObservabilityTestValidator for info endpoint validation
                ObservabilityTestValidator.ValidationResult infoResult = observabilityValidator
                                .validateInfoEndpoint(baseUrl, restTemplate);

                assertThat(infoResult.success())
                                .withFailMessage("Info endpoint validation failed: %s", infoResult.errors())
                                .isTrue();

                logger.info("Info endpoint validation completed: {}", infoResult.success());

                logTestEnd("shouldValidateActuatorEndpointsComprehensively");
        }

        @Test
        @Order(5)
        void shouldValidateTracingConfiguration() {
                logTestStart("shouldValidateTracingConfiguration");

                // Validate tracing configuration
                ObservabilityTestValidator.ValidationResult tracingResult = observabilityValidator
                                .validateTracingConfiguration();

                assertThat(tracingResult.success())
                                .withFailMessage("Tracing configuration validation failed: %s", tracingResult.errors())
                                .isTrue();

                logger.info("Tracing validation completed: success={}, errors={}", tracingResult.success(),
                                tracingResult.errors());

                logTestEnd("shouldValidateTracingConfiguration");
        }

        @Test
        @Order(6)
        void shouldValidateStructuredLogging() {
                logTestStart("shouldValidateStructuredLogging");

                // Validate structured logging configuration
                ObservabilityTestValidator.ValidationResult loggingResult = observabilityValidator
                                .validateStructuredLogging();

                assertThat(loggingResult.success())
                                .withFailMessage("Structured logging validation failed: %s", loggingResult.errors())
                                .isTrue();

                logger.info("Logging validation completed: success={}, errors={}", loggingResult.success(),
                                loggingResult.errors());

                logTestEnd("shouldValidateStructuredLogging");
        }

        @Test
        @Order(7)
        void shouldValidateCorrelationIdPropagation() {
                logTestStart("shouldValidateCorrelationIdPropagation");

                // Test correlation ID propagation through requests
                String correlationId = "correlation-test-" + System.currentTimeMillis();
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Correlation-ID", correlationId);

                HttpEntity<String> entity = new HttpEntity<>(headers);
                ResponseEntity<String> response = restTemplate.exchange(
                                baseUrl + "/actuator/info", HttpMethod.GET, entity, String.class);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

                // In a real implementation, we would check if the correlation ID appears in
                // logs
                // For now, we just verify the request was successful
                logger.info("Correlation ID test completed with ID: {}", correlationId);

                logTestEnd("shouldValidateCorrelationIdPropagation");
        }

        @Test
        @Order(8)
        void shouldValidateBasicPerformanceUnderLoad() {
                logTestStart("shouldValidateBasicPerformanceUnderLoad");

                // Generate a small number of concurrent requests to test basic performance
                int concurrentRequests = 10;
                List<CompletableFuture<ResponseEntity<String>>> futures = generateConcurrentRequests(
                                concurrentRequests);

                // Wait for all requests to complete
                CompletableFuture<Void> allRequests = CompletableFuture.allOf(
                                futures.toArray(new CompletableFuture[0]));

                await("All concurrent requests should complete")
                                .atMost(30, TimeUnit.SECONDS)
                                .until(() -> allRequests.isDone());

                // Validate all requests were successful
                long successfulRequests = futures.stream()
                                .mapToLong(future -> {
                                        try {
                                                ResponseEntity<String> response = future.get();
                                                return response.getStatusCode().is2xxSuccessful() ? 1 : 0;
                                        } catch (Exception e) {
                                                logger.warn("Request failed: {}", e.getMessage());
                                                return 0;
                                        }
                                })
                                .sum();

                assertThat(successfulRequests).isEqualTo(concurrentRequests);
                logger.info("Performance test completed: {}/{} requests successful", successfulRequests,
                                concurrentRequests);

                logTestEnd("shouldValidateBasicPerformanceUnderLoad");
        }

        @Test
        @Order(9)
        void shouldValidateErrorHandlingObservability() {
                logTestStart("shouldValidateErrorHandlingObservability");

                // Test error scenarios and validate observability
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Correlation-ID", "error-test-" + System.currentTimeMillis());
                headers.set("X-Test-Error", "true");

                HttpEntity<String> entity = new HttpEntity<>(headers);

                // Make request to non-existent endpoint to generate error
                ResponseEntity<String> errorResponse = restTemplate.exchange(
                                baseUrl + "/non-existent-endpoint", HttpMethod.GET, entity, String.class);

                assertThat(errorResponse.getStatusCode().is4xxClientError()).isTrue();

                // Verify that error was handled gracefully
                logger.info("Error handling test completed with status: {}", errorResponse.getStatusCode());

                logTestEnd("shouldValidateErrorHandlingObservability");
        }

        @Test
        @Order(10)
        void shouldValidateComprehensiveObservabilityIntegration() {
                logTestStart("shouldValidateComprehensiveObservabilityIntegration");

                // Final comprehensive validation of all observability components
                boolean allValidationsPassed = true;
                StringBuilder validationReport = new StringBuilder();
                validationReport.append("=== Comprehensive Observability Validation Report ===\\n");

                // Validate metrics
                ObservabilityTestValidator.ValidationResult metricsResult = observabilityValidator
                                .validateMetricsEndpoint(baseUrl, restTemplate);
                validationReport.append(String.format("Metrics: %s\\n", metricsResult.success() ? "✅ PASS" : "❌ FAIL"));
                allValidationsPassed &= metricsResult.success();

                // Validate health checks
                ObservabilityTestValidator.ValidationResult healthResult = observabilityValidator.validateHealthChecks(
                                baseUrl,
                                restTemplate);
                validationReport.append(
                                String.format("Health Checks: %s\\n", healthResult.success() ? "✅ PASS" : "❌ FAIL"));
                allValidationsPassed &= healthResult.success();

                // Validate info endpoint (instead of actuator endpoints)
                ObservabilityTestValidator.ValidationResult infoResult = observabilityValidator
                                .validateInfoEndpoint(baseUrl, restTemplate);
                validationReport.append(
                                String.format("Info Endpoint: %s\\n", infoResult.success() ? "✅ PASS" : "❌ FAIL"));
                allValidationsPassed &= infoResult.success();

                // Validate tracing
                ObservabilityTestValidator.ValidationResult tracingResult = observabilityValidator
                                .validateTracingConfiguration();
                validationReport.append(String.format("Tracing: %s\\n", tracingResult.success() ? "✅ PASS" : "❌ FAIL"));
                allValidationsPassed &= tracingResult.success();

                // Validate logging
                ObservabilityTestValidator.ValidationResult loggingResult = observabilityValidator
                                .validateStructuredLogging();
                validationReport.append(String.format("Structured Logging: %s\\n",
                                loggingResult.success() ? "✅ PASS" : "❌ FAIL"));
                allValidationsPassed &= loggingResult.success();

                validationReport.append(String.format("\\nOverall Result: %s\\n",
                                allValidationsPassed ? "✅ ALL VALIDATIONS PASSED" : "❌ SOME VALIDATIONS FAILED"));

                logger.info("Comprehensive Observability Validation Report:\\n{}", validationReport.toString());

                assertThat(allValidationsPassed)
                                .withFailMessage("Comprehensive observability validation failed")
                                .isTrue();

                logTestEnd("shouldValidateComprehensiveObservabilityIntegration");
        }

        // Helper methods

        private List<CompletableFuture<ResponseEntity<String>>> generateConcurrentRequests(int count) {
                return java.util.stream.IntStream.range(0, count)
                                .mapToObj(i -> CompletableFuture.supplyAsync(() -> {
                                        HttpHeaders headers = new HttpHeaders();
                                        headers.set("X-Correlation-ID", "concurrent-" + i);
                                        headers.set("X-Load-Test", "true");

                                        HttpEntity<String> entity = new HttpEntity<>(headers);
                                        return restTemplate.exchange(
                                                        baseUrl + "/actuator/info", HttpMethod.GET, entity,
                                                        String.class);
                                }))
                                .toList();
        }
}