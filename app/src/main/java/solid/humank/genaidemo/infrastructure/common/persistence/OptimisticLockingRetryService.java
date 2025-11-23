package solid.humank.genaidemo.infrastructure.common.persistence;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import jakarta.persistence.OptimisticLockException;
import solid.humank.genaidemo.infrastructure.common.persistence.OptimisticLockingConflictDetector.ConflictInfo;
import solid.humank.genaidemo.infrastructure.common.persistence.OptimisticLockingConflictDetector.RetryStrategy;

/**
 * 樂觀鎖重試服務
 *
 * 提供樂觀鎖衝突的自動重試機制，包含：
 * 1. 智能重試策略
 * 2. 退避算法實現
 * 3. 重試次數控制
 * 4. 異常處理和監控
 *
 * 建立日期: 2025年9月24日 上午10:18 (台北時間)
 * 需求: 1.1 - 並發控制機制全面重構
 *
 * @author Kiro AI Assistant
 * @since 1.0
 */
@Service
public class OptimisticLockingRetryService {
    private static final Logger logger = LoggerFactory.getLogger(OptimisticLockingRetryService.class);

    private final OptimisticLockingConflictDetector conflictDetector;

    public OptimisticLockingRetryService(OptimisticLockingConflictDetector conflictDetector) {
        this.conflictDetector = conflictDetector;
    }

    /**
     * 執行帶重試的操作
     *
     * @param operation     要執行的操作
     * @param entityType    實體類型
     * @param entityId      實體ID
     * @param operationName 操作名稱
     * @param <T>           返回類型
     * @return 操作結果
     * @throws OptimisticLockingConflictException 重試失敗後拋出
     */
    public <T> T executeWithRetry(Supplier<T> operation, String entityType, String entityId, String operationName) {
        return executeWithRetry(operation, entityType, entityId, operationName, null);
    }

    /**
     * 執行帶重試的操作（自定義最大重試次數）
     *
     * @param operation     要執行的操作
     * @param entityType    實體類型
     * @param entityId      實體ID
     * @param operationName 操作名稱
     * @param maxRetries    最大重試次數，null 表示使用默認值
     * @param <T>           返回類型
     * @return 操作結果
     * @throws OptimisticLockingConflictException 重試失敗後拋出
     */
    public <T> T executeWithRetry(Supplier<T> operation, String entityType, String entityId,
            String operationName, Integer maxRetries) {
        int attemptNumber = 1;
        Exception lastException = null;
        ConflictInfo lastConflictInfo = null;
        RetryStrategy retryStrategy = RetryStrategy.LINEAR_BACKOFF;

        while (true) {
            try {
                logger.debug("Executing operation {} for entity {} (id: {}), attempt: {}",
                        operationName, entityType, entityId, attemptNumber);

                T result = operation.get();

                if (attemptNumber > 1) {
                    logger.info("Operation {} for entity {} (id: {}) succeeded after {} attempts",
                            operationName, entityType, entityId, attemptNumber);
                }

                return result;

            } catch (OptimisticLockException | OptimisticLockingFailureException e) {
                lastException = e;

                // 檢測衝突並分析重試策略
                ConflictInfo conflictInfo = conflictDetector.detectConflict(e, entityType, entityId, operationName);
                retryStrategy = conflictDetector.analyzeAndSuggestRetryStrategy(conflictInfo);
                lastConflictInfo = conflictInfo;

                // 確定最大重試次數
                int effectiveMaxRetries = maxRetries != null ? maxRetries : getDefaultMaxRetries(retryStrategy);

                if (attemptNumber >= effectiveMaxRetries || retryStrategy == RetryStrategy.NO_RETRY) {
                    logger.error("Operation {} for entity {} (id: {}) failed after {} attempts, giving up",
                            operationName, entityType, entityId, attemptNumber);
                    break;
                }

                // 計算延遲時間並等待
                long delayMs = calculateRetryDelay(retryStrategy, attemptNumber);
                if (delayMs > 0) {
                    logger.debug("Retrying operation {} for entity {} (id: {}) in {}ms (attempt {})",
                            operationName, entityType, entityId, delayMs, attemptNumber + 1);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.warn("Retry sleep interrupted for operation {} on entity {} (id: {})",
                                operationName, entityType, entityId);
                        break;
                    }
                }

                attemptNumber++;

            } catch (Exception e) {
                // 非樂觀鎖異常，直接拋出
                logger.error("Non-optimistic locking exception in operation {} for entity {} (id: {})",
                        operationName, entityType, entityId, e);
                throw new RuntimeException("Operation failed", e);
            }
        }

        // 重試失敗，拋出增強異常
        OptimisticLockingConflictException enhancedException = conflictDetector.createEnhancedException(
                lastConflictInfo, lastException);

        logger.error("All retry attempts failed for operation {} on entity {} (id: {}): {}",
                operationName, entityType, entityId, enhancedException.getMessage());
        throw enhancedException;
    }

    /**
     * 執行帶重試的無返回值操作
     *
     * @param operation     要執行的操作
     * @param entityType    實體類型
     * @param entityId      實體ID
     * @param operationName 操作名稱
     * @throws OptimisticLockingConflictException 重試失敗後拋出
     */
    public void executeWithRetry(Runnable operation, String entityType, String entityId, String operationName) {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, entityType, entityId, operationName);
    }

    /**
     * 執行帶重試的無返回值操作（自定義最大重試次數）
     *
     * @param operation     要執行的操作
     * @param entityType    實體類型
     * @param entityId      實體ID
     * @param operationName 操作名稱
     * @param maxRetries    最大重試次數
     * @throws OptimisticLockingConflictException 重試失敗後拋出
     */
    public void executeWithRetry(Runnable operation, String entityType, String entityId,
            String operationName, Integer maxRetries) {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, entityType, entityId, operationName, maxRetries);
    }

    /**
     * 獲取默認最大重試次數
     *
     * @param retryStrategy 重試策略
     * @return 最大重試次數
     */
    private int getDefaultMaxRetries(RetryStrategy retryStrategy) {
        return switch (retryStrategy) {
            case IMMEDIATE_RETRY -> 3;
            case LINEAR_BACKOFF -> 5;
            case EXPONENTIAL_BACKOFF -> 4;
            case NO_RETRY -> 0;
        };
    }

    /**
     * 計算重試延遲時間
     *
     * @param retryStrategy 重試策略
     * @param attemptNumber 當前重試次數
     * @return 延遲時間（毫秒）
     */
    private long calculateRetryDelay(RetryStrategy retryStrategy, int attemptNumber) {
        return switch (retryStrategy) {
            case IMMEDIATE_RETRY -> 0;
            case LINEAR_BACKOFF -> attemptNumber * 100L; // 100ms, 200ms, 300ms...
            case EXPONENTIAL_BACKOFF -> (long) Math.pow(2, attemptNumber - 1) * 100L; // 100ms, 200ms, 400ms, 800ms...
            case NO_RETRY -> 0;
        };
    }
}
