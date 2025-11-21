package solid.humank.genaidemo.infrastructure.order.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import solid.humank.genaidemo.domain.common.valueobject.OrderStatus;
import solid.humank.genaidemo.infrastructure.common.persistence.BaseOptimisticLockingEntity;

/**
 * 訂單 JPA 實體 - 支援 Aurora 樂觀鎖機制
 * 
 * 更新日期: 2025年9月24日 下午2:34 (台北時間)
 * 更新內容: 繼承 BaseOptimisticLockingEntity 以支援 Aurora 樂觀鎖機制
 * 需求: 1.1 - 並發控制機制全面重構
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

    // createdAt 和 updatedAt 已在 BaseOptimisticLockingEntity 中定義

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<JpaOrderItemEntity> items = new ArrayList<>();

    // 默認建構子，JPA 需要
    public JpaOrderEntity() {}

    // Getters and Setters
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

    // getCreatedAt, setCreatedAt, getUpdatedAt, setUpdatedAt 已在 BaseOptimisticLockingEntity 中定義

    public List<JpaOrderItemEntity> getItems() {
        return items;
    }

    public void setItems(List<JpaOrderItemEntity> items) {
        this.items = items;
    }

    public void addItem(JpaOrderItemEntity item) {
        items.add(item);
    }
}
