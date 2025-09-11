package solid.humank.genaidemo.infrastructure.observability.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit tests for CostOptimizationService
 */
@ExtendWith(MockitoExtension.class)
class CostOptimizationServiceTest {

    @Mock
    private ResourceRightSizingAnalyzer rightSizingAnalyzer;

    private CostOptimizationService costOptimizationService;
    private CostOptimizationProperties properties;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        properties = new CostOptimizationProperties();
        properties.setEnabled(true);
        properties.setEstimatedMonthlyCost(500.0);

        costOptimizationService = new CostOptimizationService(
                meterRegistry, properties, rightSizingAnalyzer);
    }

    @Test
    void shouldInitializeWithDefaultOptimizationScore() {
        // When
        double score = costOptimizationService.getOptimizationScore();

        // Then
        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void shouldReturnEmptyRecommendationsInitially() {
        // When
        List<CostOptimizationService.CostOptimizationRecommendation> recommendations = costOptimizationService
                .getActiveRecommendations();

        // Then
        assertThat(recommendations).isEmpty();
    }

    @Test
    void shouldGenerateDownsizeRecommendationForLowUtilization() {
        // Given
        ResourceRightSizingAnalyzer.RightSizingRecommendation rightSizingRecommendation = new ResourceRightSizingAnalyzer.RightSizingRecommendation(
                java.time.LocalDateTime.now(),
                ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.DOWNSIZE,
                ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.DOWNSIZE,
                ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.DOWNSIZE,
                15.0, // Low average CPU
                20.0, // Low average memory
                25.0, // Low peak CPU
                30.0, // Low peak memory
                85.0, // High confidence
                "Low resource utilization detected");

        when(rightSizingAnalyzer.getCurrentRecommendation()).thenReturn(rightSizingRecommendation);

        // When
        costOptimizationService.analyzeAndRecommend();

        // Then
        List<CostOptimizationService.CostOptimizationRecommendation> recommendations = costOptimizationService
                .getActiveRecommendations();

        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations.get(0).getId()).isEqualTo("RESOURCE_DOWNSIZE");
        assertThat(recommendations.get(0).getPriority())
                .isEqualTo(CostOptimizationService.CostOptimizationRecommendation.Priority.HIGH);
        assertThat(recommendations.get(0).getPotentialMonthlySavings()).isPositive();
    }

    @Test
    void shouldGenerateUpsizeRecommendationForHighUtilization() {
        // Given
        ResourceRightSizingAnalyzer.RightSizingRecommendation rightSizingRecommendation = new ResourceRightSizingAnalyzer.RightSizingRecommendation(
                java.time.LocalDateTime.now(),
                ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.UPSIZE,
                ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.UPSIZE,
                ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.UPSIZE,
                75.0, // High average CPU
                80.0, // High average memory
                95.0, // Very high peak CPU
                90.0, // Very high peak memory
                90.0, // High confidence
                "High resource utilization detected");

        when(rightSizingAnalyzer.getCurrentRecommendation()).thenReturn(rightSizingRecommendation);

        // When
        costOptimizationService.analyzeAndRecommend();

        // Then
        List<CostOptimizationService.CostOptimizationRecommendation> recommendations = costOptimizationService
                .getActiveRecommendations();

        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations.get(0).getId()).isEqualTo("RESOURCE_UPSIZE");
        assertThat(recommendations.get(0).getPriority())
                .isEqualTo(CostOptimizationService.CostOptimizationRecommendation.Priority.MEDIUM);
        assertThat(recommendations.get(0).getPotentialMonthlySavings()).isNegative(); // Additional cost
    }

    @Test
    void shouldCalculateOptimizationScoreBasedOnRecommendations() {
        // Given - no recommendations initially
        double initialScore = costOptimizationService.getOptimizationScore();
        assertThat(initialScore).isEqualTo(0.0);

        // When - add recommendations through analysis
        ResourceRightSizingAnalyzer.RightSizingRecommendation rightSizingRecommendation = new ResourceRightSizingAnalyzer.RightSizingRecommendation(
                java.time.LocalDateTime.now(),
                ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.DOWNSIZE,
                ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.DOWNSIZE,
                ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.DOWNSIZE,
                15.0, 20.0, 25.0, 30.0, 85.0,
                "Low resource utilization detected");

        when(rightSizingAnalyzer.getCurrentRecommendation()).thenReturn(rightSizingRecommendation);
        costOptimizationService.analyzeAndRecommend();

        // Then
        double scoreAfterRecommendations = costOptimizationService.getOptimizationScore();
        assertThat(scoreAfterRecommendations).isLessThan(100.0); // Score should decrease with recommendations
        assertThat(scoreAfterRecommendations).isGreaterThan(0.0);
    }

    @Test
    void shouldRegisterMetricsInMeterRegistry() {
        // Then
        assertThat(meterRegistry.find("cost.optimization.recommendations.generated").counter()).isNotNull();
        assertThat(meterRegistry.find("cost.optimization.score").gauge()).isNotNull();
    }

    @Test
    void shouldCreateRecommendationWithCorrectProperties() {
        // Given
        String id = "TEST_RECOMMENDATION";
        String title = "Test Recommendation";
        String description = "This is a test recommendation";
        CostOptimizationService.CostOptimizationRecommendation.Priority priority = CostOptimizationService.CostOptimizationRecommendation.Priority.HIGH;
        double savings = 100.0;
        CostOptimizationService.CostOptimizationRecommendation.Category category = CostOptimizationService.CostOptimizationRecommendation.Category.COMPUTE;

        // When
        CostOptimizationService.CostOptimizationRecommendation recommendation = new CostOptimizationService.CostOptimizationRecommendation(
                id, title, description, priority, savings, category);

        // Then
        assertThat(recommendation.getId()).isEqualTo(id);
        assertThat(recommendation.getTitle()).isEqualTo(title);
        assertThat(recommendation.getDescription()).isEqualTo(description);
        assertThat(recommendation.getPriority()).isEqualTo(priority);
        assertThat(recommendation.getPotentialMonthlySavings()).isEqualTo(savings);
        assertThat(recommendation.getCategory()).isEqualTo(category);
        assertThat(recommendation.getCreatedAt()).isNotNull();
    }
}