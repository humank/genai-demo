package solid.humank.genaidemo.infrastructure.performance;

import org.springframework.stereotype.Component;

/**
 * Mock load test configuration for tests
 */
@Component
public class LoadTestConfiguration {

    public int getMaxConcurrentUsers() {
        return 100;
    }

    public int getDefaultTestDuration() {
        return 60; // seconds
    }
}