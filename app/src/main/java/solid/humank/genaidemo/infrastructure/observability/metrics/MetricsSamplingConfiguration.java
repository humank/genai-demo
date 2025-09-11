package solid.humank.genaidemo.infrastructure.observability.metrics;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;

/**
 * Configuration for metrics sampling strategies to optimize performance and
 * reduce costs.
 * Implements requirement 12.1: WHEN collecting metrics THEN the system SHALL
 * implement sampling strategies to reduce costs
 */
@Configuration
public class MetricsSamplingConfiguration {

    /**
     * Development profile - sample all metrics for debugging
     */
    @Bean
    @Profile("dev")
    public MeterRegistryCustomizer<MeterRegistry> developmentMetricsCustomizer() {
        return registry -> registry.config()
                // Sample all metrics in development
                .meterFilter(MeterFilter.accept())
                .commonTags("environment", "development", "cost-tier", "full-sampling");
    }

    /**
     * Production profile - implement cost-optimized sampling
     */
    @Bean
    @Profile("production")
    public MeterRegistryCustomizer<MeterRegistry> productionMetricsCustomizer(MetricsSamplingProperties properties) {
        return registry -> {
            // High-frequency metrics with reduced sampling
            registry.config()
                    .meterFilter(MeterFilter.denyNameStartsWith("jvm.gc.pause"))
                    .meterFilter(MeterFilter.denyNameStartsWith("jvm.memory.committed"))
                    .meterFilter(MeterFilter.denyNameStartsWith("process.cpu.usage"))

                    // Business-critical metrics - always sample
                    .meterFilter(MeterFilter.accept(id -> id.getName().startsWith("orders.") ||
                            id.getName().startsWith("payments.") ||
                            id.getName().startsWith("customers.") ||
                            id.getName().startsWith("http.server.requests")))

                    // Infrastructure metrics - reduced sampling
                    .meterFilter(MeterFilter.deny(id -> id.getName().startsWith("hikaricp.") &&
                            !id.getName().contains("active") &&
                            !id.getName().contains("pending")))

                    // Configure distribution statistics for cost optimization
                    .meterFilter(new MeterFilter() {
                        @Override
                        public DistributionStatisticConfig configure(io.micrometer.core.instrument.Meter.Id id,
                                DistributionStatisticConfig config) {
                            if (id.getName().startsWith("http.server.requests")) {
                                return DistributionStatisticConfig.builder()
                                        .percentiles(0.95, 0.99) // Only high percentiles for HTTP requests
                                        .percentilesHistogram(false) // Disable histogram to reduce cardinality
                                        .build()
                                        .merge(config);
                            }

                            if (id.getName().startsWith("spring.data.repository")) {
                                return DistributionStatisticConfig.builder()
                                        .percentiles(0.95) // Only 95th percentile for DB operations
                                        .percentilesHistogram(false)
                                        .build()
                                        .merge(config);
                            }

                            // Default: no distribution statistics for other metrics
                            return DistributionStatisticConfig.builder()
                                    .percentilesHistogram(false)
                                    .build()
                                    .merge(config);
                        }
                    })

                    .commonTags(
                            "environment", "production",
                            "cost-tier", "optimized-sampling",
                            "region", System.getenv().getOrDefault("AWS_REGION", "ap-northeast-1"));
        };
    }

    /**
     * Test profile - minimal metrics for fast tests
     */
    @Bean
    @Profile("test")
    public MeterRegistryCustomizer<MeterRegistry> testMetricsCustomizer() {
        return registry -> registry.config()
                .meterFilter(MeterFilter.denyNameStartsWith("jvm"))
                .meterFilter(MeterFilter.denyNameStartsWith("process"))
                .meterFilter(MeterFilter.denyNameStartsWith("system"))
                .meterFilter(MeterFilter.accept(id -> id.getName().startsWith("orders.") ||
                        id.getName().startsWith("customers.") ||
                        id.getName().startsWith("test.")))
                .commonTags("environment", "test", "cost-tier", "minimal");
    }

    @Bean
    @ConfigurationProperties(prefix = "genai-demo.observability.metrics.sampling")
    public MetricsSamplingProperties metricsSamplingProperties() {
        return new MetricsSamplingProperties();
    }

    /**
     * Configuration properties for metrics sampling
     */
    public static class MetricsSamplingProperties {
        private boolean enabled = true;
        private double businessMetricsSamplingRate = 1.0;
        private double infrastructureMetricsSamplingRate = 0.1;
        private double jvmMetricsSamplingRate = 0.05;
        private int maxCardinality = 10000;
        private Duration retentionPeriod = Duration.ofDays(15);
        private Map<String, Double> customSamplingRates = Map.of(
                "http.server.requests", 1.0,
                "orders.*", 1.0,
                "payments.*", 1.0,
                "customers.*", 1.0,
                "jvm.*", 0.05,
                "hikaricp.*", 0.1);

        // Getters and setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getBusinessMetricsSamplingRate() {
            return businessMetricsSamplingRate;
        }

        public void setBusinessMetricsSamplingRate(double businessMetricsSamplingRate) {
            this.businessMetricsSamplingRate = businessMetricsSamplingRate;
        }

        public double getInfrastructureMetricsSamplingRate() {
            return infrastructureMetricsSamplingRate;
        }

        public void setInfrastructureMetricsSamplingRate(double infrastructureMetricsSamplingRate) {
            this.infrastructureMetricsSamplingRate = infrastructureMetricsSamplingRate;
        }

        public double getJvmMetricsSamplingRate() {
            return jvmMetricsSamplingRate;
        }

        public void setJvmMetricsSamplingRate(double jvmMetricsSamplingRate) {
            this.jvmMetricsSamplingRate = jvmMetricsSamplingRate;
        }

        public int getMaxCardinality() {
            return maxCardinality;
        }

        public void setMaxCardinality(int maxCardinality) {
            this.maxCardinality = maxCardinality;
        }

        public Duration getRetentionPeriod() {
            return retentionPeriod;
        }

        public void setRetentionPeriod(Duration retentionPeriod) {
            this.retentionPeriod = retentionPeriod;
        }

        public Map<String, Double> getCustomSamplingRates() {
            return customSamplingRates;
        }

        public void setCustomSamplingRates(Map<String, Double> customSamplingRates) {
            this.customSamplingRates = customSamplingRates;
        }
    }
}