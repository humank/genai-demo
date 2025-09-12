package solid.humank.genaidemo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import solid.humank.genaidemo.infrastructure.cicd.CiCdPipelineValidator;
import solid.humank.genaidemo.infrastructure.config.MultiEnvironmentConfigValidator;
import solid.humank.genaidemo.infrastructure.disaster.DisasterRecoveryValidator;
import solid.humank.genaidemo.infrastructure.observability.ObservabilityIntegrationValidator;
import solid.humank.genaidemo.infrastructure.performance.LoadTestingValidator;
import solid.humank.genaidemo.testutils.annotations.IntegrationTest;
import solid.humank.genaidemo.testutils.base.BaseIntegrationTest;

/**
 * Comprehensive end-to-end integration tests validating all observability
 * features
 * and infrastructure components across multiple environments.
 */
@IntegrationTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:e2e-test",
        "logging.level.solid.humank.genaidemo=DEBUG",
        "management.endpoints.web.exposure.include=*",
        "management.endpoint.health.show-details=always"
})
@TestMethodOrder(OrderAnnotation.class)
@Disabled("暫時禁用端到端測試，因為 TestRestTemplate 依賴問題")
public class EndToEndIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private OpenTelemetry openTelemetry;

    @Autowired
    private ObservabilityIntegrationValidator observabilityValidator;

    @Autowired
    private MultiEnvironmentConfigValidator environmentValidator;

    @Autowired
    private DisasterRecoveryValidator drValidator;

    @Autowired
    private CiCdPipelineValidator cicdValidator;

    @Autowired
    private LoadTestingValidator loadTestValidator;

    private String baseUrl;
    private HttpHeaders defaultHeaders;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        defaultHeaders = new HttpHeaders();
        defaultHeaders.set("X-Correlation-ID", "e2e-test-" + System.currentTimeMillis());
        defaultHeaders.set("X-Test-Suite", "EndToEndIntegration");
    }

    @Test
    @Order(1)
    void shouldValidateApplicationStartupAndHealth() {
        // Verify application is running and healthy
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
                baseUrl + "/actuator/health", String.class);

        assertThat(healthResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(healthResponse.getBody()).contains("\"status\":\"UP\"");

        // Verify all health indicators are UP
        assertThat(healthResponse.getBody()).contains("\"db\":{\"status\":\"UP\"");
        assertThat(healthResponse.getBody()).contains("\"diskSpace\":{\"status\":\"UP\"");
    }

    @Test
    @Order(2)
    void shouldValidateObservabilityIntegration() {
        // Test structured logging
        HttpEntity<String> entity = new HttpEntity<>(defaultHeaders);
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/actuator/info", HttpMethod.GET, entity, String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().get("X-Correlation-ID")).isNotNull();

        // Validate observability components
        assertThat(observabilityValidator.validateStructuredLogging()).isTrue();
        assertThat(observabilityValidator.validateMetricsCollection()).isTrue();
        assertThat(observabilityValidator.validateDistributedTracing()).isTrue();
        assertThat(observabilityValidator.validateHealthChecks()).isTrue();
    }

    @Test
    @Order(3)
    void shouldValidateMetricsEndpoints() {
        // Test Prometheus metrics endpoint
        ResponseEntity<String> prometheusResponse = restTemplate.getForEntity(
                baseUrl + "/actuator/prometheus", String.class);

        assertThat(prometheusResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(prometheusResponse.getBody()).contains("jvm_memory_used_bytes");
        assertThat(prometheusResponse.getBody()).contains("http_server_requests_seconds");

        // Test metrics endpoint
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
                baseUrl + "/actuator/metrics", String.class);

        assertThat(metricsResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(metricsResponse.getBody()).contains("jvm.memory.used");
    }

    @Test
    @Order(4)
    void shouldValidateTracingIntegration() {
        // Generate traces by making multiple requests
        for (int i = 0; i < 5; i++) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Correlation-ID", "trace-test-" + i);
            headers.set("X-Trace-Test", "integration");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/actuator/info", HttpMethod.GET, entity, String.class);

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getHeaders().get("X-Trace-ID")).isNotNull();
        }

        // Validate tracing configuration
        assertThat(observabilityValidator.validateTracingConfiguration()).isTrue();
        assertThat(observabilityValidator.validateTraceContextPropagation()).isTrue();
    }

    @Test
    @Order(5)
    void shouldValidateMultiEnvironmentConfiguration() {
        // Validate current test environment configuration
        assertThat(environmentValidator.validateTestEnvironmentConfig()).isTrue();

        // Validate profile-based configuration
        assertThat(environmentValidator.validateProfileConfiguration("test")).isTrue();
        assertThat(environmentValidator.validateDatabaseConfiguration()).isTrue();
        assertThat(environmentValidator.validateEventPublishingConfiguration()).isTrue();

        // Validate observability configuration for test environment
        assertThat(environmentValidator.validateObservabilityConfiguration("test")).isTrue();
    }

    @Test
    @Order(6)
    void shouldValidateBusinessOperationsWithObservability() {
        // Simulate business operations and validate observability
        String correlationId = "business-ops-" + System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-ID", correlationId);
        headers.set("X-Business-Operation", "test-operation");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Make multiple business operation requests
        for (int i = 0; i < 10; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                    baseUrl + "/actuator/info", HttpMethod.GET, entity, String.class);

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(response.getHeaders().get("X-Correlation-ID")).contains(correlationId);
        }

        // Validate that metrics were recorded
        await("Business metrics should be recorded")
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(observabilityValidator.validateBusinessMetrics()).isTrue();
                });
    }

    @Test
    @Order(7)
    void shouldValidateErrorHandlingAndObservability() {
        // Test error scenarios and validate observability
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-ID", "error-test-" + System.currentTimeMillis());
        headers.set("X-Test-Error", "true");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Make request to non-existent endpoint to generate error
        ResponseEntity<String> errorResponse = restTemplate.exchange(
                baseUrl + "/non-existent-endpoint", HttpMethod.GET, entity, String.class);

        assertThat(errorResponse.getStatusCode().is4xxClientError()).isTrue();

        // Validate error observability
        await("Error metrics should be recorded")
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(observabilityValidator.validateErrorMetrics()).isTrue();
                    assertThat(observabilityValidator.validateErrorTracing()).isTrue();
                });
    }

    @Test
    @Order(8)
    void shouldValidatePerformanceUnderLoad() {
        // Simulate concurrent load and validate performance
        int concurrentRequests = 50;
        List<CompletableFuture<ResponseEntity<String>>> futures = generateConcurrentRequests(concurrentRequests);

        // Wait for all requests to complete
        CompletableFuture<Void> allRequests = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        await("All concurrent requests should complete")
                .atMost(30, TimeUnit.SECONDS)
                .until(() -> allRequests.isDone());

        // Validate performance metrics
        assertThat(loadTestValidator.validatePerformanceUnderLoad()).isTrue();
        assertThat(loadTestValidator.validateObservabilityOverhead()).isLessThan(5.0); // < 5%

        // Validate all requests were successful
        long successfulRequests = futures.stream()
                .mapToLong(future -> {
                    try {
                        ResponseEntity<String> response = future.get();
                        return response.getStatusCode().is2xxSuccessful() ? 1 : 0;
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();

        assertThat(successfulRequests).isEqualTo(concurrentRequests);
    }

    @Test
    @Order(9)
    void shouldValidateSecurityAndCompliance() {
        // Validate security configurations
        assertThat(observabilityValidator.validateSecurityConfiguration()).isTrue();
        assertThat(observabilityValidator.validatePiiMasking()).isTrue();
        assertThat(observabilityValidator.validateTlsEncryption()).isTrue();

        // Validate compliance requirements
        assertThat(observabilityValidator.validateDataRetentionPolicies()).isTrue();
        assertThat(observabilityValidator.validateAuditLogging()).isTrue();
    }

    @Test
    @Order(10)
    void shouldValidateCostOptimization() {
        // Validate cost optimization features
        assertThat(observabilityValidator.validateCostOptimization()).isTrue();
        assertThat(observabilityValidator.validateResourceOptimization()).isTrue();
        assertThat(observabilityValidator.validateSamplingStrategies()).isTrue();

        // Validate billing alerts configuration
        assertThat(observabilityValidator.validateBillingAlerts()).isTrue();
    }

    @Test
    @Order(11)
    void shouldValidateDisasterRecoveryReadiness() {
        // Validate DR configuration (simulated in test environment)
        assertThat(drValidator.validateDrConfiguration()).isTrue();
        assertThat(drValidator.validateMultiRegionSetup()).isTrue();
        assertThat(drValidator.validateFailoverProcedures()).isTrue();

        // Validate observability DR capabilities
        assertThat(drValidator.validateObservabilityReplication()).isTrue();
        assertThat(drValidator.validateCrossRegionMonitoring()).isTrue();
    }

    @Test
    @Order(12)
    void shouldValidateCiCdIntegration() {
        // Validate CI/CD pipeline integration (simulated)
        assertThat(cicdValidator.validatePipelineConfiguration()).isTrue();
        assertThat(cicdValidator.validateObservabilityInPipeline()).isTrue();
        assertThat(cicdValidator.validateQualityGates()).isTrue();

        // Validate deployment observability
        assertThat(cicdValidator.validateDeploymentMetrics()).isTrue();
        assertThat(cicdValidator.validateRollbackCapabilities()).isTrue();
    }

    @Test
    @Order(13)
    void shouldValidateEndToEndDataFlow() {
        // Test complete data flow from application to observability systems
        String testCorrelationId = "e2e-data-flow-" + System.currentTimeMillis();

        // Generate test data
        generateTestDataFlow(testCorrelationId);

        // Validate data flow through observability pipeline
        await("Data should flow through observability pipeline")
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(observabilityValidator.validateDataFlowCompleteness(testCorrelationId)).isTrue();
                    assertThat(observabilityValidator.validateDataFlowLatency(testCorrelationId))
                            .isLessThan(Duration.ofSeconds(10));
                });
    }

    @Test
    @Order(14)
    void shouldValidateSystemRecoveryAfterLoad() {
        // Validate system recovery after load testing
        await("System should recover to baseline performance")
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(loadTestValidator.validateSystemRecovery()).isTrue();
                    assertThat(loadTestValidator.validateResourceUtilization()).isLessThan(70.0); // < 70%
                });

        // Validate observability system recovery
        assertThat(observabilityValidator.validateSystemHealth()).isTrue();
        assertThat(observabilityValidator.validatePerformanceBaseline()).isTrue();
    }

    @Test
    @Order(15)
    void shouldValidateComprehensiveSystemValidation() {
        // Final comprehensive validation of all systems
        Map<String, Boolean> validationResults = Map.of(
                "observability", observabilityValidator.validateComprehensiveObservability(),
                "multiEnvironment", environmentValidator.validateComprehensiveConfiguration(),
                "disasterRecovery", drValidator.validateComprehensiveDrReadiness(),
                "cicd", cicdValidator.validateComprehensivePipeline(),
                "performance", loadTestValidator.validateComprehensivePerformance(),
                "security", observabilityValidator.validateComprehensiveSecurity());

        // All validations should pass
        validationResults.forEach((component, result) -> {
            assertThat(result)
                    .as("Component %s should pass comprehensive validation", component)
                    .isTrue();
        });

        // Generate final validation report
        String validationReport = generateValidationReport(validationResults);
        logger.info("End-to-End Integration Test Validation Report:\n{}", validationReport);
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
                            baseUrl + "/actuator/info", HttpMethod.GET, entity, String.class);
                }))
                .toList();
    }

    private void generateTestDataFlow(String correlationId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Correlation-ID", correlationId);
        headers.set("X-Data-Flow-Test", "true");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Generate multiple requests to create data flow
        for (int i = 0; i < 5; i++) {
            restTemplate.exchange(
                    baseUrl + "/actuator/info", HttpMethod.GET, entity, String.class);

            try {
                Thread.sleep(100); // Small delay between requests
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String generateValidationReport(Map<String, Boolean> results) {
        StringBuilder report = new StringBuilder();
        report.append("=== End-to-End Integration Test Results ===\n");

        results.forEach((component, result) -> {
            String status = result ? "✅ PASS" : "❌ FAIL";
            report.append(String.format("%-20s: %s\n", component, status));
        });

        long passCount = results.values().stream().mapToLong(result -> result ? 1 : 0).sum();
        double passRate = (double) passCount / results.size() * 100;

        report.append(String.format("\nOverall Pass Rate: %.1f%% (%d/%d)\n",
                passRate, passCount, results.size()));

        if (passRate == 100.0) {
            report.append("🎉 All integration tests passed successfully!");
        } else {
            report.append("⚠️  Some integration tests failed - review logs for details");
        }

        return report.toString();
    }
}