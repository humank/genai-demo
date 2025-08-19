package solid.humank.genaidemo.testutils.isolation;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 測試隔離配置
 * 提供測試隔離機制的配置選項
 */
public class TestIsolationConfiguration {

    private static final TestIsolationConfiguration DEFAULT_CONFIG = new TestIsolationConfiguration();

    private Duration cleanupTimeout = Duration.ofSeconds(30);
    private boolean enableParallelExecution = false;
    private boolean enableEventIsolation = true;
    private boolean enableResourceAutoCleanup = true;
    private boolean enableStrictIsolation = true;
    private int maxRetryAttempts = 3;
    private Duration retryDelay = Duration.ofMillis(100);

    private TestIsolationConfiguration() {
        // Private constructor for singleton pattern
    }

    /**
     * 獲取預設配置
     */
    public static TestIsolationConfiguration getDefault() {
        return DEFAULT_CONFIG;
    }

    /**
     * 創建自定義配置
     */
    public static TestIsolationConfiguration custom() {
        TestIsolationConfiguration config = new TestIsolationConfiguration();
        config.cleanupTimeout = DEFAULT_CONFIG.cleanupTimeout;
        config.enableParallelExecution = DEFAULT_CONFIG.enableParallelExecution;
        config.enableEventIsolation = DEFAULT_CONFIG.enableEventIsolation;
        config.enableResourceAutoCleanup = DEFAULT_CONFIG.enableResourceAutoCleanup;
        config.enableStrictIsolation = DEFAULT_CONFIG.enableStrictIsolation;
        config.maxRetryAttempts = DEFAULT_CONFIG.maxRetryAttempts;
        config.retryDelay = DEFAULT_CONFIG.retryDelay;
        return config;
    }

    // Getters
    public Duration getCleanupTimeout() {
        return cleanupTimeout;
    }

    public boolean isParallelExecutionEnabled() {
        return enableParallelExecution;
    }

    public boolean isEventIsolationEnabled() {
        return enableEventIsolation;
    }

    public boolean isResourceAutoCleanupEnabled() {
        return enableResourceAutoCleanup;
    }

    public boolean isStrictIsolationEnabled() {
        return enableStrictIsolation;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    // Fluent setters for custom configuration
    public TestIsolationConfiguration withCleanupTimeout(Duration timeout) {
        this.cleanupTimeout = timeout;
        return this;
    }

    public TestIsolationConfiguration withCleanupTimeout(long timeout, TimeUnit unit) {
        this.cleanupTimeout = Duration.ofMillis(unit.toMillis(timeout));
        return this;
    }

    public TestIsolationConfiguration withParallelExecution(boolean enabled) {
        this.enableParallelExecution = enabled;
        return this;
    }

    public TestIsolationConfiguration withEventIsolation(boolean enabled) {
        this.enableEventIsolation = enabled;
        return this;
    }

    public TestIsolationConfiguration withResourceAutoCleanup(boolean enabled) {
        this.enableResourceAutoCleanup = enabled;
        return this;
    }

    public TestIsolationConfiguration withStrictIsolation(boolean enabled) {
        this.enableStrictIsolation = enabled;
        return this;
    }

    public TestIsolationConfiguration withMaxRetryAttempts(int attempts) {
        this.maxRetryAttempts = Math.max(0, attempts);
        return this;
    }

    public TestIsolationConfiguration withRetryDelay(Duration delay) {
        this.retryDelay = delay;
        return this;
    }

    public TestIsolationConfiguration withRetryDelay(long delay, TimeUnit unit) {
        this.retryDelay = Duration.ofMillis(unit.toMillis(delay));
        return this;
    }

    @Override
    public String toString() {
        return "TestIsolationConfiguration{" +
                "cleanupTimeout=" + cleanupTimeout +
                ", enableParallelExecution=" + enableParallelExecution +
                ", enableEventIsolation=" + enableEventIsolation +
                ", enableResourceAutoCleanup=" + enableResourceAutoCleanup +
                ", enableStrictIsolation=" + enableStrictIsolation +
                ", maxRetryAttempts=" + maxRetryAttempts +
                ", retryDelay=" + retryDelay +
                '}';
    }
}