package solid.humank.genaidemo.infrastructure.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit tests for ResourceRightSizingAnalyzer
 */
class ResourceRightSizingAnalyzerTest {

    private ResourceRightSizingAnalyzer analyzer;
    private ResourceRightSizingProperties properties;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        properties = new ResourceRightSizingProperties();
        properties.setAnalysisEnabled(true);
        properties.setMinSamplesForRecommendation(5);
        properties.setCpuUnderutilizationThreshold(20.0);
        properties.setCpuOverutilizationThreshold(80.0);
        properties.setMemoryUnderutilizationThreshold(30.0);
        properties.setMemoryOverutilizationThreshold(85.0);

        analyzer = new ResourceRightSizingAnalyzer(meterRegistry, properties);
    }

    @Test
    void shouldInitializeWithNoRecommendation() {
        // When
        ResourceRightSizingAnalyzer.RightSizingRecommendation recommendation = analyzer.getCurrentRecommendation();

        // Then
        assertThat(recommendation).isNull();
    }

    @Test
    void shouldReturnCurrentResourceUtilization() {
        // When
        double cpuUtilization = analyzer.getCurrentCpuUtilization();
        double memoryUtilization = analyzer.getCurrentMemoryUtilization();

        // Then
        assertThat(cpuUtilization).isGreaterThanOrEqualTo(0.0);
        assertThat(memoryUtilization).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void shouldRegisterMetricsInMeterRegistry() {
        // Then
        assertThat(meterRegistry.find("resource.cpu.utilization.percentage").gauge()).isNotNull();
        assertThat(meterRegistry.find("resource.memory.utilization.percentage").gauge()).isNotNull();
        assertThat(meterRegistry.find("resource.rightsizing.recommendation.score").gauge()).isNotNull();
    }

    @Test
    void shouldCreateResourceUsageSnapshot() {
        // When
        ResourceRightSizingAnalyzer.ResourceUsageSnapshot snapshot = new ResourceRightSizingAnalyzer.ResourceUsageSnapshot(
                java.time.LocalDateTime.now(),
                50.0, // CPU utilization
                60.0, // Memory utilization
                1024L * 1024L * 512L, // 512MB heap used
                1024L * 1024L * 1024L // 1GB heap max
        );

        // Then
        assertThat(snapshot.getCpuUtilization()).isEqualTo(50.0);
        assertThat(snapshot.getMemoryUtilization()).isEqualTo(60.0);
        assertThat(snapshot.getHeapUsed()).isEqualTo(1024L * 1024L * 512L);
        assertThat(snapshot.getHeapMax()).isEqualTo(1024L * 1024L * 1024L);
        assertThat(snapshot.getTimestamp()).isNotNull();
    }

    @Test
    void shouldCreateRightSizingRecommendation() {
        // Given
        java.time.LocalDateTime timestamp = java.time.LocalDateTime.now();
        ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType overallRec = ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.DOWNSIZE;
        ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType cpuRec = ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.DOWNSIZE;
        ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType memoryRec = ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.OPTIMAL;
        double avgCpu = 15.0;
        double avgMemory = 40.0;
        double peakCpu = 25.0;
        double peakMemory = 55.0;
        double confidence = 85.0;
        String reason = "CPU underutilized";

        // When
        ResourceRightSizingAnalyzer.RightSizingRecommendation recommendation = new ResourceRightSizingAnalyzer.RightSizingRecommendation(
                timestamp, overallRec, cpuRec, memoryRec,
                avgCpu, avgMemory, peakCpu, peakMemory,
                confidence, reason);

        // Then
        assertThat(recommendation.getTimestamp()).isEqualTo(timestamp);
        assertThat(recommendation.getOverallRecommendation()).isEqualTo(overallRec);
        assertThat(recommendation.getCpuRecommendation()).isEqualTo(cpuRec);
        assertThat(recommendation.getMemoryRecommendation()).isEqualTo(memoryRec);
        assertThat(recommendation.getAvgCpuUtilization()).isEqualTo(avgCpu);
        assertThat(recommendation.getAvgMemoryUtilization()).isEqualTo(avgMemory);
        assertThat(recommendation.getPeakCpuUtilization()).isEqualTo(peakCpu);
        assertThat(recommendation.getPeakMemoryUtilization()).isEqualTo(peakMemory);
        assertThat(recommendation.getConfidenceScore()).isEqualTo(confidence);
        assertThat(recommendation.getReason()).isEqualTo(reason);
    }

    @Test
    void shouldReturnZeroRightSizingScoreInitially() {
        // When
        double score = analyzer.getRightSizingScore();

        // Then
        assertThat(score).isEqualTo(0.0);
    }
}