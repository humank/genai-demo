package solid.humank.genaidemo.interfaces.web.monitoring;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import solid.humank.genaidemo.infrastructure.observability.metrics.CostOptimizationService;
import solid.humank.genaidemo.infrastructure.observability.metrics.ResourceRightSizingAnalyzer;

/**
 * REST controller for cost optimization and resource right-sizing information
 */
@RestController
@RequestMapping("/api/monitoring/cost-optimization")
public class CostOptimizationController {

        private final CostOptimizationService costOptimizationService;
        private final ResourceRightSizingAnalyzer rightSizingAnalyzer;

        public CostOptimizationController(CostOptimizationService costOptimizationService,
                        ResourceRightSizingAnalyzer rightSizingAnalyzer) {
                this.costOptimizationService = costOptimizationService;
                this.rightSizingAnalyzer = rightSizingAnalyzer;
        }

        /**
         * Get current cost optimization score
         */
        @GetMapping("/score")
        public ResponseEntity<Map<String, Object>> getOptimizationScore() {
                double score = costOptimizationService.getOptimizationScore();

                return ResponseEntity.ok(Map.of(
                                "optimizationScore", score,
                                "status",
                                score >= 80 ? "excellent"
                                                : score >= 60 ? "good" : score >= 40 ? "fair" : "needs-improvement",
                                "timestamp", System.currentTimeMillis()));
        }

        /**
         * Get active cost optimization recommendations
         */
        @GetMapping("/recommendations")
        public ResponseEntity<List<CostOptimizationService.CostOptimizationRecommendation>> getRecommendations() {
                List<CostOptimizationService.CostOptimizationRecommendation> recommendations = costOptimizationService
                                .getActiveRecommendations();

                return ResponseEntity.ok(recommendations);
        }

        /**
         * Get current resource utilization and right-sizing recommendation
         */
        @GetMapping("/resource-utilization")
        public ResponseEntity<Map<String, Object>> getResourceUtilization() {
                double cpuUtilization = rightSizingAnalyzer.getCurrentCpuUtilization();
                double memoryUtilization = rightSizingAnalyzer.getCurrentMemoryUtilization();
                ResourceRightSizingAnalyzer.RightSizingRecommendation recommendation = rightSizingAnalyzer
                                .getCurrentRecommendation();

                Map<String, Object> response = new java.util.HashMap<>();
                response.put("currentCpuUtilization", cpuUtilization);
                response.put("currentMemoryUtilization", memoryUtilization);
                response.put("rightSizingRecommendation", recommendation != null ? Map.of(
                                "recommendation", recommendation.getOverallRecommendation(),
                                "confidenceScore", recommendation.getConfidenceScore(),
                                "reason", recommendation.getReason(),
                                "avgCpuUtilization", recommendation.getAvgCpuUtilization(),
                                "avgMemoryUtilization", recommendation.getAvgMemoryUtilization(),
                                "peakCpuUtilization", recommendation.getPeakCpuUtilization(),
                                "peakMemoryUtilization", recommendation.getPeakMemoryUtilization()) : null);
                response.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.ok(response);
        }

        /**
         * Get cost optimization summary
         */
        @GetMapping("/summary")
        public ResponseEntity<Map<String, Object>> getCostOptimizationSummary() {
                double score = costOptimizationService.getOptimizationScore();
                List<CostOptimizationService.CostOptimizationRecommendation> recommendations = costOptimizationService
                                .getActiveRecommendations();

                double totalPotentialSavings = recommendations.stream()
                                .mapToDouble(CostOptimizationService.CostOptimizationRecommendation::getPotentialMonthlySavings)
                                .sum();

                long highPriorityCount = recommendations.stream()
                                .filter(r -> r.getPriority() == CostOptimizationService.CostOptimizationRecommendation.Priority.HIGH)
                                .count();

                return ResponseEntity.ok(Map.of(
                                "optimizationScore", score,
                                "totalRecommendations", recommendations.size(),
                                "highPriorityRecommendations", highPriorityCount,
                                "totalPotentialMonthlySavings", totalPotentialSavings,
                                "status",
                                score >= 80 ? "excellent"
                                                : score >= 60 ? "good" : score >= 40 ? "fair" : "needs-improvement",
                                "timestamp", System.currentTimeMillis()));
        }
}