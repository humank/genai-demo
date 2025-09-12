package solid.humank.genaidemo.infrastructure.performance;

import org.springframework.stereotype.Component;

/**
 * Mock system resource monitor for tests
 */
@Component
public class SystemResourceMonitor {

    public void startMonitoring() {
        // Mock implementation
    }

    public void stopMonitoring() {
        // Mock implementation
    }

    public double getCpuUtilization() {
        return 45.0; // Mock value
    }

    public double getMemoryUtilization() {
        return 60.0; // Mock value
    }

    public boolean isAutoScalingTriggered() {
        return true;
    }

    public boolean hasMemoryLeaks() {
        return false;
    }

    public boolean isSystemRecovered() {
        return true;
    }

    public boolean isAutoScalingDown() {
        return true;
    }

    public double getMemoryGrowthRate() {
        return 2.0; // Mock value
    }

    public double getConnectionPoolEfficiency() {
        return 95.0; // Mock value
    }

    public double getReservedInstanceUtilization() {
        return 85.0; // Mock value
    }

    public double getSpotInstanceSavings() {
        return 30.0; // Mock value
    }
}