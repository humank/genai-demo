package solid.humank.genaidemo.infrastructure.performance;

import org.springframework.stereotype.Component;

/**
 * Mock performance metrics collector for tests
 */
@Component
public class PerformanceMetricsCollector {

    public void reset() {
        // Mock implementation
    }

    public boolean isPerformanceAtBaseline() {
        return true;
    }
}