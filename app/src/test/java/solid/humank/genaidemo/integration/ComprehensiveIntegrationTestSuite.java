package solid.humank.genaidemo.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import io.cucumber.junit.platform.engine.Cucumber;
import solid.humank.genaidemo.testutils.annotations.IntegrationTest;

/**
 * Comprehensive integration test suite that runs all end-to-end integration
 * tests
 * including BDD scenarios, performance tests, and infrastructure validation.
 */
@IntegrationTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:comprehensive-test",
        "logging.level.solid.humank.genaidemo=INFO",
        "management.endpoints.web.exposure.include=*",
        "management.endpoint.health.show-details=always",
        "cucumber.junit-platform.naming-strategy=long",
        "cucumber.execution.parallel.enabled=false"
})
@TestMethodOrder(OrderAnnotation.class)
@Cucumber
public class ComprehensiveIntegrationTestSuite {

    @Test
    @Order(1)
    @DisplayName("End-to-End Integration Tests")
    void runEndToEndIntegrationTests() {
        // This test is handled by the EndToEndIntegrationTest class
        // The @Cucumber annotation will run all BDD scenarios
    }

    @Test
    @Order(2)
    @DisplayName("Load Testing and Performance Validation")
    void runLoadTestingIntegration() {
        // This test is handled by the LoadTestingIntegrationTest class
    }

    @Test
    @Order(3)
    @DisplayName("BDD Observability Scenarios")
    void runObservabilityBddScenarios() {
        // BDD scenarios are automatically executed by Cucumber
        // Features:
        // - comprehensive-observability-integration.feature
        // - distributed-tracing.feature
    }

    @Test
    @Order(4)
    @DisplayName("BDD Infrastructure Scenarios")
    void runInfrastructureBddScenarios() {
        // BDD scenarios are automatically executed by Cucumber
        // Features:
        // - multi-environment-integration.feature
        // - disaster-recovery-integration.feature
        // - cicd-pipeline-integration.feature
    }

    @Test
    @Order(5)
    @DisplayName("BDD Performance Scenarios")
    void runPerformanceBddScenarios() {
        // BDD scenarios are automatically executed by Cucumber
        // Features:
        // - load-testing-integration.feature
    }
}