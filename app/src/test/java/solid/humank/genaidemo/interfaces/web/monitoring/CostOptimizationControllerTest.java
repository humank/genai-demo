package solid.humank.genaidemo.interfaces.web.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import solid.humank.genaidemo.infrastructure.observability.metrics.CostOptimizationService;
import solid.humank.genaidemo.infrastructure.observability.metrics.ResourceRightSizingAnalyzer;

/**
 * Unit tests for CostOptimizationController
 */
@ExtendWith(MockitoExtension.class)
class CostOptimizationControllerTest {

        @Mock
        private CostOptimizationService costOptimizationService;

        @Mock
        private ResourceRightSizingAnalyzer rightSizingAnalyzer;

        private CostOptimizationController controller;

        @BeforeEach
        void setUp() {
                controller = new CostOptimizationController(costOptimizationService, rightSizingAnalyzer);
        }

        @Test
        void shouldReturnOptimizationScore() {
                // Given
                double expectedScore = 85.5;
                when(costOptimizationService.getOptimizationScore()).thenReturn(expectedScore);

                // When
                ResponseEntity<Map<String, Object>> response = controller.getOptimizationScore();

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().get("optimizationScore")).isEqualTo(expectedScore);
                assertThat(response.getBody().get("status")).isEqualTo("excellent");
                assertThat(response.getBody().get("timestamp")).isNotNull();
        }

        @Test
        void shouldReturnCorrectStatusForDifferentScores() {
                // Test excellent score (>= 80)
                when(costOptimizationService.getOptimizationScore()).thenReturn(85.0);
                ResponseEntity<Map<String, Object>> response = controller.getOptimizationScore();
                assertThat(response.getBody().get("status")).isEqualTo("excellent");

                // Test good score (>= 60, < 80)
                when(costOptimizationService.getOptimizationScore()).thenReturn(70.0);
                response = controller.getOptimizationScore();
                assertThat(response.getBody().get("status")).isEqualTo("good");

                // Test fair score (>= 40, < 60)
                when(costOptimizationService.getOptimizationScore()).thenReturn(50.0);
                response = controller.getOptimizationScore();
                assertThat(response.getBody().get("status")).isEqualTo("fair");

                // Test needs improvement score (< 40)
                when(costOptimizationService.getOptimizationScore()).thenReturn(30.0);
                response = controller.getOptimizationScore();
                assertThat(response.getBody().get("status")).isEqualTo("needs-improvement");
        }

        @Test
        void shouldReturnRecommendations() {
                // Given
                List<CostOptimizationService.CostOptimizationRecommendation> recommendations = List.of(
                                new CostOptimizationService.CostOptimizationRecommendation(
                                                "TEST_REC_1",
                                                "Test Recommendation 1",
                                                "This is a test recommendation",
                                                CostOptimizationService.CostOptimizationRecommendation.Priority.HIGH,
                                                100.0,
                                                CostOptimizationService.CostOptimizationRecommendation.Category.COMPUTE));
                when(costOptimizationService.getActiveRecommendations()).thenReturn(recommendations);

                // When
                ResponseEntity<List<CostOptimizationService.CostOptimizationRecommendation>> response = controller
                                .getRecommendations();

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isEqualTo(recommendations);
        }

        @Test
        void shouldReturnResourceUtilization() {
                // Given
                double cpuUtilization = 45.5;
                double memoryUtilization = 67.8;
                ResourceRightSizingAnalyzer.RightSizingRecommendation recommendation = new ResourceRightSizingAnalyzer.RightSizingRecommendation(
                                LocalDateTime.now(),
                                ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.OPTIMAL,
                                ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.OPTIMAL,
                                ResourceRightSizingAnalyzer.RightSizingRecommendation.RecommendationType.OPTIMAL,
                                40.0, 60.0, 50.0, 70.0, 90.0,
                                "Resource utilization is optimal");

                when(rightSizingAnalyzer.getCurrentCpuUtilization()).thenReturn(cpuUtilization);
                when(rightSizingAnalyzer.getCurrentMemoryUtilization()).thenReturn(memoryUtilization);
                when(rightSizingAnalyzer.getCurrentRecommendation()).thenReturn(recommendation);

                // When
                ResponseEntity<Map<String, Object>> response = controller.getResourceUtilization();

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().get("currentCpuUtilization")).isEqualTo(cpuUtilization);
                assertThat(response.getBody().get("currentMemoryUtilization")).isEqualTo(memoryUtilization);
                assertThat(response.getBody().get("rightSizingRecommendation")).isNotNull();
                assertThat(response.getBody().get("timestamp")).isNotNull();
        }

        @Test
        void shouldReturnResourceUtilizationWithoutRecommendation() {
                // Given
                double cpuUtilization = 45.5;
                double memoryUtilization = 67.8;
                when(rightSizingAnalyzer.getCurrentCpuUtilization()).thenReturn(cpuUtilization);
                when(rightSizingAnalyzer.getCurrentMemoryUtilization()).thenReturn(memoryUtilization);
                when(rightSizingAnalyzer.getCurrentRecommendation()).thenReturn(null);

                // When
                ResponseEntity<Map<String, Object>> response = controller.getResourceUtilization();

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().get("currentCpuUtilization")).isEqualTo(cpuUtilization);
                assertThat(response.getBody().get("currentMemoryUtilization")).isEqualTo(memoryUtilization);
                assertThat(response.getBody().get("rightSizingRecommendation")).isNull();
        }

        @Test
        void shouldReturnCostOptimizationSummary() {
                // Given
                double score = 75.0;
                List<CostOptimizationService.CostOptimizationRecommendation> recommendations = List.of(
                                new CostOptimizationService.CostOptimizationRecommendation(
                                                "HIGH_REC",
                                                "High Priority Recommendation",
                                                "This is a high priority recommendation",
                                                CostOptimizationService.CostOptimizationRecommendation.Priority.HIGH,
                                                150.0,
                                                CostOptimizationService.CostOptimizationRecommendation.Category.COMPUTE),
                                new CostOptimizationService.CostOptimizationRecommendation(
                                                "MED_REC",
                                                "Medium Priority Recommendation",
                                                "This is a medium priority recommendation",
                                                CostOptimizationService.CostOptimizationRecommendation.Priority.MEDIUM,
                                                75.0,
                                                CostOptimizationService.CostOptimizationRecommendation.Category.STORAGE));
                when(costOptimizationService.getOptimizationScore()).thenReturn(score);
                when(costOptimizationService.getActiveRecommendations()).thenReturn(recommendations);

                // When
                ResponseEntity<Map<String, Object>> response = controller.getCostOptimizationSummary();

                // Then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().get("optimizationScore")).isEqualTo(score);
                assertThat(response.getBody().get("totalRecommendations")).isEqualTo(2);
                assertThat(response.getBody().get("highPriorityRecommendations")).isEqualTo(1L);
                assertThat(response.getBody().get("totalPotentialMonthlySavings")).isEqualTo(225.0);
                assertThat(response.getBody().get("status")).isEqualTo("good");
                assertThat(response.getBody().get("timestamp")).isNotNull();
        }
}