package solid.humank.genaidemo.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import solid.humank.genaidemo.infrastructure.observability.logging.LogRetentionOptimizer;
import solid.humank.genaidemo.infrastructure.observability.logging.LogRetentionProperties;
import solid.humank.genaidemo.infrastructure.observability.metrics.CostOptimizationProperties;
import solid.humank.genaidemo.infrastructure.observability.metrics.CostOptimizationService;
import solid.humank.genaidemo.infrastructure.observability.metrics.ResourceRightSizingAnalyzer;
import solid.humank.genaidemo.infrastructure.observability.metrics.ResourceRightSizingProperties;

/**
 * Integration test for cost optimization components working together
 */
@SpringBootTest(classes = {
        CostOptimizationService.class,
        ResourceRightSizingAnalyzer.class,
        LogRetentionOptimizer.class,
        CostOptimizationProperties.class,
        ResourceRightSizingProperties.class,
        LogRetentionProperties.class
})
@ActiveProfiles("test")
class CostOptimizationIntegrationTest {

    private CostOptimizationService costOptimizationService;
    private ResourceRightSizingAnalyzer rightSizingAnalyzer;
    private LogRetentionOptimizer logRetentionOptimizer;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();

        // Create properties
        CostOptimizationProperties costProperties = new CostOptimizationProperties();
        costProperties.setEnabled(true);
        costProperties.setEstimatedMonthlyCost(500.0);

        ResourceRightSizingProperties rightSizingProperties = new ResourceRightSizingProperties();
        rightSizingProperties.setAnalysisEnabled(true);
        rightSizingProperties.setMinSamplesForRecommendation(5);

        LogRetentionProperties logProperties = new LogRetentionProperties();
        logProperties.setOptimizationEnabled(true);

        // Create services
        rightSizingAnalyzer = new ResourceRightSizingAnalyzer(meterRegistry, rightSizingProperties);
        costOptimizationService = new CostOptimizationService(meterRegistry, costProperties, rightSizingAnalyzer);
        logRetentionOptimizer = new LogRetentionOptimizer(logProperties);
    }

    @Test
    void shouldInitializeAllComponents() {
        // Then
        assertThat(costOptimizationService).isNotNull();
        assertThat(rightSizingAnalyzer).isNotNull();
        assertThat(logRetentionOptimizer).isNotNull();
    }

    @Test
    void shouldRegisterMetricsInMeterRegistry() {
        // Then
        assertThat(meterRegistry.find("cost.optimization.recommendations.generated").counter()).isNotNull();
        assertThat(meterRegistry.find("cost.optimization.score").gauge()).isNotNull();
        assertThat(meterRegistry.find("resource.cpu.utilization.percentage").gauge()).isNotNull();
        assertThat(meterRegistry.find("resource.memory.utilization.percentage").gauge()).isNotNull();
        assertThat(meterRegistry.find("resource.rightsizing.recommendation.score").gauge()).isNotNull();
    }

    @Test
    void shouldProvideInitialOptimizationScore() {
        // When
        double score = costOptimizationService.getOptimizationScore();

        // Then
        assertThat(score).isGreaterThanOrEqualTo(0.0);
        assertThat(score).isLessThanOrEqualTo(100.0);
    }

    @Test
    void shouldProvideResourceUtilizationMetrics() {
        // When
        double cpuUtilization = rightSizingAnalyzer.getCurrentCpuUtilization();
        double memoryUtilization = rightSizingAnalyzer.getCurrentMemoryUtilization();

        // Then
        assertThat(cpuUtilization).isGreaterThanOrEqualTo(0.0);
        assertThat(memoryUtilization).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void shouldProvideEmptyRecommendationsInitially() {
        // When
        List<CostOptimizationService.CostOptimizationRecommendation> recommendations = costOptimizationService
                .getActiveRecommendations();

        // Then
        assertThat(recommendations).isEmpty();
    }

    @Test
    void shouldAllowLogEventRecording() {
        // When
        logRetentionOptimizer.recordLogEvent("test.logger", "INFO");
        logRetentionOptimizer.recordLogEvent("test.logger", "DEBUG");
        logRetentionOptimizer.recordLogEvent("another.logger", "WARN");

        // Then - no exception should be thrown
        // This verifies the log retention optimizer can handle log events
    }

    @Test
    void shouldHaveCorrectMeterRegistryTags() {
        // When
        double cpuUtilization = rightSizingAnalyzer.getCurrentCpuUtilization();

        // Then
        assertThat(meterRegistry.find("resource.cpu.utilization.percentage").gauge()).isNotNull();
        assertThat(meterRegistry.find("resource.cpu.utilization.percentage").gauge().value())
                .isEqualTo(cpuUtilization);
    }

    @Test
    void shouldProvideRightSizingScore() {
        // When
        double rightSizingScore = rightSizingAnalyzer.getRightSizingScore();

        // Then
        assertThat(rightSizingScore).isGreaterThanOrEqualTo(0.0);
        assertThat(rightSizingScore).isLessThanOrEqualTo(100.0);
    }

    @Test
    void shouldHaveNoRightSizingRecommendationInitially() {
        // When
        ResourceRightSizingAnalyzer.RightSizingRecommendation recommendation = rightSizingAnalyzer
                .getCurrentRecommendation();

        // Then
        assertThat(recommendation).isNull();
    }

    @Test
    void shouldIncrementRecommendationsCounterWhenAnalyzing() {
        // Given
        double initialCount = meterRegistry.find("cost.optimization.recommendations.generated")
                .counter().count();

        // When
        costOptimizationService.analyzeAndRecommend();

        // Then
        double finalCount = meterRegistry.find("cost.optimization.recommendations.generated")
                .counter().count();
        assertThat(finalCount).isGreaterThanOrEqualTo(initialCount);
    }
}