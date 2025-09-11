package solid.humank.genaidemo.infrastructure.observability.metrics;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Service that analyzes system usage and provides cost optimization
 * recommendations.
 * Implements requirement 12.5: IF resource usage is low THEN the system SHALL
 * suggest optimization opportunities
 */
@Service
public class CostOptimizationService {

    private static final Logger logger = LoggerFactory.getLogger(CostOptimizationService.class);

    private final MeterRegistry meterRegistry;
    private final CostOptimizationProperties properties;
    private final ResourceRightSizingAnalyzer rightSizingAnalyzer;

    private final Counter optimizationRecommendationsCounter;
    private final AtomicReference<Double> currentOptimizationScore = new AtomicReference<>(0.0);
    private final List<CostOptimizationRecommendation> activeRecommendations = new ArrayList<>();

    public CostOptimizationService(MeterRegistry meterRegistry,
            CostOptimizationProperties properties,
            ResourceRightSizingAnalyzer rightSizingAnalyzer) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
        this.rightSizingAnalyzer = rightSizingAnalyzer;

        this.optimizationRecommendationsCounter = Counter.builder("cost.optimization.recommendations.generated")
                .description("Number of cost optimization recommendations generated")
                .register(meterRegistry);

        Gauge.builder("cost.optimization.score", this, CostOptimizationService::getOptimizationScore)
                .description("Current cost optimization score (0-100)")
                .register(meterRegistry);
    }

    /**
     * Analyzes system usage and generates cost optimization recommendations
     */
    @Scheduled(fixedRate = 900000) // Every 15 minutes
    public void analyzeAndRecommend() {
        if (!properties.isEnabled()) {
            return;
        }

        logger.info("Starting cost optimization analysis");

        List<CostOptimizationRecommendation> newRecommendations = new ArrayList<>();

        // Analyze resource utilization
        newRecommendations.addAll(analyzeResourceUtilization());

        // Analyze logging costs
        newRecommendations.addAll(analyzeLoggingCosts());

        // Analyze metrics costs
        newRecommendations.addAll(analyzeMetricsCosts());

        // Analyze storage costs
        newRecommendations.addAll(analyzeStorageCosts());

        // Analyze network costs
        newRecommendations.addAll(analyzeNetworkCosts());

        // Update active recommendations
        updateActiveRecommendations(newRecommendations);

        // Calculate optimization score
        double score = calculateOptimizationScore();
        currentOptimizationScore.set(score);

        // Log recommendations
        logRecommendations(newRecommendations, score);

        optimizationRecommendationsCounter.increment(newRecommendations.size());
    }

    private List<CostOptimizationRecommendation> analyzeResourceUtilization() {
        List<CostOptimizationRecommendation> recommendations = new ArrayList<>();

        ResourceRightSizingAnalyzer.RightSizingRecommendation rightSizing = rightSizingAnalyzer
                .getCurrentRecommendation();

        if (rightSizing != null) {
            switch (rightSizing.getOverallRecommendation()) {
                case DOWNSIZE -> recommendations.add(new CostOptimizationRecommendation(
                        "RESOURCE_DOWNSIZE",
                        "Resource Downsizing",
                        String.format("Consider downsizing instances. Current utilization: CPU %.1f%%, Memory %.1f%%",
                                rightSizing.getAvgCpuUtilization(), rightSizing.getAvgMemoryUtilization()),
                        CostOptimizationRecommendation.Priority.HIGH,
                        estimateDownsizingSavings(rightSizing),
                        CostOptimizationRecommendation.Category.COMPUTE));

                case UPSIZE -> recommendations.add(new CostOptimizationRecommendation(
                        "RESOURCE_UPSIZE",
                        "Resource Upsizing Required",
                        String.format(
                                "Consider upsizing instances to avoid performance issues. Peak utilization: CPU %.1f%%, Memory %.1f%%",
                                rightSizing.getPeakCpuUtilization(), rightSizing.getPeakMemoryUtilization()),
                        CostOptimizationRecommendation.Priority.MEDIUM,
                        -estimateUpsizingCost(rightSizing), // Negative savings (additional cost)
                        CostOptimizationRecommendation.Category.COMPUTE));
            }
        }

        return recommendations;
    }

    private List<CostOptimizationRecommendation> analyzeLoggingCosts() {
        List<CostOptimizationRecommendation> recommendations = new ArrayList<>();

        // Analyze log volume and suggest optimizations
        if (isHighLogVolume()) {
            recommendations.add(new CostOptimizationRecommendation(
                    "LOG_LEVEL_OPTIMIZATION",
                    "Optimize Log Levels",
                    "High log volume detected. Consider reducing log levels in production to WARN or ERROR for non-critical components.",
                    CostOptimizationRecommendation.Priority.MEDIUM,
                    estimateLogOptimizationSavings(),
                    CostOptimizationRecommendation.Category.LOGGING));
        }

        if (isLongLogRetention()) {
            recommendations.add(new CostOptimizationRecommendation(
                    "LOG_RETENTION_OPTIMIZATION",
                    "Optimize Log Retention",
                    "Consider implementing tiered storage for logs: CloudWatch (7 days) → S3 Standard (30 days) → S3 Glacier (long-term).",
                    CostOptimizationRecommendation.Priority.HIGH,
                    estimateLogRetentionSavings(),
                    CostOptimizationRecommendation.Category.LOGGING));
        }

        return recommendations;
    }

    private List<CostOptimizationRecommendation> analyzeMetricsCosts() {
        List<CostOptimizationRecommendation> recommendations = new ArrayList<>();

        if (isHighMetricsCardinality()) {
            recommendations.add(new CostOptimizationRecommendation(
                    "METRICS_SAMPLING",
                    "Implement Metrics Sampling",
                    "High metrics cardinality detected. Consider implementing sampling for non-critical metrics to reduce CloudWatch costs.",
                    CostOptimizationRecommendation.Priority.MEDIUM,
                    estimateMetricsSamplingSavings(),
                    CostOptimizationRecommendation.Category.MONITORING));
        }

        return recommendations;
    }

    private List<CostOptimizationRecommendation> analyzeStorageCosts() {
        List<CostOptimizationRecommendation> recommendations = new ArrayList<>();

        recommendations.add(new CostOptimizationRecommendation(
                "STORAGE_LIFECYCLE",
                "Implement S3 Lifecycle Policies",
                "Configure S3 lifecycle policies to automatically transition data to cheaper storage classes (IA, Glacier, Deep Archive).",
                CostOptimizationRecommendation.Priority.HIGH,
                estimateStorageLifecycleSavings(),
                CostOptimizationRecommendation.Category.STORAGE));

        return recommendations;
    }

    private List<CostOptimizationRecommendation> analyzeNetworkCosts() {
        List<CostOptimizationRecommendation> recommendations = new ArrayList<>();

        recommendations.add(new CostOptimizationRecommendation(
                "NETWORK_OPTIMIZATION",
                "Optimize Network Usage",
                "Consider using VPC endpoints for AWS services and CloudFront for static content to reduce data transfer costs.",
                CostOptimizationRecommendation.Priority.LOW,
                estimateNetworkOptimizationSavings(),
                CostOptimizationRecommendation.Category.NETWORK));

        return recommendations;
    }

    private void updateActiveRecommendations(List<CostOptimizationRecommendation> newRecommendations) {
        synchronized (activeRecommendations) {
            activeRecommendations.clear();
            activeRecommendations.addAll(newRecommendations);
        }
    }

    private double calculateOptimizationScore() {
        if (activeRecommendations.isEmpty()) {
            return 100.0; // Perfect score if no recommendations
        }

        // Calculate score based on potential savings and priority
        double totalPotentialSavings = activeRecommendations.stream()
                .mapToDouble(CostOptimizationRecommendation::getPotentialMonthlySavings)
                .sum();

        double highPriorityCount = activeRecommendations.stream()
                .mapToLong(r -> r.getPriority() == CostOptimizationRecommendation.Priority.HIGH ? 1 : 0)
                .sum();

        // Score decreases with more recommendations and higher potential savings
        double score = 100.0 - (totalPotentialSavings * 0.1) - (highPriorityCount * 10);
        return Math.max(0.0, Math.min(100.0, score));
    }

    private void logRecommendations(List<CostOptimizationRecommendation> recommendations, double score) {
        logger.info("Cost optimization analysis complete. Score: {:.1f}/100", score);

        if (!recommendations.isEmpty()) {
            logger.info("Generated {} cost optimization recommendations:", recommendations.size());
            recommendations.forEach(rec -> logger.info("- {} ({}): {} - Potential savings: ${:.2f}/month",
                    rec.getTitle(),
                    rec.getPriority(),
                    rec.getDescription(),
                    rec.getPotentialMonthlySavings()));
        } else {
            logger.info("No cost optimization opportunities identified at this time");
        }
    }

    // Helper methods for analysis
    private boolean isHighLogVolume() {
        // Implementation would check actual log volume metrics
        return false; // Placeholder
    }

    private boolean isLongLogRetention() {
        // Implementation would check log retention settings
        return true; // Always recommend tiered storage
    }

    private boolean isHighMetricsCardinality() {
        // Implementation would check metrics cardinality
        return false; // Placeholder
    }

    // Cost estimation methods
    private double estimateDownsizingSavings(ResourceRightSizingAnalyzer.RightSizingRecommendation rightSizing) {
        // Estimate 30-50% savings from downsizing
        return properties.getEstimatedMonthlyCost() * 0.4;
    }

    private double estimateUpsizingCost(ResourceRightSizingAnalyzer.RightSizingRecommendation rightSizing) {
        // Estimate 50-100% additional cost from upsizing
        return properties.getEstimatedMonthlyCost() * 0.75;
    }

    private double estimateLogOptimizationSavings() {
        return properties.getEstimatedMonthlyCost() * 0.15; // 15% savings
    }

    private double estimateLogRetentionSavings() {
        return properties.getEstimatedMonthlyCost() * 0.25; // 25% savings
    }

    private double estimateMetricsSamplingSavings() {
        return properties.getEstimatedMonthlyCost() * 0.10; // 10% savings
    }

    private double estimateStorageLifecycleSavings() {
        return properties.getEstimatedMonthlyCost() * 0.20; // 20% savings
    }

    private double estimateNetworkOptimizationSavings() {
        return properties.getEstimatedMonthlyCost() * 0.05; // 5% savings
    }

    // Public accessors
    public double getOptimizationScore() {
        return currentOptimizationScore.get();
    }

    public List<CostOptimizationRecommendation> getActiveRecommendations() {
        synchronized (activeRecommendations) {
            return new ArrayList<>(activeRecommendations);
        }
    }

    /**
     * Cost optimization recommendation
     */
    public static class CostOptimizationRecommendation {
        public enum Priority {
            HIGH, MEDIUM, LOW
        }

        public enum Category {
            COMPUTE, STORAGE, NETWORK, MONITORING, LOGGING
        }

        private final String id;
        private final String title;
        private final String description;
        private final Priority priority;
        private final double potentialMonthlySavings;
        private final Category category;
        private final LocalDateTime createdAt;

        public CostOptimizationRecommendation(String id, String title, String description,
                Priority priority, double potentialMonthlySavings, Category category) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.priority = priority;
            this.potentialMonthlySavings = potentialMonthlySavings;
            this.category = category;
            this.createdAt = LocalDateTime.now();
        }

        // Getters
        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public Priority getPriority() {
            return priority;
        }

        public double getPotentialMonthlySavings() {
            return potentialMonthlySavings;
        }

        public Category getCategory() {
            return category;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
}