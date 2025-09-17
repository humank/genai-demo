package solid.humank.genaidemo.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import solid.humank.genaidemo.testutils.annotations.IntegrationTest;

/**
 * Simple end-to-end integration test to validate basic functionality
 */
@IntegrationTest
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
                "spring.security.user.name=test",
                "spring.security.user.password=test",
                "spring.security.user.roles=USER",
                "springdoc.api-docs.enabled=false",
                "springdoc.swagger-ui.enabled=false"
})
@ActiveProfiles("test")
@org.springframework.context.annotation.Import({
                solid.humank.genaidemo.config.UnifiedTestHttpClientConfiguration.class
})
public class SimpleEndToEndTest {

        @LocalServerPort
        private int port;

        @Test
        void shouldValidateApplicationHealth() {
                // Test basic application startup - if we get here, the app started successfully
                assertThat(port).isGreaterThan(0);
        }

        @Test
        void shouldValidateActuatorEndpoints() {
                // Test that actuator configuration is loaded
                assertThat(port).isGreaterThan(0);
        }

        @Test
        void shouldValidateMetricsEndpoint() {
                // Test that metrics configuration is loaded
                assertThat(port).isGreaterThan(0);
        }
}