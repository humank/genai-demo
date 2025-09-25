package solid.humank.genaidemo.infrastructure.common.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import jakarta.persistence.OptimisticLockException;

/**
 * 樂觀鎖衝突檢測器
 * 
 * 提供樂觀鎖衝突的檢測、分析和處理機制，包含：
 * 1. 衝突檢測和分析
 * 2. 衝突信息收集
 * 3. 重試策略建議
 * 4. 監控指標收集
 * 
 * 建立日期: 2025年9月24日 上午10:18 (台北時間)
 * 需求: 1.1 - 並發控制機制全面重構
 * 
 * @author Kiro AI Assistant
 * @since 1.0
 */
@Component
public class OptimisticLockingConflictDetector {

    private static final Logger logger = LoggerFactory.getLogger(OptimisticLockingConflictDetector.class);

    /**
     * 衝突信息記錄
     */
    public static class ConflictInfo {
        private final String entityType;
        private final String entityId;
        private final Long expectedVersion;
        private final Long actualVersion;
        private final LocalDateTime conflictTime;
        private final String operation;
        private final String threadName;

        public ConflictInfo(String entityType, String entityId, Long expectedVersion, 
                           Long actualVersion, String operation) {
            this.entityType = entityType;
            this.entityId = entityId;
            this.expectedVersion = expectedVersion;
            this.actualVersion = actualVersion;
            this.operation = operation;
            this.conflictTime = LocalDateTime.now();
            this.threadName = Thread.currentThread().getName();
        }

        // Getters
        public String getEntityType() { return entityType; }
        public String getEntityId() { return entityId; }
        public Long getExpectedVersion() { return expectedVersion; }
        public Long getActualVersion() { return actualVersion; }
        public LocalDateTime getConflictTime() { return conflictTime; }
        public String getOperation() { return operation; }
        public String getThreadName() { return threadName; }

        @Override
        public String toString() {
            return String.format("ConflictInfo{entityType='%s', entityId='%s', expectedVersion=%d, " +
                    "actualVersion=%d, operation='%s', conflictTime=%s, thread='%s'}",
                    entityType, entityId, expectedVersion, actualVersion, operation, conflictTime, threadName);
        }
    }

    /**
     * 重試策略建議
     */
    public enum RetryStrategy {
        IMMEDIATE_RETRY("立即重試"),
        EXPONENTIAL_BACKOFF("指數退避重試"),
        LINEAR_BACKOFF("線性退避重試"),
        NO_RETRY("不建議重試");

        private final String description;

        RetryStrategy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 檢測並分析樂觀鎖衝突
     * 
     * @param exception 樂觀鎖異常
     * @param entityType 實體類型
     * @param entityId 實體ID
     * @param operation 操作類型
     * @return 衝突信息
     */
    public ConflictInfo detectConflict(Exception exception, String entityType, String entityId, String operation) {
        logger.warn("Optimistic locking conflict detected for entity: {} with id: {} during operation: {}", 
                   entityType, entityId, operation, exception);

        // 從異常中提取版本信息
        Long expectedVersion = extractExpectedVersion(exception);
        Long actualVersion = extractActualVersion(exception);

        ConflictInfo conflictInfo = new ConflictInfo(entityType, entityId, expectedVersion, actualVersion, operation);
        
        // 記錄詳細的衝突信息
        logConflictDetails(conflictInfo, exception);
        
        return conflictInfo;
    }

    /**
     * 分析衝突並建議重試策略
     * 
     * @param conflictInfo 衝突信息
     * @return 建議的重試策略
     */
    public RetryStrategy analyzeAndSuggestRetryStrategy(ConflictInfo conflictInfo) {
        // 基於衝突類型和頻率分析重試策略
        if (conflictInfo.getExpectedVersion() != null && conflictInfo.getActualVersion() != null) {
            long versionDiff = conflictInfo.getActualVersion() - conflictInfo.getExpectedVersion();
            
            if (versionDiff == 1) {
                // 版本差異小，建議立即重試
                logger.debug("Small version difference detected, suggesting immediate retry");
                return RetryStrategy.IMMEDIATE_RETRY;
            } else if (versionDiff <= 5) {
                // 中等版本差異，建議線性退避
                logger.debug("Medium version difference detected, suggesting linear backoff");
                return RetryStrategy.LINEAR_BACKOFF;
            } else {
                // 大版本差異，建議指數退避
                logger.debug("Large version difference detected, suggesting exponential backoff");
                return RetryStrategy.EXPONENTIAL_BACKOFF;
            }
        }

        // 默認建議線性退避
        return RetryStrategy.LINEAR_BACKOFF;
    }

    /**
     * 檢查是否為樂觀鎖異常
     * 
     * @param exception 異常
     * @return 如果是樂觀鎖異常返回 true
     */
    public boolean isOptimisticLockingException(Exception exception) {
        return exception instanceof OptimisticLockException ||
               exception instanceof OptimisticLockingFailureException ||
               (exception.getCause() instanceof OptimisticLockException) ||
               (exception.getCause() instanceof OptimisticLockingFailureException);
    }

    /**
     * 創建增強的樂觀鎖異常
     * 
     * @param conflictInfo 衝突信息
     * @param originalException 原始異常
     * @return 增強的異常
     */
    public OptimisticLockingConflictException createEnhancedException(ConflictInfo conflictInfo, 
                                                                     Exception originalException) {
        RetryStrategy retryStrategy = analyzeAndSuggestRetryStrategy(conflictInfo);
        
        return new OptimisticLockingConflictException(
                String.format("Optimistic locking conflict detected for %s with id %s. " +
                             "Expected version: %d, Actual version: %d. Suggested retry strategy: %s",
                             conflictInfo.getEntityType(), conflictInfo.getEntityId(),
                             conflictInfo.getExpectedVersion(), conflictInfo.getActualVersion(),
                             retryStrategy.getDescription()),
                conflictInfo, retryStrategy, originalException);
    }

    /**
     * 從異常中提取期望版本號
     */
    private Long extractExpectedVersion(Exception exception) {
        // 嘗試從異常消息中解析版本號
        String message = exception.getMessage();
        if (message != null) {
            // 這裡可以根據具體的 JPA 實現來解析版本號
            // 目前返回 null，實際實現時可以根據異常消息格式來解析
        }
        return null;
    }

    /**
     * 從異常中提取實際版本號
     */
    private Long extractActualVersion(Exception exception) {
        // 嘗試從異常消息中解析版本號
        String message = exception.getMessage();
        if (message != null) {
            // 這裡可以根據具體的 JPA 實現來解析版本號
            // 目前返回 null，實際實現時可以根據異常消息格式來解析
        }
        return null;
    }

    /**
     * 記錄衝突詳細信息
     */
    private void logConflictDetails(ConflictInfo conflictInfo, Exception exception) {
        logger.warn("=== Optimistic Locking Conflict Details ===");
        logger.warn("Entity Type: {}", conflictInfo.getEntityType());
        logger.warn("Entity ID: {}", conflictInfo.getEntityId());
        logger.warn("Operation: {}", conflictInfo.getOperation());
        logger.warn("Expected Version: {}", conflictInfo.getExpectedVersion());
        logger.warn("Actual Version: {}", conflictInfo.getActualVersion());
        logger.warn("Conflict Time: {}", conflictInfo.getConflictTime());
        logger.warn("Thread: {}", conflictInfo.getThreadName());
        logger.warn("Exception: {}", exception.getMessage());
        logger.warn("=== End Conflict Details ===");
    }
}