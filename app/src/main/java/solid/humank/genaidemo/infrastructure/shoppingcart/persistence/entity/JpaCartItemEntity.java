package solid.humank.genaidemo.infrastructure.shoppingcart.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import solid.humank.genaidemo.infrastructure.common.persistence.BaseOptimisticLockingEntity;

/**
 * 購物車項目 JPA 實體 - 支援 Aurora 樂觀鎖機制
 * 
 * 更新日期: 2025年9月24日 下午2:34 (台北時間)
 * 更新內容: 繼承 BaseOptimisticLockingEntity 以支援 Aurora 樂觀鎖機制
 * 需求: 1.1 - 並發控制機制全面重構
 */
@Entity
@Table(name = "cart_items")
public class JpaCartItemEntity extends BaseOptimisticLockingEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private JpaShoppingCartEntity cart;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    // updatedAt 已在 BaseOptimisticLockingEntity 中定義

    // 默認建構子
    public JpaCartItemEntity() {}

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public JpaShoppingCartEntity getCart() {
        return cart;
    }

    public void setCart(JpaShoppingCartEntity cart) {
        this.cart = cart;
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    // getUpdatedAt, setUpdatedAt 已在 BaseOptimisticLockingEntity 中定義
}
