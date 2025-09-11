package solid.humank.genaidemo.infrastructure.observability.metrics;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for cost optimization
 */
@Configuration
@ConfigurationProperties(prefix = "genai-demo.observability.cost-optimization")
public class CostOptimizationProperties {

    private boolean enabled = true;
    private double estimatedMonthlyCost = 500.0; // Default estimated monthly cost
    private double savingsThreshold = 10.0; // Minimum savings to recommend (USD)
    private int analysisIntervalMinutes = 15;
    private Map<String, Double> serviceCosts = Map.of(
            "eks", 200.0,
            "rds", 150.0,
            "cloudwatch", 100.0,
            "s3", 50.0,
            "msk", 100.0);

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getEstimatedMonthlyCost() {
        return estimatedMonthlyCost;
    }

    public void setEstimatedMonthlyCost(double estimatedMonthlyCost) {
        this.estimatedMonthlyCost = estimatedMonthlyCost;
    }

    public double getSavingsThreshold() {
        return savingsThreshold;
    }

    public void setSavingsThreshold(double savingsThreshold) {
        this.savingsThreshold = savingsThreshold;
    }

    public int getAnalysisIntervalMinutes() {
        return analysisIntervalMinutes;
    }

    public void setAnalysisIntervalMinutes(int analysisIntervalMinutes) {
        this.analysisIntervalMinutes = analysisIntervalMinutes;
    }

    public Map<String, Double> getServiceCosts() {
        return serviceCosts;
    }

    public void setServiceCosts(Map<String, Double> serviceCosts) {
        this.serviceCosts = serviceCosts;
    }
}