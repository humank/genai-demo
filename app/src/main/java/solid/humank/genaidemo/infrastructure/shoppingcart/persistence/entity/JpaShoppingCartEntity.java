package solid.humank.genaidemo.infrastructure.shoppingcart.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** 購物車 JPA 實體 */
@Entity
@Table(name = "shopping_carts")
public class JpaShoppingCartEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal finalAmount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private CartStatusEnum status;

    @OneToMany(
            mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<JpaCartItemEntity> items = new HashSet<>();

    @OneToMany(
            mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    private Set<JpaCartAppliedPromotionEntity> appliedPromotions = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 默認建構子
    public JpaShoppingCartEntity() {}

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
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

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public CartStatusEnum getStatus() {
        return status;
    }

    public void setStatus(CartStatusEnum status) {
        this.status = status;
    }

    public Set<JpaCartItemEntity> getItems() {
        return items;
    }

    public void setItems(Set<JpaCartItemEntity> items) {
        this.items = items;
    }

    public Set<JpaCartAppliedPromotionEntity> getAppliedPromotions() {
        return appliedPromotions;
    }

    public void setAppliedPromotions(Set<JpaCartAppliedPromotionEntity> appliedPromotions) {
        this.appliedPromotions = appliedPromotions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // 添加購物車項目
    public void addItem(JpaCartItemEntity item) {
        items.add(item);
        item.setCart(this);
    }

    // 移除購物車項目
    public void removeItem(JpaCartItemEntity item) {
        items.remove(item);
        item.setCart(null);
    }

    // 添加應用的促銷
    public void addAppliedPromotion(JpaCartAppliedPromotionEntity promotion) {
        appliedPromotions.add(promotion);
        promotion.setCart(this);
    }

    // 移除應用的促銷
    public void removeAppliedPromotion(JpaCartAppliedPromotionEntity promotion) {
        appliedPromotions.remove(promotion);
        promotion.setCart(null);
    }

    /** 購物車狀態枚舉 */
    public enum CartStatusEnum {
        ACTIVE,
        CHECKED_OUT,
        ABANDONED,
        EXPIRED
    }
}
