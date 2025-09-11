package solid.humank.genaidemo.infrastructure.observability.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Analyzes resource usage patterns and provides right-sizing recommendations.
 * Implements requirement 12.3: WHEN resources are provisioned THEN the system
 * SHALL use right-sized instances based on workload
 */
@Component
public class ResourceRightSizingAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ResourceRightSizingAnalyzer.class);

    private final MeterRegistry meterRegistry;
    private final ResourceRightSizingProperties properties;
    private final MemoryMXBean memoryBean;
    private final OperatingSystemMXBean osBean;

    private final List<ResourceUsageSnapshot> usageHistory = new ArrayList<>();
    private final AtomicReference<RightSizingRecommendation> currentRecommendation = new AtomicReference<>();

    public ResourceRightSizingAnalyzer(MeterRegistry meterRegistry, ResourceRightSizingProperties properties) {
        this.meterRegistry = meterRegistry;
        this.properties = properties;
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();

        registerMetrics();
    }

    private void registerMetrics() {
        // Register custom metrics for resource utilization
        Gauge.builder("resource.cpu.utilization.percentage", this,
                ResourceRightSizingAnalyzer::getCurrentCpuUtilization)
                .description("Current CPU utilization percentage")
                .register(meterRegistry);

        Gauge.builder("resource.memory.utilization.percentage", this,
                ResourceRightSizingAnalyzer::getCurrentMemoryUtilization)
                .description("Current memory utilization percentage")
                .register(meterRegistry);

        Gauge.builder("resource.rightsizing.recommendation.score", this,
                ResourceRightSizingAnalyzer::getRightSizingScore)
                .description("Right-sizing recommendation score (0-100)")
                .register(meterRegistry);
    }

    /**
     * Analyzes resource usage patterns and generates recommendations
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void analyzeResourceUsage() {
        if (!properties.isAnalysisEnabled()) {
            return;
        }

        ResourceUsageSnapshot snapshot = captureResourceSnapshot();
        usageHistory.add(snapshot);

        // Keep only recent history
        if (usageHistory.size() > properties.getHistoryRetentionMinutes()) {
            usageHistory.remove(0);
        }

        // Generate recommendations every 15 minutes
        if (usageHistory.size() % 15 == 0) {
            generateRightSizingRecommendation();
        }
    }

    private ResourceUsageSnapshot captureResourceSnapshot() {
        double cpuUsage = getCurrentCpuUtilization();
        double memoryUsage = getCurrentMemoryUtilization();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();

        return new ResourceUsageSnapshot(
                LocalDateTime.now(),
                cpuUsage,
                memoryUsage,
                heapUsed,
                heapMax);
    }

    private void generateRightSizingRecommendation() {
        if (usageHistory.size() < properties.getMinSamplesForRecommendation()) {
            return;
        }

        logger.info("Generating right-sizing recommendation based on {} samples", usageHistory.size());

        // Calculate average resource utilization
        double avgCpuUsage = usageHistory.stream()
                .mapToDouble(ResourceUsageSnapshot::getCpuUtilization)
                .average()
                .orElse(0.0);

        double avgMemoryUsage = usageHistory.stream()
                .mapToDouble(ResourceUsageSnapshot::getMemoryUtilization)
                .average()
                .orElse(0.0);

        // Calculate peak utilization
        double peakCpuUsage = usageHistory.stream()
                .mapToDouble(ResourceUsageSnapshot::getCpuUtilization)
                .max()
                .orElse(0.0);

        double peakMemoryUsage = usageHistory.stream()
                .mapToDouble(ResourceUsageSnapshot::getMemoryUtilization)
                .max()
                .orElse(0.0);

        RightSizingRecommendation recommendation = analyzeAndRecommend(
                avgCpuUsage, avgMemoryUsage, peakCpuUsage, peakMemoryUsage);

        currentRecommendation.set(recommendation);
        logRecommendation(recommendation);
    }

    private RightSizingRecommendation analyzeAndRecommend(
            double avgCpu, double avgMemory, double peakCpu, double peakMemory) {

        RightSizingRecommendation.RecommendationType cpuRecommendation = analyzeCpuRecommendation(avgCpu, peakCpu);
        RightSizingRecommendation.RecommendationType memoryRecommendation = analyzeMemoryRecommendation(avgMemory,
                peakMemory);

        // Overall recommendation based on most constraining resource
        RightSizingRecommendation.RecommendationType overallRecommendation = determineOverallRecommendation(
                cpuRecommendation, memoryRecommendation);

        double confidenceScore = calculateConfidenceScore(avgCpu, avgMemory, peakCpu, peakMemory);

        return new RightSizingRecommendation(
                LocalDateTime.now(),
                overallRecommendation,
                cpuRecommendation,
                memoryRecommendation,
                avgCpu,
                avgMemory,
                peakCpu,
                peakMemory,
                confidenceScore,
                generateRecommendationReason(overallRecommendation, avgCpu, avgMemory, peakCpu, peakMemory));
    }

    private RightSizingRecommendation.RecommendationType analyzeCpuRecommendation(double avgCpu, double peakCpu) {
        if (avgCpu < properties.getCpuUnderutilizationThreshold() &&
                peakCpu < properties.getCpuUnderutilizationThreshold() * 1.5) {
            return RightSizingRecommendation.RecommendationType.DOWNSIZE;
        } else if (peakCpu > properties.getCpuOverutilizationThreshold()) {
            return RightSizingRecommendation.RecommendationType.UPSIZE;
        }
        return RightSizingRecommendation.RecommendationType.OPTIMAL;
    }

    private RightSizingRecommendation.RecommendationType analyzeMemoryRecommendation(double avgMemory,
            double peakMemory) {
        if (avgMemory < properties.getMemoryUnderutilizationThreshold() &&
                peakMemory < properties.getMemoryUnderutilizationThreshold() * 1.2) {
            return RightSizingRecommendation.RecommendationType.DOWNSIZE;
        } else if (peakMemory > properties.getMemoryOverutilizationThreshold()) {
            return RightSizingRecommendation.RecommendationType.UPSIZE;
        }
        return RightSizingRecommendation.RecommendationType.OPTIMAL;
    }

    private RightSizingRecommendation.RecommendationType determineOverallRecommendation(
            RightSizingRecommendation.RecommendationType cpu,
            RightSizingRecommendation.RecommendationType memory) {

        // If either resource needs upsizing, upsize overall
        if (cpu == RightSizingRecommendation.RecommendationType.UPSIZE ||
                memory == RightSizingRecommendation.RecommendationType.UPSIZE) {
            return RightSizingRecommendation.RecommendationType.UPSIZE;
        }

        // If both can be downsized, downsize overall
        if (cpu == RightSizingRecommendation.RecommendationType.DOWNSIZE &&
                memory == RightSizingRecommendation.RecommendationType.DOWNSIZE) {
            return RightSizingRecommendation.RecommendationType.DOWNSIZE;
        }

        return RightSizingRecommendation.RecommendationType.OPTIMAL;
    }

    private double calculateConfidenceScore(double avgCpu, double avgMemory, double peakCpu, double peakMemory) {
        // Higher confidence when utilization patterns are consistent
        double cpuVariance = Math.abs(peakCpu - avgCpu);
        double memoryVariance = Math.abs(peakMemory - avgMemory);

        // Lower variance = higher confidence
        double confidenceScore = 100.0 - (cpuVariance + memoryVariance) / 2.0;
        return Math.max(0.0, Math.min(100.0, confidenceScore));
    }

    private String generateRecommendationReason(
            RightSizingRecommendation.RecommendationType recommendation,
            double avgCpu, double avgMemory, double peakCpu, double peakMemory) {

        return switch (recommendation) {
            case UPSIZE -> String.format(
                    "Resource constraints detected: Peak CPU %.1f%%, Peak Memory %.1f%%. Consider upgrading instance size.",
                    peakCpu, peakMemory);
            case DOWNSIZE -> String.format(
                    "Resource underutilization detected: Avg CPU %.1f%%, Avg Memory %.1f%%. Consider downsizing to reduce costs.",
                    avgCpu, avgMemory);
            case OPTIMAL -> String.format(
                    "Resource utilization is optimal: Avg CPU %.1f%%, Avg Memory %.1f%%.",
                    avgCpu, avgMemory);
        };
    }

    private void logRecommendation(RightSizingRecommendation recommendation) {
        logger.info("Right-sizing recommendation: {} (confidence: {:.1f}%) - {}",
                recommendation.getOverallRecommendation(),
                recommendation.getConfidenceScore(),
                recommendation.getReason());

        if (recommendation.getConfidenceScore() > properties.getRecommendationConfidenceThreshold()) {
            logger.warn("High-confidence recommendation: Consider implementing the suggested changes");
        }
    }

    // Metric accessor methods
    public double getCurrentCpuUtilization() {
        // Use system load average as a proxy for CPU utilization
        double loadAverage = osBean.getSystemLoadAverage();
        int availableProcessors = osBean.getAvailableProcessors();

        if (loadAverage >= 0 && availableProcessors > 0) {
            return Math.min(100.0, (loadAverage / availableProcessors) * 100.0);
        }

        return 0.0; // Fallback if load average is not available
    }

    public double getCurrentMemoryUtilization() {
        long used = memoryBean.getHeapMemoryUsage().getUsed();
        long max = memoryBean.getHeapMemoryUsage().getMax();
        return max > 0 ? (double) used / max * 100.0 : 0.0;
    }

    public double getRightSizingScore() {
        RightSizingRecommendation recommendation = currentRecommendation.get();
        return recommendation != null ? recommendation.getConfidenceScore() : 0.0;
    }

    public RightSizingRecommendation getCurrentRecommendation() {
        return currentRecommendation.get();
    }

    /**
     * Snapshot of resource usage at a point in time
     */
    public static class ResourceUsageSnapshot {
        private final LocalDateTime timestamp;
        private final double cpuUtilization;
        private final double memoryUtilization;
        private final long heapUsed;
        private final long heapMax;

        public ResourceUsageSnapshot(LocalDateTime timestamp, double cpuUtilization,
                double memoryUtilization, long heapUsed, long heapMax) {
            this.timestamp = timestamp;
            this.cpuUtilization = cpuUtilization;
            this.memoryUtilization = memoryUtilization;
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
        }

        // Getters
        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public double getCpuUtilization() {
            return cpuUtilization;
        }

        public double getMemoryUtilization() {
            return memoryUtilization;
        }

        public long getHeapUsed() {
            return heapUsed;
        }

        public long getHeapMax() {
            return heapMax;
        }
    }

    /**
     * Right-sizing recommendation with analysis details
     */
    public static class RightSizingRecommendation {
        public enum RecommendationType {
            UPSIZE, DOWNSIZE, OPTIMAL
        }

        private final LocalDateTime timestamp;
        private final RecommendationType overallRecommendation;
        private final RecommendationType cpuRecommendation;
        private final RecommendationType memoryRecommendation;
        private final double avgCpuUtilization;
        private final double avgMemoryUtilization;
        private final double peakCpuUtilization;
        private final double peakMemoryUtilization;
        private final double confidenceScore;
        private final String reason;

        public RightSizingRecommendation(LocalDateTime timestamp, RecommendationType overallRecommendation,
                RecommendationType cpuRecommendation, RecommendationType memoryRecommendation,
                double avgCpuUtilization, double avgMemoryUtilization,
                double peakCpuUtilization, double peakMemoryUtilization,
                double confidenceScore, String reason) {
            this.timestamp = timestamp;
            this.overallRecommendation = overallRecommendation;
            this.cpuRecommendation = cpuRecommendation;
            this.memoryRecommendation = memoryRecommendation;
            this.avgCpuUtilization = avgCpuUtilization;
            this.avgMemoryUtilization = avgMemoryUtilization;
            this.peakCpuUtilization = peakCpuUtilization;
            this.peakMemoryUtilization = peakMemoryUtilization;
            this.confidenceScore = confidenceScore;
            this.reason = reason;
        }

        // Getters
        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public RecommendationType getOverallRecommendation() {
            return overallRecommendation;
        }

        public RecommendationType getCpuRecommendation() {
            return cpuRecommendation;
        }

        public RecommendationType getMemoryRecommendation() {
            return memoryRecommendation;
        }

        public double getAvgCpuUtilization() {
            return avgCpuUtilization;
        }

        public double getAvgMemoryUtilization() {
            return avgMemoryUtilization;
        }

        public double getPeakCpuUtilization() {
            return peakCpuUtilization;
        }

        public double getPeakMemoryUtilization() {
            return peakMemoryUtilization;
        }

        public double getConfidenceScore() {
            return confidenceScore;
        }

        public String getReason() {
            return reason;
        }
    }
}