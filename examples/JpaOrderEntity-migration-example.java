package solid.humank.genaidemo.infrastructure.order.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import solid.humank.genaidemo.domain.common.valueobject.OrderStatus;
import solid.humank.genaidemo.infrastructure.common.persistence.BaseOptimisticLockingEntity;

/**
 * 訂單 JPA 實體 - 遷移到樂觀鎖版本
 * 
 * 更新日期: 2025年9月24日 下午2:34 (台北時間)
 * 更新內容: 繼承 BaseOptimisticLockingEntity 以支援 Aurora 樂觀鎖機制
 * 需求: 1.1 - 並發控制機制全面重構
 * 
 * 主要變更:
 * 1. 繼承 BaseOptimisticLockingEntity
 * 2. 移除重複的 createdAt 和 updatedAt 欄位
 * 3. 移除相關的 getter/setter 方法
 * 4. 自動獲得樂觀鎖版本控制
 */
@Entity
@Table(name = "orders")
public class JpaOrderEntity extends BaseOptimisticLockingEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "shipping_address", nullable = false)
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "effective_amount", nullable = false)
    private BigDecimal effectiveAmount;

    // ❌ 移除：這些欄位已在 BaseOptimisticLockingEntity 中定義
    // @Column(name = "created_at", nullable = false)
    // private LocalDateTime createdAt;
    
    // @Column(name = "updated_at", nullable = false)
    // private LocalDateTime updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<JpaOrderItemEntity> items = new ArrayList<>();

    // 默認建構子，JPA 需要
    public JpaOrderEntity() {}

    // 業務建構子 - 不再需要時間戳記參數
    public JpaOrderEntity(String id, String customerId, String shippingAddress, 
                         OrderStatus status, BigDecimal totalAmount, String currency, 
                         BigDecimal effectiveAmount) {
        this.id = id;
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
        this.status = status;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.effectiveAmount = effectiveAmount;
        // 時間戳記會在 @PrePersist 時自動設置
    }

    // Getters and Setters - 移除時間戳記相關方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getEffectiveAmount() {
        return effectiveAmount;
    }

    public void setEffectiveAmount(BigDecimal effectiveAmount) {
        this.effectiveAmount = effectiveAmount;
    }

    // ❌ 移除：這些方法已在 BaseOptimisticLockingEntity 中定義
    // public LocalDateTime getCreatedAt() {
    //     return createdAt;
    // }
    
    // public void setCreatedAt(LocalDateTime createdAt) {
    //     this.createdAt = createdAt;
    // }
    
    // public LocalDateTime getUpdatedAt() {
    //     return updatedAt;
    // }
    
    // public void setUpdatedAt(LocalDateTime updatedAt) {
    //     this.updatedAt = updatedAt;
    // }

    public List<JpaOrderItemEntity> getItems() {
        return items;
    }

    public void setItems(List<JpaOrderItemEntity> items) {
        this.items = items;
    }

    public void addItem(JpaOrderItemEntity item) {
        items.add(item);
    }

    // 新增：業務方法範例，展示如何使用樂觀鎖
    public void updateOrderStatus(OrderStatus newStatus) {
        if (this.status == OrderStatus.CANCELLED || this.status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot update status of cancelled or completed order");
        }
        this.status = newStatus;
        // updatedAt 會在 @PreUpdate 時自動更新
    }

    public void addOrderItem(String productId, int quantity, BigDecimal unitPrice) {
        JpaOrderItemEntity item = new JpaOrderItemEntity();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setUnitPrice(unitPrice);
        item.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        
        addItem(item);
        
        // 重新計算總金額
        recalculateTotalAmount();
    }

    private void recalculateTotalAmount() {
        BigDecimal total = items.stream()
            .map(JpaOrderItemEntity::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        this.totalAmount = total;
        this.effectiveAmount = total; // 簡化版本，實際可能需要考慮折扣等
    }

    @Override
    public String toString() {
        return String.format("JpaOrderEntity{id='%s', customerId='%s', status=%s, totalAmount=%s, version=%d, createdAt=%s, updatedAt=%s}",
                id, customerId, status, totalAmount, getVersion(), getCreatedAt(), getUpdatedAt());
    }
}