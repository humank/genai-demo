package solid.humank.genaidemo.infrastructure.inventory.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import solid.humank.genaidemo.infrastructure.common.persistence.BaseOptimisticLockingEntity;

/**
 * 庫存預留 JPA 實體 - 支援 Aurora 樂觀鎖機制
 * 
 * 更新日期: 2025年9月24日 下午2:34 (台北時間)
 * 更新內容: 繼承 BaseOptimisticLockingEntity 以支援 Aurora 樂觀鎖機制
 * 需求: 1.1 - 並發控制機制全面重構
 */
@Entity
@Table(name = "inventory_reservations")
public class JpaReservationEntity extends BaseOptimisticLockingEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private JpaInventoryEntity inventory;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ReservationStatusEnum status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // createdAt 已在 BaseOptimisticLockingEntity 中定義

    // 默認構造函數
    public JpaReservationEntity() {}

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public JpaInventoryEntity getInventory() {
        return inventory;
    }

    public void setInventory(JpaInventoryEntity inventory) {
        this.inventory = inventory;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public ReservationStatusEnum getStatus() {
        return status;
    }

    public void setStatus(ReservationStatusEnum status) {
        this.status = status;
    }

    // getCreatedAt, setCreatedAt 已在 BaseOptimisticLockingEntity 中定義

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /** 預留狀態枚舉 */
    public enum ReservationStatusEnum {
        ACTIVE,
        CONFIRMED,
        RELEASED,
        EXPIRED
    }
}
