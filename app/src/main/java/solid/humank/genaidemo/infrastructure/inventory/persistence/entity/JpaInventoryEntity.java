package solid.humank.genaidemo.infrastructure.inventory.persistence.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import solid.humank.genaidemo.infrastructure.common.persistence.BaseOptimisticLockingEntity;

/**
 * 庫存 JPA 實體 - 支援 Aurora 樂觀鎖機制
 * 
 * 更新日期: 2025年9月24日 下午2:34 (台北時間)
 * 更新內容: 繼承 BaseOptimisticLockingEntity 以支援 Aurora 樂觀鎖機制
 * 需求: 1.1 - 並發控制機制全面重構
 */
@Entity
@Table(name = "inventories")
public class JpaInventoryEntity extends BaseOptimisticLockingEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "threshold")
    private int threshold;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private InventoryStatusEnum status;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<JpaReservationEntity> reservations = new HashSet<>();

    // createdAt 和 updatedAt 已在 BaseOptimisticLockingEntity 中定義

    // 默認構造函數
    public JpaInventoryEntity() {}

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(int reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public InventoryStatusEnum getStatus() {
        return status;
    }

    public void setStatus(InventoryStatusEnum status) {
        this.status = status;
    }

    public Set<JpaReservationEntity> getReservations() {
        return reservations;
    }

    public void setReservations(Set<JpaReservationEntity> reservations) {
        this.reservations = reservations;
    }

    // getCreatedAt, setCreatedAt, getUpdatedAt, setUpdatedAt 已在 BaseOptimisticLockingEntity 中定義

    // 添加預留
    public void addReservation(JpaReservationEntity reservation) {
        reservations.add(reservation);
        reservation.setInventory(this);
    }

    // 移除預留
    public void removeReservation(JpaReservationEntity reservation) {
        reservations.remove(reservation);
        reservation.setInventory(null);
    }

    /** 庫存狀態枚舉 */
    public enum InventoryStatusEnum {
        ACTIVE,
        INACTIVE,
        DISCONTINUED
    }
}
