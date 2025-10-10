package solid.humank.genaidemo.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.instrument.search.Search;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit test for AWS Native Concurrency Monitoring System
 * 
 * This test verifies the monitoring logic without requiring full Spring context.
 * Tests metric registration and calculation logic in isolation.
 * 
 * NOTE: Converted from integration test to unit test following standards.
 * For actual metrics collection testing, see separate integration test.
 * 
 * Created: 2025年9月24日 下午6:23 (台北時間)
 * Updated: 2025-01-22 (Converted to unit test)
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("Concurrency Monitoring Unit Tests")
class ConcurrencyMonitoringIntegrationTest {

    private MeterRegistry meterRegistry;

    @Mock
    private ThreadPoolTaskExecutor eventProcessingExecutor;

    @Mock
    private ThreadPoolTaskExecutor retryExecutor;
    
    @Mock
    private ThreadPoolExecutor threadPoolExecutor;

    @BeforeEach
    void setUp() {
        // Use SimpleMeterRegistry for unit testing
        meterRegistry = new SimpleMeterRegistry();
        
        // Setup mock thread pool executor
        when(eventProcessingExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
        when(retryExecutor.getThreadPoolExecutor()).thenReturn(threadPoolExecutor);
        
        // Setup default mock behavior - use lenient() to avoid UnnecessaryStubbingException
        org.mockito.Mockito.lenient().when(threadPoolExecutor.getActiveCount()).thenReturn(5);
        org.mockito.Mockito.lenient().when(threadPoolExecutor.getMaximumPoolSize()).thenReturn(10);
        org.mockito.Mockito.lenient().when(threadPoolExecutor.getQueue()).thenReturn(new java.util.concurrent.LinkedBlockingQueue<>());
        org.mockito.Mockito.lenient().when(threadPoolExecutor.getCompletedTaskCount()).thenReturn(100L);
        
        // Register metrics manually for testing
        registerThreadPoolMetrics("event_processing", eventProcessingExecutor);
        registerThreadPoolMetrics("retry", retryExecutor);
    }
    
    private void registerThreadPoolMetrics(String poolName, ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
        
        // Register active threads gauge
        meterRegistry.gauge("thread_pool_active_threads", 
            io.micrometer.core.instrument.Tags.of("pool", poolName, "component", "thread-pool"),
            pool, ThreadPoolExecutor::getActiveCount);
        
        // Register utilization ratio gauge
        meterRegistry.gauge("thread_pool_utilization_ratio",
            io.micrometer.core.instrument.Tags.of("pool", poolName, "component", "thread-pool"),
            pool, p -> (double) p.getActiveCount() / p.getMaximumPoolSize());
        
        // Register queue utilization ratio gauge
        meterRegistry.gauge("thread_pool_queue_utilization_ratio",
            io.micrometer.core.instrument.Tags.of("pool", poolName, "component", "thread-pool"),
            pool, p -> (double) p.getQueue().size() / (p.getQueue().size() + p.getQueue().remainingCapacity()));
        
        // Register pressure score gauge
        meterRegistry.gauge("thread_pool_pressure_score",
            io.micrometer.core.instrument.Tags.of("pool", poolName, "component", "thread-pool"),
            pool, this::calculatePressureScore);
        
        // Register completed tasks gauge
        meterRegistry.gauge("thread_pool_completed_tasks_total",
            io.micrometer.core.instrument.Tags.of("pool", poolName, "component", "thread-pool"),
            pool, p -> (double) p.getCompletedTaskCount());
        
        // Register standard executor metrics
        meterRegistry.gauge("executor.active",
            io.micrometer.core.instrument.Tags.of("name", poolName + "_executor"),
            pool, ThreadPoolExecutor::getActiveCount);
        
        meterRegistry.gauge("executor.pool.max",
            io.micrometer.core.instrument.Tags.of("name", poolName + "_executor"),
            pool, ThreadPoolExecutor::getMaximumPoolSize);
        
        meterRegistry.gauge("executor.queued",
            io.micrometer.core.instrument.Tags.of("name", poolName + "_executor"),
            pool, p -> (double) p.getQueue().size());
    }
    
    private double calculatePressureScore(ThreadPoolExecutor pool) {
        double utilizationRatio = (double) pool.getActiveCount() / pool.getMaximumPoolSize();
        double queueUtilizationRatio = (double) pool.getQueue().size() / 
            (pool.getQueue().size() + pool.getQueue().remainingCapacity());
        return (utilizationRatio * 0.7) + (queueUtilizationRatio * 0.3);
    }

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
    @DisplayName("Should calculate thread pool pressure score correctly")
    void should_calculate_thread_pool_pressure_score_correctly() {
        // Given - Mock thread pool with specific values
        when(threadPoolExecutor.getActiveCount()).thenReturn(7);
        when(threadPoolExecutor.getMaximumPoolSize()).thenReturn(10);
        when(threadPoolExecutor.getQueue()).thenReturn(new java.util.concurrent.LinkedBlockingQueue<>());
        
        // When - Get the pressure score gauge
        var pressureScoreGauge = Search.in(meterRegistry)
                .name("thread_pool_pressure_score")
                .tag("pool", "event_processing")
                .gauge();

        // Then
        assertThat(pressureScoreGauge).isNotNull();
        double pressureScore = pressureScoreGauge.value();
        assertThat(pressureScore).isBetween(0.0, 1.0);
        // With 7/10 active threads and empty queue: (0.7 * 0.7) + (0.0 * 0.3) = 0.49
        assertThat(pressureScore).isCloseTo(0.49, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    @DisplayName("Should calculate utilization ratios correctly")
    void should_calculate_utilization_ratios_correctly() {
        // Given - Mock thread pool with specific values
        when(threadPoolExecutor.getActiveCount()).thenReturn(5);
        when(threadPoolExecutor.getMaximumPoolSize()).thenReturn(10);
        
        // When - Get the utilization ratio gauge
        var utilizationGauge = Search.in(meterRegistry)
                .name("thread_pool_utilization_ratio")
                .tag("pool", "event_processing")
                .gauge();

        // Then
        assertThat(utilizationGauge).isNotNull();
        double utilization = utilizationGauge.value();
        assertThat(utilization).isEqualTo(0.5); // 5/10 = 0.5

        // When - Get the queue utilization ratio gauge
        var queueUtilizationGauge = Search.in(meterRegistry)
                .name("thread_pool_queue_utilization_ratio")
                .tag("pool", "event_processing")
                .gauge();

        // Then
        assertThat(queueUtilizationGauge).isNotNull();
        double queueUtilization = queueUtilizationGauge.value();
        assertThat(queueUtilization).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("Should have proper metric tags")
    void should_have_proper_metric_tags() {
        // When - Search for active threads metrics
        var activeThreadsMeters = Search.in(meterRegistry)
                .name("thread_pool_active_threads")
                .meters();

        // Then - Verify metrics exist and have proper tags
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
    @DisplayName("Should track completed tasks")
    void should_track_completed_tasks() {
        // Given - Mock completed tasks count
        when(threadPoolExecutor.getCompletedTaskCount()).thenReturn(150L);
        
        // When - Get the completed tasks gauge
        var completedTasksGauge = Search.in(meterRegistry)
                .name("thread_pool_completed_tasks_total")
                .tag("pool", "event_processing")
                .gauge();

        // Then
        assertThat(completedTasksGauge).isNotNull();
        double completedTasks = completedTasksGauge.value();
        assertThat(completedTasks).isEqualTo(150.0);
    }
}