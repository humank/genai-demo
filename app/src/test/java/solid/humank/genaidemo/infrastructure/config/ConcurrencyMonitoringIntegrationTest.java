package solid.humank.genaidemo.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for AWS Native Concurrency Monitoring System
 * 
 * This test verifies that the monitoring configuration is properly set up
 * and that metrics are being collected for KEDA autoscaling.
 * 
 * Created: 2025年9月24日 下午6:23 (台北時間)
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = {
        EventProcessingConfig.class,
        TestMetricsConfiguration.class
    }
)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.main.web-application-type=none",
    "management.endpoints.web.exposure.include=health,metrics,prometheus",
    "management.endpoint.health.show-details=always",
    "management.metrics.export.cloudwatch.enabled=false",
    "aws.xray.enabled=false",
    "tracing.enabled=false",
    "spring.redis.host=localhost",
    "spring.redis.port=6379",
    "spring.data.redis.repositories.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.data.redis.RedisReactiveHealthContributorAutoConfiguration,org.redisson.spring.starter.RedissonAutoConfigurationV2"
})
class ConcurrencyMonitoringIntegrationTest {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ThreadPoolTaskExecutor eventProcessingExecutor;

    @Autowired
    private ThreadPoolTaskExecutor retryExecutor;

    @Test
    void should_register_thread_pool_metrics_for_keda() {
        // Verify that KEDA-specific metrics are registered
        assertThat(Search.in(meterRegistry)
                .name("thread_pool_active_threads")
                .tag("pool", "event_processing")
                .meters())
                .hasSize(1);

        assertThat(Search.in(meterRegistry)
                .name("thread_pool_utilization_ratio")
                .tag("pool", "event_processing")
                .meters())
                .hasSize(1);

        assertThat(Search.in(meterRegistry)
                .name("thread_pool_queue_utilization_ratio")
                .tag("pool", "event_processing")
                .meters())
                .hasSize(1);

        assertThat(Search.in(meterRegistry)
                .name("thread_pool_pressure_score")
                .tag("pool", "event_processing")
                .meters())
                .hasSize(1);
    }

    @Test
    void should_register_retry_executor_metrics() {
        // Verify that retry executor metrics are registered
        assertThat(Search.in(meterRegistry)
                .name("thread_pool_active_threads")
                .tag("pool", "retry")
                .meters())
                .hasSize(1);

        assertThat(Search.in(meterRegistry)
                .name("thread_pool_utilization_ratio")
                .tag("pool", "retry")
                .meters())
                .hasSize(1);
    }

    @Test
    void should_have_executor_service_metrics() {
        // Verify that standard executor service metrics are registered
        assertThat(Search.in(meterRegistry)
                .name("executor.active")
                .tag("name", "event_processing_executor")
                .meters())
                .hasSize(1);

        assertThat(Search.in(meterRegistry)
                .name("executor.pool.max")
                .tag("name", "event_processing_executor")
                .meters())
                .hasSize(1);

        assertThat(Search.in(meterRegistry)
                .name("executor.queued")
                .tag("name", "event_processing_executor")
                .meters())
                .hasSize(1);
    }

    @Test
    void should_calculate_thread_pool_pressure_score_correctly() {
        // Get the pressure score gauge
        var pressureScoreGauge = Search.in(meterRegistry)
                .name("thread_pool_pressure_score")
                .tag("pool", "event_processing")
                .gauge();

        assertThat(pressureScoreGauge).isNotNull();
        
        // The pressure score should be between 0 and 1
        double pressureScore = pressureScoreGauge.value();
        assertThat(pressureScore).isBetween(0.0, 1.0);
    }

    @Test
    void should_calculate_utilization_ratios_correctly() {
        // Get the utilization ratio gauge
        var utilizationGauge = Search.in(meterRegistry)
                .name("thread_pool_utilization_ratio")
                .tag("pool", "event_processing")
                .gauge();

        assertThat(utilizationGauge).isNotNull();
        
        // The utilization ratio should be between 0 and 1
        double utilization = utilizationGauge.value();
        assertThat(utilization).isBetween(0.0, 1.0);

        // Get the queue utilization ratio gauge
        var queueUtilizationGauge = Search.in(meterRegistry)
                .name("thread_pool_queue_utilization_ratio")
                .tag("pool", "event_processing")
                .gauge();

        assertThat(queueUtilizationGauge).isNotNull();
        
        // The queue utilization ratio should be between 0 and 1
        double queueUtilization = queueUtilizationGauge.value();
        assertThat(queueUtilization).isBetween(0.0, 1.0);
    }

    @Test
    void should_have_proper_metric_tags() {
        // Verify that metrics have proper tags for identification
        var activeThreadsMeters = Search.in(meterRegistry)
                .name("thread_pool_active_threads")
                .meters();

        assertThat(activeThreadsMeters).isNotEmpty();
        
        activeThreadsMeters.forEach(meter -> {
            assertThat(meter.getId().getTags()).anyMatch(tag -> 
                tag.getKey().equals("pool") && 
                (tag.getValue().equals("event_processing") || tag.getValue().equals("retry")));
            
            assertThat(meter.getId().getTags()).anyMatch(tag -> 
                tag.getKey().equals("component") && 
                tag.getValue().equals("thread-pool"));
        });
    }

    @Test
    void should_track_completed_tasks() {
        // Verify that completed tasks metric is registered
        var completedTasksGauge = Search.in(meterRegistry)
                .name("thread_pool_completed_tasks_total")
                .tag("pool", "event_processing")
                .gauge();

        assertThat(completedTasksGauge).isNotNull();
        
        // The completed tasks count should be non-negative
        double completedTasks = completedTasksGauge.value();
        assertThat(completedTasks).isGreaterThanOrEqualTo(0.0);
    }
}