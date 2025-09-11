package solid.humank.genaidemo.infrastructure.observability.metrics;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for resource right-sizing analysis
 */
@Configuration
@ConfigurationProperties(prefix = "genai-demo.observability.rightsizing")
public class ResourceRightSizingProperties {

    private boolean analysisEnabled = true;
    private int historyRetentionMinutes = 60;
    private int minSamplesForRecommendation = 15;
    private double cpuUnderutilizationThreshold = 20.0; // 20%
    private double cpuOverutilizationThreshold = 80.0; // 80%
    private double memoryUnderutilizationThreshold = 30.0; // 30%
    private double memoryOverutilizationThreshold = 85.0; // 85%
    private double recommendationConfidenceThreshold = 75.0; // 75%

    // Getters and setters
    public boolean isAnalysisEnabled() {
        return analysisEnabled;
    }

    public void setAnalysisEnabled(boolean analysisEnabled) {
        this.analysisEnabled = analysisEnabled;
    }

    public int getHistoryRetentionMinutes() {
        return historyRetentionMinutes;
    }

    public void setHistoryRetentionMinutes(int historyRetentionMinutes) {
        this.historyRetentionMinutes = historyRetentionMinutes;
    }

    public int getMinSamplesForRecommendation() {
        return minSamplesForRecommendation;
    }

    public void setMinSamplesForRecommendation(int minSamplesForRecommendation) {
        this.minSamplesForRecommendation = minSamplesForRecommendation;
    }

    public double getCpuUnderutilizationThreshold() {
        return cpuUnderutilizationThreshold;
    }

    public void setCpuUnderutilizationThreshold(double cpuUnderutilizationThreshold) {
        this.cpuUnderutilizationThreshold = cpuUnderutilizationThreshold;
    }

    public double getCpuOverutilizationThreshold() {
        return cpuOverutilizationThreshold;
    }

    public void setCpuOverutilizationThreshold(double cpuOverutilizationThreshold) {
        this.cpuOverutilizationThreshold = cpuOverutilizationThreshold;
    }

    public double getMemoryUnderutilizationThreshold() {
        return memoryUnderutilizationThreshold;
    }

    public void setMemoryUnderutilizationThreshold(double memoryUnderutilizationThreshold) {
        this.memoryUnderutilizationThreshold = memoryUnderutilizationThreshold;
    }

    public double getMemoryOverutilizationThreshold() {
        return memoryOverutilizationThreshold;
    }

    public void setMemoryOverutilizationThreshold(double memoryOverutilizationThreshold) {
        this.memoryOverutilizationThreshold = memoryOverutilizationThreshold;
    }

    public double getRecommendationConfidenceThreshold() {
        return recommendationConfidenceThreshold;
    }

    public void setRecommendationConfidenceThreshold(double recommendationConfidenceThreshold) {
        this.recommendationConfidenceThreshold = recommendationConfidenceThreshold;
    }
}