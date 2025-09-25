package solid.humank.genaidemo.domain.common.lock;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 分散式鎖管理器抽象介面
 * 
 * 提供統一的分散式鎖操作介面，支援不同的實現方式：
 * - 記憶體實現 (本機開發和測試)
 * - Redis 實現 (Staging 和 Production)
 */
public interface DistributedLockManager {
    
    /**
     * 嘗試獲取鎖
     * 
     * @param lockKey 鎖的唯一標識
     * @param waitTime 等待時間
     * @param leaseTime 鎖的持有時間
     * @param timeUnit 時間單位
     * @return 是否成功獲取鎖
     */
    boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit);
    
    /**
     * 嘗試獲取鎖 (使用預設時間)
     * 
     * @param lockKey 鎖的唯一標識
     * @return 是否成功獲取鎖
     */
    default boolean tryLock(String lockKey) {
        return tryLock(lockKey, 3, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 嘗試獲取鎖 (使用 Duration)
     * 
     * @param lockKey 鎖的唯一標識
     * @param waitTime 等待時間
     * @param leaseTime 鎖的持有時間
     * @return 是否成功獲取鎖
     */
    default boolean tryLock(String lockKey, Duration waitTime, Duration leaseTime) {
        return tryLock(lockKey, waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * 釋放鎖
     * 
     * @param lockKey 鎖的唯一標識
     */
    void unlock(String lockKey);
    
    /**
     * 檢查鎖是否被持有
     * 
     * @param lockKey 鎖的唯一標識
     * @return 鎖是否被持有
     */
    boolean isLocked(String lockKey);
    
    /**
     * 獲取鎖的剩餘時間
     * 
     * @param lockKey 鎖的唯一標識
     * @return 剩餘時間 (毫秒)，如果鎖不存在返回 -1
     */
    long getRemainingTime(String lockKey);
    
    /**
     * 強制釋放鎖 (管理用途)
     * 
     * @param lockKey 鎖的唯一標識
     */
    void forceUnlock(String lockKey);
    
    /**
     * 清理所有過期的鎖
     */
    void cleanupExpiredLocks();
    
    /**
     * 獲取當前活躍的鎖數量
     * 
     * @return 活躍鎖數量
     */
    int getActiveLockCount();
    
    /**
     * 獲取鎖的詳細資訊
     * 
     * @param lockKey 鎖的唯一標識
     * @return 鎖的詳細資訊，如果鎖不存在返回 null
     */
    String getLockInfo(String lockKey);
}