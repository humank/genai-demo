package solid.humank.genaidemo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for tracing - beans are provided by
 * TestObservabilityConfiguration
 * to avoid conflicts
 */
@TestConfiguration
@Profile("test")
public class TestTracingConfiguration {
    // Tracing beans are provided by TestObservabilityConfiguration to avoid
    // conflicts
}