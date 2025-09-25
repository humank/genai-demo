package solid.humank.genaidemo.infrastructure.common.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;

/**
 * Aurora 樂觀鎖基礎實體類
 * 
 * 提供樂觀鎖機制的基礎實現，包含：
 * 1. @Version 註解用於樂觀鎖控制
 * 2. 創建和更新時間戳記
 * 3. 自動時間戳記管理
 * 
 * 使用方式：
 * - 所有需要樂觀鎖的 JPA 實體都應該繼承此類
 * - 系統會自動處理版本號和時間戳記
 * - 並發更新時會拋出 OptimisticLockException
 * 
 * 建立日期: 2025年9月24日 上午10:18 (台北時間)
 * 需求: 1.1 - 並發控制機制全面重構
 * 
 * @author Kiro AI Assistant
 * @since 1.0
 */
@MappedSuperclass
public abstract class BaseOptimisticLockingEntity {

    /**
     * 樂觀鎖版本號
     * JPA 會自動管理此欄位，每次更新時自動遞增
     * 當並發更新發生時，會拋出 OptimisticLockException
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * 創建時間
     * 記錄實體首次持久化的時間
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 最後更新時間
     * 記錄實體最後一次更新的時間
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 默認建構子
     * JPA 需要無參數建構子
     */
    protected BaseOptimisticLockingEntity() {
    }

    /**
     * 獲取樂觀鎖版本號
     * 
     * @return 當前版本號，如果是新實體則為 null
     */
    public Long getVersion() {
        return version;
    }

    /**
     * 設定樂觀鎖版本號
     * 注意：通常不需要手動設定，JPA 會自動管理
     * 
     * @param version 版本號
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * 獲取創建時間
     * 
     * @return 實體創建時間
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 設定創建時間
     * 注意：通常不需要手動設定，會在 @PrePersist 時自動設定
     * 
     * @param createdAt 創建時間
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * 獲取最後更新時間
     * 
     * @return 實體最後更新時間
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 設定最後更新時間
     * 注意：通常不需要手動設定，會在 @PreUpdate 時自動設定
     * 
     * @param updatedAt 更新時間
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 檢查實體是否為新實體
     * 
     * @return 如果是新實體（未持久化）返回 true
     */
    public boolean isNew() {
        return version == null;
    }

    /**
     * 檢查實體是否已持久化
     * 
     * @return 如果已持久化返回 true
     */
    public boolean isPersisted() {
        return version != null;
    }

    /**
     * 持久化前的回調方法
     * 自動設定創建時間和更新時間
     */
    @PrePersist
    protected void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    /**
     * 更新前的回調方法
     * 自動更新最後更新時間
     */
    @PreUpdate
    protected void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 獲取實體的字符串表示
     * 包含版本號和時間戳記信息
     */
    @Override
    public String toString() {
        return String.format("%s{version=%d, createdAt=%s, updatedAt=%s}",
                getClass().getSimpleName(), version, createdAt, updatedAt);
    }
}