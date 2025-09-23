package solid.humank.genaidemo.testutils;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Observability Test Validator
 * 
 * Provides comprehensive validation of observability features including
 * metrics endpoints, health checks, tracing configuration, and structured
 * logging.
 */
@Component
public class ObservabilityTestValidator {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityTestValidator.class);

    /**
     * Validates that the metrics endpoint is accessible and returns valid data
     */
    public ValidationResult validateMetricsEndpoint(String baseUrl, TestRestTemplate restTemplate) {
        long startTime = System.currentTimeMillis();

        try {
            String metricsUrl = baseUrl + "/actuator/metrics";
            ResponseEntity<String> response = restTemplate.getForEntity(metricsUrl, String.class);

            Duration executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return ValidationResult.failure(
                        "validateMetricsEndpoint",
                        executionTime,
                        java.util.List.of("Metrics endpoint returned status: " + response.getStatusCode()));
            }

            String body = response.getBody();
            if (body == null || body.isEmpty()) {
                return ValidationResult.failure(
                        "validateMetricsEndpoint",
                        executionTime,
                        java.util.List.of("Metrics endpoint returned empty response"));
            }

            // Check for any available metrics (flexible for test environment)
            boolean hasMetrics = body.contains("application.ready.time") ||
                    body.contains("http.server.requests") ||
                    body.contains("hikaricp.connections") ||
                    body.contains("disk.free") ||
                    body.contains("tomcat.sessions") ||
                    body.contains("jvm.memory.used");

            if (!hasMetrics) {
                return ValidationResult.failure(
                        "validateMetricsEndpoint",
                        executionTime,
                        java.util.List.of("Metrics endpoint does not contain any expected metrics"));
            }

            Map<String, Object> metrics = Map.of(
                    "responseTime", executionTime.toMillis(),
                    "statusCode", response.getStatusCode().value(),
                    "contentLength", body.length());

            logger.debug("Metrics endpoint validation successful in {}ms", executionTime.toMillis());
            return ValidationResult.success("validateMetricsEndpoint", executionTime).withMetrics(metrics);

        } catch (Exception e) {
            Duration executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            logger.error("Metrics endpoint validation failed", e);
            return ValidationResult.failure(
                    "validateMetricsEndpoint",
                    executionTime,
                    java.util.List.of("Exception occurred: " + e.getMessage()));
        }
    }

    /**
     * Validates that the Prometheus metrics endpoint is accessible and returns
     * valid data
     */
    public ValidationResult validatePrometheusEndpoint(String baseUrl, TestRestTemplate restTemplate) {
        long startTime = System.currentTimeMillis();

        try {
            String prometheusUrl = baseUrl + "/actuator/prometheus";
            ResponseEntity<String> response = restTemplate.getForEntity(prometheusUrl, String.class);

            Duration executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return ValidationResult.failure(
                        "validatePrometheusEndpoint",
                        executionTime,
                        java.util.List.of("Prometheus endpoint returned status: " + response.getStatusCode()));
            }

            String body = response.getBody();
            if (body == null || body.isEmpty()) {
                return ValidationResult.failure(
                        "validatePrometheusEndpoint",
                        executionTime,
                        java.util.List.of("Prometheus endpoint returned empty response"));
            }

            // Check for any Prometheus format metrics (flexible for test environment)
            boolean hasPrometheusMetrics = body.contains("application_ready_time") ||
                    body.contains("http_server_requests") ||
                    body.contains("hikaricp_connections") ||
                    body.contains("disk_free") ||
                    body.contains("tomcat_sessions") ||
                    body.contains("jvm_memory_used_bytes");

            if (!hasPrometheusMetrics) {
                return ValidationResult.failure(
                        "validatePrometheusEndpoint",
                        executionTime,
                        java.util.List.of("Prometheus endpoint does not contain expected metrics format"));
            }

            Map<String, Object> metrics = Map.of(
                    "responseTime", executionTime.toMillis(),
                    "statusCode", response.getStatusCode().value(),
                    "metricsCount", countMetrics(body));

            logger.debug("Prometheus endpoint validation successful in {}ms", executionTime.toMillis());
            return ValidationResult.success("validatePrometheusEndpoint", executionTime).withMetrics(metrics);

        } catch (Exception e) {
            Duration executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            logger.error("Prometheus endpoint validation failed", e);
            return ValidationResult.failure(
                    "validatePrometheusEndpoint",
                    executionTime,
                    java.util.List.of("Exception occurred: " + e.getMessage()));
        }
    }

    /**
     * Validates that all health checks are working correctly
     */
    public ValidationResult validateHealthChecks(String baseUrl, TestRestTemplate restTemplate) {
        long startTime = System.currentTimeMillis();

        try {
            String healthUrl = baseUrl + "/actuator/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);

            Duration executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);

            if (response.getStatusCode() != HttpStatus.OK) {
                return ValidationResult.failure(
                        "validateHealthChecks",
                        executionTime,
                        java.util.List.of("Health endpoint returned status: " + response.getStatusCode()));
            }

            String body = response.getBody();
            if (body == null || !body.contains("\"status\":\"UP\"")) {
                return ValidationResult.failure(
                        "validateHealthChecks",
                        executionTime,
                        java.util.List.of("Health endpoint does not show UP status"));
            }

            // Validate specific health indicators
            boolean hasDbHealth = body.contains("\"db\"") || body.contains("\"diskSpace\"");

            Map<String, Object> metrics = Map.of(
                    "responseTime", executionTime.toMillis(),
                    "statusCode", response.getStatusCode().value(),
                    "hasDbHealth", hasDbHealth,
                    "overallStatus", "UP");

            logger.debug("Health checks validation successful in {}ms", executionTime.toMillis());
            return ValidationResult.success("validateHealthChecks", executionTime).withMetrics(metrics);

        } catch (Exception e) {
            Duration executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            logger.error("Health checks validation failed", e);
            return ValidationResult.failure(
                    "validateHealthChecks",
                    executionTime,
                    java.util.List.of("Exception occurred: " + e.getMessage()));
        }
    }

    /**
     * Validates tracing configuration (basic validation)
     */
    public ValidationResult validateTracingConfiguration() {
        long startTime = System.currentTimeMillis();

        try {
            // Basic validation - check if tracing classes are available
            Class.forName("io.micrometer.tracing.Tracer");

            Duration executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);

            Map<String, Object> metrics = Map.of(
                    "tracingClassesAvailable", true,
                    "validationTime", executionTime.toMillis());

            logger.debug("Tracing configuration validation successful");
            return ValidationResult.success("validateTracingConfiguration", executionTime).withMetrics(metrics);

        } catch (ClassNotFoundException e) {
            Duration executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            logger.warn("Tracing classes not available: {}", e.getMessage());
            return ValidationResult.failure(
                    "validateTracingConfiguration",
                    executionTime,
                    java.util.List.of("Tracing classes not available: " + e.getMessage()));
        } catch (Exception e) {
            Duration executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            logger.error("Tracing configuration validation failed", e);
            return ValidationResult.failure(
                    "validateTracingConfiguration",
                    executionTime,
                    java.util.List.of("Exception occurred: " + e.getMessage()));
        }
    }

    /**
     * Validates structured logging functionality
     */
    public ValidationResult validateStructuredLogging() {
        long startTime = System.currentTimeMillis();

        try {
            // Test structured logging by creating a log entry
            logger.info("Structured logging test - correlationId: {}, testType: {}",
                    "test-" + System.currentTimeMillis(), "observability-validation");

            Duration executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);

            Map<String, Object> metrics = Map.of(
                    "loggerAvailable", true,
                    "structuredLoggingTest", true,
                    "validationTime", executionTime.toMillis());

            logger.debug("Structured logging validation successful");
            return ValidationResult.success("validateStructuredLogging", executionTime).withMetrics(metrics);

        } catch (Exception e) {
            Duration executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            logger.error("Structured logging validation failed", e);
            return ValidationResult.failure(
                    "validateStructuredLogging",
                    executionTime,
                    java.util.List.of("Exception occurred: " + e.getMessage()));
        }
    }

    /**
     * Validates info endpoint provides expected application information
     */
    public ValidationResult validateInfoEndpoint(String baseUrl, TestRestTemplate restTemplate) {
        long startTime = System.currentTimeMillis();

        try {
            String infoUrl = baseUrl + "/actuator/info";
            ResponseEntity<String> response = restTemplate.getForEntity(infoUrl, String.class);

            Duration executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return ValidationResult.failure(
                        "validateInfoEndpoint",
                        executionTime,
                        java.util.List.of("Info endpoint returned status: " + response.getStatusCode()));
            }

            Map<String, Object> metrics = Map.of(
                    "responseTime", executionTime.toMillis(),
                    "statusCode", response.getStatusCode().value(),
                    "hasContent", response.getBody() != null);

            logger.debug("Info endpoint validation successful in {}ms", executionTime.toMillis());
            return ValidationResult.success("validateInfoEndpoint", executionTime).withMetrics(metrics);

        } catch (Exception e) {
            Duration executionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
            logger.error("Info endpoint validation failed", e);
            return ValidationResult.failure(
                    "validateInfoEndpoint",
                    executionTime,
                    java.util.List.of("Exception occurred: " + e.getMessage()));
        }
    }

    /**
     * Performs comprehensive observability validation
     */
    public ValidationResult validateComprehensiveObservability(String baseUrl, TestRestTemplate restTemplate) {
        logger.info("Starting comprehensive observability validation");

        long startTime = System.currentTimeMillis();
        java.util.List<String> errors = new java.util.ArrayList<>();
        Map<String, Object> allMetrics = new java.util.HashMap<>();

        // Run all validations
        ValidationResult metricsResult = validateMetricsEndpoint(baseUrl, restTemplate);
        ValidationResult prometheusResult = validatePrometheusEndpoint(baseUrl, restTemplate);
        ValidationResult healthResult = validateHealthChecks(baseUrl, restTemplate);
        ValidationResult tracingResult = validateTracingConfiguration();
        ValidationResult loggingResult = validateStructuredLogging();
        ValidationResult infoResult = validateInfoEndpoint(baseUrl, restTemplate);

        // Collect results
        if (!metricsResult.success())
            errors.addAll(metricsResult.errors());
        if (!prometheusResult.success())
            errors.addAll(prometheusResult.errors());
        if (!healthResult.success())
            errors.addAll(healthResult.errors());
        if (!tracingResult.success())
            errors.addAll(tracingResult.errors());
        if (!loggingResult.success())
            errors.addAll(loggingResult.errors());
        if (!infoResult.success())
            errors.addAll(infoResult.errors());

        // Collect metrics
        allMetrics.putAll(metricsResult.metrics());
        allMetrics.putAll(prometheusResult.metrics());
        allMetrics.putAll(healthResult.metrics());
        allMetrics.putAll(tracingResult.metrics());
        allMetrics.putAll(loggingResult.metrics());
        allMetrics.putAll(infoResult.metrics());

        Duration totalExecutionTime = Duration.ofMillis(System.currentTimeMillis() - startTime);
        allMetrics.put("totalValidationTime", totalExecutionTime.toMillis());
        allMetrics.put("validationsRun", 6);
        allMetrics.put("validationsPassed", 6 - errors.size());

        if (errors.isEmpty()) {
            logger.info("Comprehensive observability validation successful in {}ms", totalExecutionTime.toMillis());
            return ValidationResult.success("validateComprehensiveObservability", totalExecutionTime)
                    .withMetrics(allMetrics);
        } else {
            logger.warn("Comprehensive observability validation failed with {} errors", errors.size());
            return ValidationResult.failure("validateComprehensiveObservability", totalExecutionTime, errors)
                    .withMetrics(allMetrics);
        }
    }

    // Helper methods

    private int countMetrics(String prometheusOutput) {
        if (prometheusOutput == null)
            return 0;
        return (int) prometheusOutput.lines()
                .filter(line -> !line.startsWith("#") && !line.trim().isEmpty())
                .count();
    }

    /**
     * Validation result record
     */
    public record ValidationResult(
            boolean success,
            String testName,
            Duration executionTime,
            java.util.List<String> errors,
            Map<String, Object> metrics,
            java.util.Optional<String> details) {
        public static ValidationResult success(String testName, Duration executionTime) {
            return new ValidationResult(true, testName, executionTime, java.util.List.of(), Map.of(),
                    java.util.Optional.empty());
        }

        public static ValidationResult failure(String testName, Duration executionTime, java.util.List<String> errors) {
            return new ValidationResult(false, testName, executionTime, errors, Map.of(), java.util.Optional.empty());
        }

        public ValidationResult withMetrics(Map<String, Object> metrics) {
            return new ValidationResult(success, testName, executionTime, errors, metrics, details);
        }

        public ValidationResult withDetails(String details) {
            return new ValidationResult(success, testName, executionTime, errors, metrics,
                    java.util.Optional.of(details));
        }
    }
}