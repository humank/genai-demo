package solid.humank.genaidemo.infrastructure.common.persistence;

import solid.humank.genaidemo.infrastructure.common.persistence.OptimisticLockingConflictDetector.ConflictInfo;
import solid.humank.genaidemo.infrastructure.common.persistence.OptimisticLockingConflictDetector.RetryStrategy;

/**
 * 樂觀鎖衝突異常
 * 
 * 增強的樂觀鎖異常，提供詳細的衝突信息和重試建議
 * 
 * 建立日期: 2025年9月24日 上午10:18 (台北時間)
 * 需求: 1.1 - 並發控制機制全面重構
 * 
 * @author Kiro AI Assistant
 * @since 1.0
 */
public class OptimisticLockingConflictException extends RuntimeException {

    private final ConflictInfo conflictInfo;
    private final RetryStrategy suggestedRetryStrategy;

    /**
     * 建構子
     * 
     * @param message 異常消息
     * @param conflictInfo 衝突信息
     * @param suggestedRetryStrategy 建議的重試策略
     * @param cause 原始異常
     */
    public OptimisticLockingConflictException(String message, ConflictInfo conflictInfo, 
                                            RetryStrategy suggestedRetryStrategy, Throwable cause) {
        super(message, cause);
        this.conflictInfo = conflictInfo;
        this.suggestedRetryStrategy = suggestedRetryStrategy;
    }

    /**
     * 建構子
     * 
     * @param message 異常消息
     * @param conflictInfo 衝突信息
     * @param suggestedRetryStrategy 建議的重試策略
     */
    public OptimisticLockingConflictException(String message, ConflictInfo conflictInfo, 
                                            RetryStrategy suggestedRetryStrategy) {
        super(message);
        this.conflictInfo = conflictInfo;
        this.suggestedRetryStrategy = suggestedRetryStrategy;
    }

    /**
     * 獲取衝突信息
     * 
     * @return 衝突信息
     */
    public ConflictInfo getConflictInfo() {
        return conflictInfo;
    }

    /**
     * 獲取建議的重試策略
     * 
     * @return 重試策略
     */
    public RetryStrategy getSuggestedRetryStrategy() {
        return suggestedRetryStrategy;
    }

    /**
     * 獲取實體類型
     * 
     * @return 實體類型
     */
    public String getEntityType() {
        return conflictInfo != null ? conflictInfo.getEntityType() : null;
    }

    /**
     * 獲取實體ID
     * 
     * @return 實體ID
     */
    public String getEntityId() {
        return conflictInfo != null ? conflictInfo.getEntityId() : null;
    }

    /**
     * 獲取期望版本號
     * 
     * @return 期望版本號
     */
    public Long getExpectedVersion() {
        return conflictInfo != null ? conflictInfo.getExpectedVersion() : null;
    }

    /**
     * 獲取實際版本號
     * 
     * @return 實際版本號
     */
    public Long getActualVersion() {
        return conflictInfo != null ? conflictInfo.getActualVersion() : null;
    }

    /**
     * 檢查是否建議重試
     * 
     * @return 如果建議重試返回 true
     */
    public boolean shouldRetry() {
        return suggestedRetryStrategy != RetryStrategy.NO_RETRY;
    }

    /**
     * 獲取重試延遲時間（毫秒）
     * 
     * @param attemptNumber 重試次數（從1開始）
     * @return 延遲時間（毫秒）
     */
    public long getRetryDelayMs(int attemptNumber) {
        if (!shouldRetry()) {
            return 0;
        }

        return switch (suggestedRetryStrategy) {
            case IMMEDIATE_RETRY -> 0;
            case LINEAR_BACKOFF -> attemptNumber * 100L; // 100ms, 200ms, 300ms...
            case EXPONENTIAL_BACKOFF -> (long) Math.pow(2, attemptNumber - 1) * 100L; // 100ms, 200ms, 400ms, 800ms...
            case NO_RETRY -> 0;
        };
    }

    /**
     * 獲取最大重試次數建議
     * 
     * @return 最大重試次數
     */
    public int getMaxRetryAttempts() {
        return switch (suggestedRetryStrategy) {
            case IMMEDIATE_RETRY -> 3;
            case LINEAR_BACKOFF -> 5;
            case EXPONENTIAL_BACKOFF -> 4;
            case NO_RETRY -> 0;
        };
    }

    @Override
    public String toString() {
        return String.format("OptimisticLockingConflictException{conflictInfo=%s, suggestedRetryStrategy=%s, message='%s'}",
                conflictInfo, suggestedRetryStrategy, getMessage());
    }
}