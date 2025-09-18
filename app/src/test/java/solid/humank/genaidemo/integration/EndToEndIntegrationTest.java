package solid.humank.genaidemo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.Map;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import solid.humank.genaidemo.infrastructure.cicd.CiCdPipelineValidator;
import solid.humank.genaidemo.infrastructure.config.MultiEnvironmentConfigValidator;
import solid.humank.genaidemo.infrastructure.disaster.DisasterRecoveryValidator;
import solid.humank.genaidemo.infrastructure.observability.ObservabilityIntegrationValidator;
import solid.humank.genaidemo.infrastructure.performance.LoadTestingValidator;
import solid.humank.genaidemo.testutils.base.BaseIntegrationTest;

/**
 * Comprehensive end-to-end integration tests validating all observability
 * features
 * and infrastructure components across multiple environments.
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:e2e-test",
        "logging.level.solid.humank.genaidemo=DEBUG",
        "management.endpoints.web.exposure.include=*",
        "management.endpoint.health.show-details=always",
        "spring.profiles.active=test"
})
@TestMethodOrder(OrderAnnotation.class)
public class EndToEndIntegrationTest extends BaseIntegrationTest {

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