package solid.humank.genaidemo.domain.shoppingcart.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.Entity;
import solid.humank.genaidemo.domain.shoppingcart.model.valueobject.CartPromotionId;

/**
 * 購物車促銷 Entity
 * 
 * 管理應用於購物車的促銷活動，包含折扣計算、條件檢查等
 */
@Entity(name = "CartPromotion", description = "購物車促銷實體，管理應用於購物車的促銷活動和折扣")
public class CartPromotion {

    public enum PromotionType {
        PERCENTAGE_DISCOUNT("百分比折扣"),
        FIXED_AMOUNT_DISCOUNT("固定金額折扣"),
        FREE_SHIPPING("免運費"),
        BUY_X_GET_Y("買X送Y"),
        SPEND_THRESHOLD("滿額優惠"),
        COUPON("優惠券"),
        MEMBER_DISCOUNT("會員折扣"),
        GIFT_WITH_PURCHASE("滿額贈品"),
        BUNDLE_DISCOUNT("組合優惠");

        private final String description;

        PromotionType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum PromotionStatus {
        ACTIVE("有效"),
        EXPIRED("已過期"),
        USED("已使用"),
        INVALID("無效"),
        PENDING("待生效");

        private final String description;

        PromotionStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final CartPromotionId id;
    private final String promotionId; // 促銷活動ID
    private final PromotionType type;
    private final String name;
    private final String description;
    private final BigDecimal discountValue; // 折扣值
    private final BigDecimal minSpendAmount; // 最小消費金額
    private final BigDecimal maxDiscountAmount; // 最大折扣金額
    private final String couponCode; // 優惠券代碼
    private PromotionStatus status;
    private BigDecimal appliedDiscountAmount; // 實際應用的折扣金額
    private LocalDateTime appliedAt;
    private LocalDateTime expiresAt;
    private String failureReason; // 失效原因
    private int priority; // 優先級（數字越小優先級越高）

    public CartPromotion(CartPromotionId id, String promotionId, PromotionType type,
            String name, String description, BigDecimal discountValue,
            BigDecimal minSpendAmount, BigDecimal maxDiscountAmount,
            String couponCode, LocalDateTime expiresAt, int priority) {
        this.id = Objects.requireNonNull(id, "CartPromotion ID cannot be null");
        this.promotionId = Objects.requireNonNull(promotionId, "Promotion ID cannot be null");
        this.type = Objects.requireNonNull(type, "Promotion type cannot be null");
        this.name = Objects.requireNonNull(name, "Promotion name cannot be null");
        this.description = description;
        this.discountValue = Objects.requireNonNull(discountValue, "Discount value cannot be null");
        this.minSpendAmount = minSpendAmount != null ? minSpendAmount : BigDecimal.ZERO;
        this.maxDiscountAmount = maxDiscountAmount;
        this.couponCode = couponCode;
        this.expiresAt = expiresAt;
        this.priority = priority;
        this.status = PromotionStatus.PENDING;
        this.appliedDiscountAmount = BigDecimal.ZERO;
        this.appliedAt = LocalDateTime.now();
        this.failureReason = null;

        validatePromotion();
    }

    // Getters
    public CartPromotionId getId() {
        return id;
    }

    public String getPromotionId() {
        return promotionId;
    }

    public PromotionType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public BigDecimal getMinSpendAmount() {
        return minSpendAmount;
    }

    public BigDecimal getMaxDiscountAmount() {
        return maxDiscountAmount;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public PromotionStatus getStatus() {
        return status;
    }

    public BigDecimal getAppliedDiscountAmount() {
        return appliedDiscountAmount;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public int getPriority() {
        return priority;
    }

    // 業務方法

    /** 計算折扣金額 */
    public BigDecimal calculateDiscountAmount(BigDecimal cartAmount) {
        if (!canApply(cartAmount)) {
            return BigDecimal.ZERO;
        }

        BigDecimal discountAmount;

        switch (type) {
            case PERCENTAGE_DISCOUNT:
                discountAmount = cartAmount.multiply(discountValue.divide(BigDecimal.valueOf(100)));
                // 應用最大折扣金額限制
                if (maxDiscountAmount != null && discountAmount.compareTo(maxDiscountAmount) > 0) {
                    discountAmount = maxDiscountAmount;
                }
                break;
            case FIXED_AMOUNT_DISCOUNT:
            case COUPON:
                discountAmount = discountValue;
                // 折扣金額不能超過購物車金額
                if (discountAmount.compareTo(cartAmount) > 0) {
                    discountAmount = cartAmount;
                }
                break;
            case FREE_SHIPPING:
                // 免運費的折扣金額需要從外部傳入運費金額
                discountAmount = BigDecimal.ZERO;
                break;
            default:
                discountAmount = BigDecimal.ZERO;
        }

        return discountAmount;
    }

    /** 應用促銷 */
    public void apply(BigDecimal cartAmount) {
        if (!canApply(cartAmount)) {
            this.status = PromotionStatus.INVALID;
            this.failureReason = "不符合促銷條件";
            return;
        }

        this.appliedDiscountAmount = calculateDiscountAmount(cartAmount);
        this.status = PromotionStatus.ACTIVE;
        this.appliedAt = LocalDateTime.now();
        this.failureReason = null;
    }

    /** 檢查是否可以應用 */
    public boolean canApply(BigDecimal cartAmount) {
        // 檢查是否過期
        if (isExpired()) {
            return false;
        }

        // 檢查最小消費金額
        if (cartAmount.compareTo(minSpendAmount) < 0) {
            return false;
        }

        // 檢查狀態
        return status == PromotionStatus.PENDING || status == PromotionStatus.ACTIVE;
    }

    /** 檢查是否已過期 */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /** 檢查是否有效 */
    public boolean isActive() {
        return status == PromotionStatus.ACTIVE && !isExpired();
    }

    /** 標記為已使用 */
    public void markAsUsed() {
        this.status = PromotionStatus.USED;
    }

    /** 標記為無效 */
    public void markAsInvalid(String reason) {
        this.status = PromotionStatus.INVALID;
        this.failureReason = reason;
    }

    /** 標記為過期 */
    public void markAsExpired() {
        if (isExpired()) {
            this.status = PromotionStatus.EXPIRED;
            this.failureReason = "促銷已過期";
        }
    }

    /** 重置促銷狀態 */
    public void reset() {
        this.status = PromotionStatus.PENDING;
        this.appliedDiscountAmount = BigDecimal.ZERO;
        this.failureReason = null;
    }

    /** 檢查是否為優惠券促銷 */
    public boolean isCouponPromotion() {
        return type == PromotionType.COUPON && couponCode != null;
    }

    /** 檢查是否為免運費促銷 */
    public boolean isFreeShippingPromotion() {
        return type == PromotionType.FREE_SHIPPING;
    }

    /** 檢查是否為百分比折扣 */
    public boolean isPercentageDiscount() {
        return type == PromotionType.PERCENTAGE_DISCOUNT;
    }

    /** 檢查是否為固定金額折扣 */
    public boolean isFixedAmountDiscount() {
        return type == PromotionType.FIXED_AMOUNT_DISCOUNT;
    }

    /** 檢查是否為會員折扣 */
    public boolean isMemberDiscount() {
        return type == PromotionType.MEMBER_DISCOUNT;
    }

    /** 獲取折扣百分比（僅適用於百分比折扣） */
    public BigDecimal getDiscountPercentage() {
        if (type == PromotionType.PERCENTAGE_DISCOUNT) {
            return discountValue;
        }
        return BigDecimal.ZERO;
    }

    /** 獲取折扣金額（僅適用於固定金額折扣） */
    public BigDecimal getDiscountAmount() {
        if (type == PromotionType.FIXED_AMOUNT_DISCOUNT || type == PromotionType.COUPON) {
            return discountValue;
        }
        return BigDecimal.ZERO;
    }

    /** 檢查促銷是否可以與其他促銷疊加 */
    public boolean canStackWith(CartPromotion other) {
        if (other == null)
            return true;

        // 優惠券通常不能疊加
        if (this.isCouponPromotion() || other.isCouponPromotion()) {
            return false;
        }

        // 免運費可以與其他折扣疊加
        if (this.isFreeShippingPromotion() || other.isFreeShippingPromotion()) {
            return true;
        }

        // 會員折扣可以與部分促銷疊加
        if (this.isMemberDiscount() && !other.isMemberDiscount()) {
            return true;
        }

        // 預設不能疊加
        return false;
    }

    /** 比較優先級 */
    public int comparePriority(CartPromotion other) {
        return Integer.compare(this.priority, other.priority);
    }

    /** 驗證促銷設定 */
    private void validatePromotion() {
        if (discountValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("折扣值不能為負數");
        }

        if (type == PromotionType.PERCENTAGE_DISCOUNT &&
                discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("百分比折扣不能超過100%");
        }

        if (minSpendAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("最小消費金額不能為負數");
        }

        if (maxDiscountAmount != null && maxDiscountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("最大折扣金額不能為負數");
        }

        if (type == PromotionType.COUPON && (couponCode == null || couponCode.isBlank())) {
            throw new IllegalArgumentException("優惠券促銷必須提供優惠券代碼");
        }
    }

    /** 創建百分比折扣促銷 */
    public static CartPromotion createPercentageDiscount(String promotionId, String name,
            BigDecimal percentage, BigDecimal minSpend,
            BigDecimal maxDiscount, LocalDateTime expiresAt,
            int priority) {
        return new CartPromotion(
                CartPromotionId.generate(),
                promotionId,
                PromotionType.PERCENTAGE_DISCOUNT,
                name,
                String.format("享受 %s%% 折扣", percentage),
                percentage,
                minSpend,
                maxDiscount,
                null,
                expiresAt,
                priority);
    }

    /** 創建固定金額折扣促銷 */
    public static CartPromotion createFixedAmountDiscount(String promotionId, String name,
            BigDecimal discountAmount, BigDecimal minSpend,
            LocalDateTime expiresAt, int priority) {
        return new CartPromotion(
                CartPromotionId.generate(),
                promotionId,
                PromotionType.FIXED_AMOUNT_DISCOUNT,
                name,
                String.format("折扣 $%s", discountAmount),
                discountAmount,
                minSpend,
                null,
                null,
                expiresAt,
                priority);
    }

    /** 創建優惠券促銷 */
    public static CartPromotion createCouponPromotion(String promotionId, String name,
            String couponCode, BigDecimal discountAmount,
            BigDecimal minSpend, LocalDateTime expiresAt,
            int priority) {
        return new CartPromotion(
                CartPromotionId.generate(),
                promotionId,
                PromotionType.COUPON,
                name,
                String.format("優惠券 %s 折扣 $%s", couponCode, discountAmount),
                discountAmount,
                minSpend,
                null,
                couponCode,
                expiresAt,
                priority);
    }

    /** 創建免運費促銷 */
    public static CartPromotion createFreeShippingPromotion(String promotionId, String name,
            BigDecimal minSpend, LocalDateTime expiresAt,
            int priority) {
        return new CartPromotion(
                CartPromotionId.generate(),
                promotionId,
                PromotionType.FREE_SHIPPING,
                name,
                "免運費",
                BigDecimal.ZERO,
                minSpend,
                null,
                null,
                expiresAt,
                priority);
    }

    /** 創建會員折扣促銷 */
    public static CartPromotion createMemberDiscount(String promotionId, String name,
            BigDecimal percentage, int priority) {
        return new CartPromotion(
                CartPromotionId.generate(),
                promotionId,
                PromotionType.MEMBER_DISCOUNT,
                name,
                String.format("會員專享 %s%% 折扣", percentage),
                percentage,
                BigDecimal.ZERO,
                null,
                null,
                null, // 會員折扣通常不過期
                priority);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        CartPromotion that = (CartPromotion) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("CartPromotion{id=%s, name='%s', type=%s, discount=%s, status=%s}",
                id, name, type, appliedDiscountAmount, status);
    }
}