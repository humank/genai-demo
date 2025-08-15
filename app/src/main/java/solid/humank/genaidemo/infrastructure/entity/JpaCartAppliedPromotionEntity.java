package solid.humank.genaidemo.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 購物車應用促銷 JPA 實體 */
@Entity
@Table(name = "cart_applied_promotions")
public class JpaCartAppliedPromotionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "cart_id", nullable = false)
    private String cartId;

    @Column(name = "promotion_id", nullable = false)
    private String promotionId;

    @Column(name = "promotion_name", nullable = false)
    private String promotionName;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    // 默認建構子
    public JpaCartAppliedPromotionEntity() {}

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    public String getPromotionId() {
        return promotionId;
    }

    public void setPromotionId(String promotionId) {
        this.promotionId = promotionId;
    }

    public String getPromotionName() {
        return promotionName;
    }

    public void setPromotionName(String promotionName) {
        this.promotionName = promotionName;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    @PrePersist
    protected void onCreate() {
        appliedAt = LocalDateTime.now();
    }
}
