package solid.humank.genaidemo.infrastructure.common.lock;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.lock.DistributedLockManager;

/**
 * 記憶體分散式鎖管理器實現
 *
 * 用於本機開發和測試環境，提供與 Redis 相同的鎖語義，
 * 但使用 JVM 記憶體實現，無需外部依賴。
 *
 * 特性：
 * - 執行緒安全
 * - 支援鎖過期
 * - 支援重入鎖
 * - 自動清理過期鎖
 */
@Component
@Profile({ "local", "test", "docker" })
public class InMemoryDistributedLockManager implements DistributedLockManager {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryDistributedLockManager.class);

    /**
     * 鎖資訊內部類別
     */
    private static class LockInfo {
        private final Thread ownerThread;
        private final Instant expirationTime;
        private int reentrantCount;

        public LockInfo(Thread ownerThread, Instant expirationTime) {
            this.ownerThread = ownerThread;
            this.expirationTime = expirationTime;
            this.reentrantCount = 1;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expirationTime);
        }

        public boolean isOwnedByCurrentThread() {
            return Thread.currentThread().equals(ownerThread);
        }

        public long getRemainingTimeMillis() {
            long remaining = expirationTime.toEpochMilli() - Instant.now().toEpochMilli();
            return Math.max(0, remaining);
        }
    }

    private final ConcurrentHashMap<String, LockInfo> locks = new ConcurrentHashMap<>();
    private final ReentrantLock globalLock = new ReentrantLock();

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        if (lockKey == null || lockKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Lock key cannot be null or empty");
        }

        long waitTimeMillis = timeUnit.toMillis(waitTime);
        long leaseTimeMillis = timeUnit.toMillis(leaseTime);
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < waitTimeMillis) {
            if (tryAcquireLock(lockKey, leaseTimeMillis)) {
                logger.debug("Successfully acquired lock: {}", lockKey);
                return true;
            }

            // 短暫等待後重試
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Thread interrupted while waiting for lock: {}", lockKey);
                return false;
            }
        }

        logger.debug("Failed to acquire lock within wait time: {}", lockKey);
        return false;
    }

    private boolean tryAcquireLock(String lockKey, long leaseTimeMillis) {
        globalLock.lock();
        try {
            // 清理過期鎖
            cleanupExpiredLock(lockKey);

            LockInfo existingLock = locks.get(lockKey);

            // 檢查是否為重入鎖
            if (existingLock != null && existingLock.isOwnedByCurrentThread()) {
                existingLock.reentrantCount++;
                logger.debug("Reentrant lock acquired: {} (count: {})", lockKey, existingLock.reentrantCount);
                return true;
            }

            // 檢查是否有其他執行緒持有鎖
            if (existingLock != null && !existingLock.isExpired()) {
                return false;
            }

            // 獲取新鎖
            Instant expirationTime = Instant.now().plusMillis(leaseTimeMillis);
            LockInfo newLock = new LockInfo(Thread.currentThread(), expirationTime);
            locks.put(lockKey, newLock);

            return true;
        } finally {
            globalLock.unlock();
        }
    }

    @Override
    public void unlock(String lockKey) {
        if (lockKey == null || lockKey.trim().isEmpty()) {
            return;
        }

        globalLock.lock();
        try {
            LockInfo lockInfo = locks.get(lockKey);

            if (lockInfo == null) {
                logger.warn("Attempting to unlock non-existent lock: {}", lockKey);
                return;
            }

            if (!lockInfo.isOwnedByCurrentThread()) {
                logger.warn("Attempting to unlock lock owned by different thread: {}", lockKey);
                return;
            }

            // 處理重入鎖
            lockInfo.reentrantCount--;
            if (lockInfo.reentrantCount > 0) {
                logger.debug("Reentrant lock released: {} (remaining count: {})", lockKey, lockInfo.reentrantCount);
                return;
            }

            // 完全釋放鎖
            locks.remove(lockKey);
            logger.debug("Lock released: {}", lockKey);

        } finally {
            globalLock.unlock();
        }
    }

    @Override
    public boolean isLocked(String lockKey) {
        if (lockKey == null || lockKey.trim().isEmpty()) {
            return false;
        }

        LockInfo lockInfo = locks.get(lockKey);
        return lockInfo != null && !lockInfo.isExpired();
    }

    @Override
    public long getRemainingTime(String lockKey) {
        if (lockKey == null || lockKey.trim().isEmpty()) {
            return -1;
        }

        LockInfo lockInfo = locks.get(lockKey);
        if (lockInfo == null || lockInfo.isExpired()) {
            return -1;
        }

        return lockInfo.getRemainingTimeMillis();
    }

    @Override
    public void forceUnlock(String lockKey) {
        if (lockKey == null || lockKey.trim().isEmpty()) {
            return;
        }

        globalLock.lock();
        try {
            LockInfo removed = locks.remove(lockKey);
            if (removed != null) {
                logger.info("Force unlocked: {}", lockKey);
            }
        } finally {
            globalLock.unlock();
        }
    }

    @Override
    public void cleanupExpiredLocks() {
        globalLock.lock();
        try {
            int initialSize = locks.size();
            locks.entrySet().removeIf(entry -> {
                boolean expired = entry.getValue().isExpired();
                if (expired) {
                    logger.debug("Cleaned up expired lock: {}", entry.getKey());
                }
                return expired;
            });

            int cleanedCount = initialSize - locks.size();
            if (cleanedCount > 0) {
                logger.info("Cleaned up {} expired locks", cleanedCount);
            }
        } finally {
            globalLock.unlock();
        }
    }

    private void cleanupExpiredLock(String lockKey) {
        LockInfo lockInfo = locks.get(lockKey);
        if (lockInfo != null && lockInfo.isExpired()) {
            locks.remove(lockKey);
            logger.debug("Cleaned up expired lock: {}", lockKey);
        }
    }

    @Override
    public int getActiveLockCount() {
        // 清理過期鎖後返回計數
        cleanupExpiredLocks();
        return locks.size();
    }

    /**
     * 清理所有鎖 (測試用途)
     */
    public void clearAllLocks() {
        globalLock.lock();
        try {
            int count = locks.size();
            locks.clear();
            logger.info("Cleared all {} locks", count);
        } finally {
            globalLock.unlock();
        }
    }

    /**
     * 獲取鎖的詳細資訊 (除錯用途)
     */
    @Override
    public String getLockInfo(String lockKey) {
        LockInfo lockInfo = locks.get(lockKey);
        if (lockInfo == null) {
            return "Lock not found: " + lockKey;
        }

        return String.format("Lock[key=%s, owner=%s, remaining=%dms, reentrant=%d, expired=%s]",
                lockKey,
                lockInfo.ownerThread.getName(),
                lockInfo.getRemainingTimeMillis(),
                lockInfo.reentrantCount,
                lockInfo.isExpired());
    }
}
