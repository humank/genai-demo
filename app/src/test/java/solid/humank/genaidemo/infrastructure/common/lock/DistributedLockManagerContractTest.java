package solid.humank.genaidemo.infrastructure.common.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import solid.humank.genaidemo.domain.common.lock.DistributedLockManager;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 分散式鎖管理器契約測試
 * 
 * 這個測試類別驗證 DistributedLockManager 介面的契約，
 * 確保所有實現（記憶體、Redis）都遵循相同的行為規範。
 * 
 * 在 local/test 環境中，這些測試使用 InMemoryDistributedLockManager。
 * 在 staging/production 環境中，相同的契約應該由 Redis 實現滿足。
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Distributed Lock Manager Contract Tests")
class DistributedLockManagerContractTest {
    
    private DistributedLockManager lockManager;
    
    @BeforeEach
    void setUp() {
        // 在測試環境中，這會自動注入 InMemoryDistributedLockManager
        lockManager = new InMemoryDistributedLockManager();
        
        // 確保測試開始前清理所有鎖
        lockManager.cleanupExpiredLocks();
    }
    
    @Test
    @DisplayName("Contract: Should acquire and release lock successfully")
    void contract_should_acquire_and_release_lock_successfully() {
        String lockKey = "contract-test-basic-lock";
        
        // 契約：tryLock 成功時返回 true
        boolean acquired = lockManager.tryLock(lockKey, 1, 10, TimeUnit.SECONDS);
        assertThat(acquired).isTrue();
        
        // 契約：isLocked 應該返回 true
        assertThat(lockManager.isLocked(lockKey)).isTrue();
        
        // 契約：unlock 後 isLocked 應該返回 false
        lockManager.unlock(lockKey);
        assertThat(lockManager.isLocked(lockKey)).isFalse();
    }
    
    @Test
    @DisplayName("Contract: Should provide lock information for debugging")
    void contract_should_provide_lock_information_for_debugging() {
        String lockKey = "contract-test-debug-info";
        
        // 契約：獲取鎖資訊應該包含關鍵資訊
        boolean acquired = lockManager.tryLock(lockKey, 1, 10, TimeUnit.SECONDS);
        assertThat(acquired).isTrue();
        
        String lockInfo = lockManager.getLockInfo(lockKey);
        assertThat(lockInfo).contains(lockKey);
        
        lockManager.unlock(lockKey);
        
        // 契約：不存在的鎖應該返回適當的資訊
        String nonExistentInfo = lockManager.getLockInfo("non-existent-lock");
        assertThat(nonExistentInfo).isNotNull();
        assertThat(nonExistentInfo).containsAnyOf("not found", "Not found", "Lock not found");
    }
}