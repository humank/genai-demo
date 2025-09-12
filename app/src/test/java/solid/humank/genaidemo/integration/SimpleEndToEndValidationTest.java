package solid.humank.genaidemo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import solid.humank.genaidemo.testutils.annotations.IntegrationTest;

/**
 * Simple End-to-End Validation Test
 * Tests basic application functionality without complex dependencies
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@IntegrationTest
@Disabled("暫時禁用端到端測試，因為 TestRestTemplate 依賴問題")
public class SimpleEndToEndValidationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void shouldValidateApplicationHealth() {
        // Test health endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    public void shouldValidateActuatorEndpoints() {
        // Test actuator root endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("health");
        assertThat(response.getBody()).contains("metrics");
    }

    @Test
    public void shouldValidateMetricsEndpoint() {
        // Test metrics endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/metrics", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jvm.memory.used");
    }

    @Test
    public void shouldValidatePrometheusMetrics() {
        // Test prometheus metrics endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/prometheus", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jvm_memory_used_bytes");
    }

    @Test
    public void shouldValidateInfoEndpoint() {
        // Test info endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/info", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}