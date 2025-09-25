package solid.humank.genaidemo.infrastructure.common.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Aurora 樂觀鎖配置屬性
 * 
 * 提供 Aurora 樂觀鎖和讀寫分離的配置選項
 * 
 * 配置範例：
 * ```yaml
 * aurora:
 *   optimistic-locking:
 *     enabled: true
 *     default-max-retries: 3
 *     enable-conflict-detection: true
 *     enable-performance-monitoring: true
 *   read-write-separation:
 *     enabled: true
 *   datasource:
 *     write:
 *       url: jdbc:postgresql://aurora-cluster-writer.cluster-xxx.region.rds.amazonaws.com:5432/genaidemo
 *       username: ${DB_USERNAME}
 *       password: ${DB_PASSWORD}
 *       driver-class-name: org.postgresql.Driver
 *     read:
 *       url: jdbc:postgresql://aurora-cluster-reader.cluster-xxx.region.rds.amazonaws.com:5432/genaidemo
 *       username: ${DB_USERNAME}
 *       password: ${DB_PASSWORD}
 *       driver-class-name: org.postgresql.Driver
 * ```
 * 
 * 建立日期: 2025年9月24日 上午10:18 (台北時間)
 * 需求: 1.1 - 並發控制機制全面重構
 * 
 * @author Kiro AI Assistant
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "aurora")
public class AuroraOptimisticLockingProperties {

    /**
     * 樂觀鎖配置
     */
    private OptimisticLocking optimisticLocking = new OptimisticLocking();

    /**
     * 讀寫分離配置
     */
    private ReadWriteSeparation readWriteSeparation = new ReadWriteSeparation();

    public OptimisticLocking getOptimisticLocking() {
        return optimisticLocking;
    }

    public void setOptimisticLocking(OptimisticLocking optimisticLocking) {
        this.optimisticLocking = optimisticLocking;
    }

    public ReadWriteSeparation getReadWriteSeparation() {
        return readWriteSeparation;
    }

    public void setReadWriteSeparation(ReadWriteSeparation readWriteSeparation) {
        this.readWriteSeparation = readWriteSeparation;
    }

    /**
     * 樂觀鎖配置類
     */
    public static class OptimisticLocking {
        
        /**
         * 是否啟用樂觀鎖機制
         */
        private boolean enabled = true;

        /**
         * 默認最大重試次數
         */
        private int defaultMaxRetries = 3;

        /**
         * 是否啟用衝突檢測
         */
        private boolean enableConflictDetection = true;

        /**
         * 是否啟用性能監控
         */
        private boolean enablePerformanceMonitoring = true;

        /**
         * 重試延遲基數（毫秒）
         */
        private long retryDelayBaseMs = 100L;

        /**
         * 最大重試延遲（毫秒）
         */
        private long maxRetryDelayMs = 5000L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getDefaultMaxRetries() {
            return defaultMaxRetries;
        }

        public void setDefaultMaxRetries(int defaultMaxRetries) {
            this.defaultMaxRetries = defaultMaxRetries;
        }

        public boolean isEnableConflictDetection() {
            return enableConflictDetection;
        }

        public void setEnableConflictDetection(boolean enableConflictDetection) {
            this.enableConflictDetection = enableConflictDetection;
        }

        public boolean isEnablePerformanceMonitoring() {
            return enablePerformanceMonitoring;
        }

        public void setEnablePerformanceMonitoring(boolean enablePerformanceMonitoring) {
            this.enablePerformanceMonitoring = enablePerformanceMonitoring;
        }

        public long getRetryDelayBaseMs() {
            return retryDelayBaseMs;
        }

        public void setRetryDelayBaseMs(long retryDelayBaseMs) {
            this.retryDelayBaseMs = retryDelayBaseMs;
        }

        public long getMaxRetryDelayMs() {
            return maxRetryDelayMs;
        }

        public void setMaxRetryDelayMs(long maxRetryDelayMs) {
            this.maxRetryDelayMs = maxRetryDelayMs;
        }
    }

    /**
     * 讀寫分離配置類
     */
    public static class ReadWriteSeparation {
        
        /**
         * 是否啟用讀寫分離
         */
        private boolean enabled = false;

        /**
         * 讀取數據源權重（用於負載均衡）
         */
        private int readDataSourceWeight = 1;

        /**
         * 是否在事務中強制使用寫入數據源
         */
        private boolean forceWriteInTransaction = true;

        /**
         * 讀取數據源連接超時（毫秒）
         */
        private long readConnectionTimeoutMs = 5000L;

        /**
         * 寫入數據源連接超時（毫秒）
         */
        private long writeConnectionTimeoutMs = 10000L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getReadDataSourceWeight() {
            return readDataSourceWeight;
        }

        public void setReadDataSourceWeight(int readDataSourceWeight) {
            this.readDataSourceWeight = readDataSourceWeight;
        }

        public boolean isForceWriteInTransaction() {
            return forceWriteInTransaction;
        }

        public void setForceWriteInTransaction(boolean forceWriteInTransaction) {
            this.forceWriteInTransaction = forceWriteInTransaction;
        }

        public long getReadConnectionTimeoutMs() {
            return readConnectionTimeoutMs;
        }

        public void setReadConnectionTimeoutMs(long readConnectionTimeoutMs) {
            this.readConnectionTimeoutMs = readConnectionTimeoutMs;
        }

        public long getWriteConnectionTimeoutMs() {
            return writeConnectionTimeoutMs;
        }

        public void setWriteConnectionTimeoutMs(long writeConnectionTimeoutMs) {
            this.writeConnectionTimeoutMs = writeConnectionTimeoutMs;
        }
    }
}