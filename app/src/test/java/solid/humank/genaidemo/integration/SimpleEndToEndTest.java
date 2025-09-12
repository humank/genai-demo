package solid.humank.genaidemo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import solid.humank.genaidemo.testutils.annotations.IntegrationTest;

/**
 * Simple end-to-end integration test to validate basic functionality
 */
@IntegrationTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.security.user.name=test",
        "spring.security.user.password=test",
        "spring.security.user.roles=USER"
})
@ActiveProfiles("test")
@org.springframework.context.annotation.Import({
        solid.humank.genaidemo.config.TestHttpClientConfiguration.class,
        solid.humank.genaidemo.config.TestWebMvcConfiguration.class
})
public class SimpleEndToEndTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldValidateApplicationHealth() {
        // Test basic application health
        ResponseEntity<String> healthResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);

        assertThat(healthResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(healthResponse.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void shouldValidateActuatorEndpoints() {
        // Test actuator info endpoint
        ResponseEntity<String> infoResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/info", String.class);

        assertThat(infoResponse.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    void shouldValidateMetricsEndpoint() {
        // Test metrics endpoint
        ResponseEntity<String> metricsResponse = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/metrics", String.class);

        assertThat(metricsResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(metricsResponse.getBody()).contains("jvm.memory.used");
    }
}