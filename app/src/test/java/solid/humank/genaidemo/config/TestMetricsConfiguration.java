package solid.humank.genaidemo.config;

import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for Prometheus metrics to ensure the endpoint works in tests
 */
@TestConfiguration
@Profile("test")
@Import(PrometheusMetricsExportAutoConfiguration.class)
public class TestMetricsConfiguration {
    // This configuration ensures Prometheus metrics are properly configured in tests
}