package solid.humank.genaidemo.infrastructure.observability.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for LogRetentionOptimizer
 */
@ExtendWith(MockitoExtension.class)
class LogRetentionOptimizerTest {

    private LogRetentionOptimizer optimizer;
    private LogRetentionProperties properties;

    @BeforeEach
    void setUp() {
        properties = new LogRetentionProperties();
        properties.setOptimizationEnabled(true);
        properties.setHighVolumeThreshold(1000);

        optimizer = new LogRetentionOptimizer(properties);
    }

    @Test
    void shouldInitializeWithDefaultProperties() {
        // Then
        assertThat(properties.isOptimizationEnabled()).isTrue();
        assertThat(properties.getHighVolumeThreshold()).isEqualTo(1000);
        assertThat(properties.getRetentionPolicies()).isNotEmpty();
    }

    @Test
    void shouldRecordLogEvent() {
        // Given
        String loggerName = "test.logger";
        String level = "INFO";

        // When
        optimizer.recordLogEvent(loggerName, level);

        // Then - no exception should be thrown
        // This is a void method that updates internal state
    }

    @Test
    void shouldCreateLogLevelStats() {
        // Given
        String loggerName = "test.logger";
        String level = "DEBUG";

        // When
        LogRetentionOptimizer.LogLevelStats stats = new LogRetentionOptimizer.LogLevelStats(loggerName, level);

        // Then
        assertThat(stats.getLoggerName()).isEqualTo(loggerName);
        assertThat(stats.getLevel()).isEqualTo(level);
        assertThat(stats.getCount()).isEqualTo(0);
        assertThat(stats.getLastUpdated()).isNotNull();
    }

    @Test
    void shouldIncrementLogLevelStatsCount() {
        // Given
        LogRetentionOptimizer.LogLevelStats stats = new LogRetentionOptimizer.LogLevelStats("test.logger", "INFO");

        // When
        stats.incrementCount();
        stats.incrementCount();

        // Then
        assertThat(stats.getCount()).isEqualTo(2);
    }

    @Test
    void shouldCalculateVolumePerMinute() {
        // Given
        LogRetentionOptimizer.LogLevelStats stats = new LogRetentionOptimizer.LogLevelStats("test.logger", "INFO");

        // When
        stats.incrementCount();
        stats.incrementCount();
        stats.incrementCount();
        double volume = stats.getVolumePerMinute();

        // Then
        assertThat(volume).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void shouldCreateRetentionPolicy() {
        // Given
        Duration hot = Duration.ofDays(7);
        Duration warm = Duration.ofDays(30);
        Duration cold = Duration.ofDays(90);

        // When
        LogRetentionProperties.RetentionPolicy policy = new LogRetentionProperties.RetentionPolicy(hot, warm, cold);

        // Then
        assertThat(policy.getHotStorageDuration()).isEqualTo(hot);
        assertThat(policy.getWarmStorageDuration()).isEqualTo(warm);
        assertThat(policy.getColdStorageDuration()).isEqualTo(cold);
    }

    @Test
    void shouldHaveDefaultRetentionPolicies() {
        // Then
        assertThat(properties.getRetentionPolicies()).containsKeys(
                "application", "security", "audit", "performance");

        LogRetentionProperties.RetentionPolicy appPolicy = properties.getRetentionPolicies().get("application");
        assertThat(appPolicy.getHotStorageDuration()).isEqualTo(Duration.ofDays(7));
        assertThat(appPolicy.getWarmStorageDuration()).isEqualTo(Duration.ofDays(30));
        assertThat(appPolicy.getColdStorageDuration()).isEqualTo(Duration.ofDays(90));
    }
}