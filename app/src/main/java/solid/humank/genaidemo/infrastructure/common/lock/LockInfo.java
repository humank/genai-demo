package solid.humank.genaidemo.infrastructure.common.lock;

import java.time.Instant;

/**
 * 分散式鎖資訊類別
 * 
 * 包含鎖的詳細資訊，包括：
 * - 鎖的鍵值
 * - 是否被鎖定
 * - 剩餘時間
 * - 擁有者資訊
 * - 過期時間
 * 
 * 建立日期: 2025年9月24日 上午10:54 (台北時間)
 * 需求: 1.1 - 並發控制機制全面重構
 * 
 * @author Kiro AI Assistant
 * @since 1.0
 */
public class LockInfo {
    
    private final String lockKey;
    private final boolean locked;
    private final long remainingTimeMs;
    private final String ownerInfo;
    private final Instant expirationTime;
    
    public LockInfo(String lockKey, boolean locked, long remainingTimeMs, String ownerInfo, Instant expirationTime) {
        this.lockKey = lockKey;
        this.locked = locked;
        this.remainingTimeMs = remainingTimeMs;
        this.ownerInfo = ownerInfo;
        this.expirationTime = expirationTime;
    }
    
    public String getLockKey() {
        return lockKey;
    }
    
    public boolean isLocked() {
        return locked;
    }
    
    public long getRemainingTimeMs() {
        return remainingTimeMs;
    }
    
    public String getOwnerInfo() {
        return ownerInfo;
    }
    
    public Instant getExpirationTime() {
        return expirationTime;
    }
    
    @Override
    public String toString() {
        return String.format("LockInfo [key=%s, locked=%s, remaining=%dms, owner=%s, expires=%s]",
            lockKey, locked, remainingTimeMs, ownerInfo, expirationTime);
    }
}