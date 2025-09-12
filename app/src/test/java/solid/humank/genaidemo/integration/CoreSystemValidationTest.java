package solid.humank.genaidemo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import io.micrometer.core.instrument.MeterRegistry;
import solid.humank.genaidemo.testutils.annotations.IntegrationTest;

/**
 * Core System Validation Test
 * Validates basic functionality and components are working properly
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
public class CoreSystemValidationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    public void testApplicationStartup() {
        // Verify application context loads successfully
        assertNotNull(applicationContext, "Application context should be loaded");
        assertThat(applicationContext.getBeanDefinitionNames().length).isGreaterThan(0);
    }

    @Test
    public void testBasicComponentsAvailable() {
        // Verify basic Spring components are available
        assertNotNull(environment, "Environment should be available");
        assertNotNull(meterRegistry, "MeterRegistry should be available");
    }

    @Test
    public void testProfileConfiguration() {
        // Verify test profile is active
        String[] activeProfiles = environment.getActiveProfiles();
        assertThat(activeProfiles).contains("test");
    }

    @Test
    public void testHealthChecks() {
        // Test health endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    public void testMetricsSystem() {
        // Verify metrics system is functional
        assertNotNull(meterRegistry, "MeterRegistry should be available");

        // Test metrics endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/metrics", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void testLoggingSystem() {
        // Verify logging system by checking loggers endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/loggers", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    public void testObservabilityStack() {
        // Test prometheus metrics endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/prometheus", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jvm_memory_used_bytes");
    }

    @Test
    public void testInfrastructureComponents() {
        // Test info endpoint to verify infrastructure info
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/info", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}