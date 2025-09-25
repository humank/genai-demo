package solid.humank.genaidemo.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Test configuration for metrics and thread pool monitoring
 * 
 * This configuration provides the necessary beans for testing
 * the concurrency monitoring system without requiring external dependencies.
 * 
 * Created: 2025年9月24日 下午6:41 (台北時間)
 */
@TestConfiguration
public class TestMetricsConfiguration {

    @Bean
    @Primary
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}