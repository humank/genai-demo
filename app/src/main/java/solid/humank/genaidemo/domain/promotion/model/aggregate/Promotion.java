package solid.humank.genaidemo.domain.promotion.model.aggregate;

import java.time.LocalDateTime;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.promotion.model.events.PromotionActivatedEvent;
import solid.humank.genaidemo.domain.promotion.model.events.PromotionCreatedEvent;
import solid.humank.genaidemo.domain.promotion.model.events.PromotionInfoUpdatedEvent;
import solid.humank.genaidemo.domain.promotion.model.events.PromotionStatusChangedEvent;
import solid.humank.genaidemo.domain.promotion.model.events.PromotionUsedEvent;
import solid.humank.genaidemo.domain.promotion.model.valueobject.AddOnPurchaseRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.CartSummary;
import solid.humank.genaidemo.domain.promotion.model.valueobject.ConvenienceStoreVoucherRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.DateRange;
import solid.humank.genaidemo.domain.promotion.model.valueobject.FlashSaleRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.GiftWithPurchaseRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.LimitedQuantityRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionStatus;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionType;

/** 促銷聚合根 */
@AggregateRoot(name = "Promotion", description = "促銷聚合根，管理各種促銷活動規則", boundedContext = "Promotion", version = "1.0")
public class Promotion extends solid.humank.genaidemo.domain.common.aggregate.AggregateRoot {

    private final PromotionId id;
    private String name;
    private String description;
    private PromotionType type;
    private PromotionRule rule;
    private DateRange validPeriod;
    private PromotionStatus status;
    private int usageLimit;
    private int usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Promotion(
            PromotionId id,
            String name,
            String description,
            PromotionType type,
            PromotionRule rule,
            DateRange validPeriod) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.rule = rule;
        this.validPeriod = validPeriod;
        this.status = PromotionStatus.ACTIVE;
        this.usageLimit = -1; // -1 表示無限制
        this.usageCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // 發布促銷活動創建事件
        collectEvent(PromotionCreatedEvent.create(id, name, description, type));
    }

    // Getters
    public PromotionId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public PromotionType getType() {
        return type;
    }

    public PromotionRule getRule() {
        return rule;
    }

    public DateRange getValidPeriod() {
        return validPeriod;
    }

    public PromotionStatus getStatus() {
        return status;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public int getUsageCount() {
        return usageCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // 業務方法

    /** 檢查促銷是否適用於購物車 */
    public boolean isApplicable(CartSummary cartSummary) {
        return status == PromotionStatus.ACTIVE
                && validPeriod.contains(LocalDateTime.now())
                && !isUsageLimitReached()
                && rule.matches(cartSummary);
    }

    /** 計算折扣金額 */
    public Money calculateDiscount(CartSummary cartSummary) {
        if (!isApplicable(cartSummary)) {
            return Money.twd(0);
        }
        return rule.calculateDiscount(cartSummary);
    }

    /** 使用促銷 */
    public void use(String customerId, String orderId) {
        if (!canUse()) {
            throw new IllegalStateException("促銷無法使用");
        }
        this.usageCount++;
        this.updatedAt = LocalDateTime.now();

        // 發布促銷使用事件
        collectEvent(PromotionUsedEvent.create(this.id, customerId, orderId, this.usageCount));
    }

    /** 使用促銷（向後兼容） */
    public void use() {
        use("unknown", "unknown");
    }

    /** 檢查是否可以使用 */
    public boolean canUse() {
        return status == PromotionStatus.ACTIVE
                && validPeriod.contains(LocalDateTime.now())
                && !isUsageLimitReached();
    }

    /** 檢查使用次數是否達到上限 */
    public boolean isUsageLimitReached() {
        return usageLimit > 0 && usageCount >= usageLimit;
    }

    /** 檢查是否已過期 */
    public boolean isExpired() {
        return !validPeriod.contains(LocalDateTime.now());
    }

    /** 更新促銷狀態 */
    public void updateStatus(PromotionStatus newStatus, String reason) {
        PromotionStatus oldStatus = this.status;
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();

        // 發布促銷狀態變更事件
        collectEvent(PromotionStatusChangedEvent.create(this.id, oldStatus, newStatus, reason));

        // 如果促銷被激活，發布促銷激活事件
        if (newStatus == PromotionStatus.ACTIVE && oldStatus != PromotionStatus.ACTIVE) {
            Money discountAmount = Money.twd(0); // 預設折扣金額，實際應根據規則計算
            collectEvent(PromotionActivatedEvent.create(
                    this.id,
                    this.name,
                    this.type,
                    discountAmount,
                    this.validPeriod.startDate(),
                    this.validPeriod.endDate()));
        }
    }

    /** 更新促銷狀態（向後兼容） */
    public void updateStatus(PromotionStatus newStatus) {
        updateStatus(newStatus, "Status updated");
    }

    /** 設定使用限制 */
    public void setUsageLimit(int limit) {
        this.usageLimit = limit;
        this.updatedAt = LocalDateTime.now();
    }

    /** 更新促銷資訊 */
    public void updateInfo(String newName, String newDescription) {
        String oldName = this.name;
        String oldDescription = this.description;

        this.name = newName;
        this.description = newDescription;
        this.updatedAt = LocalDateTime.now();

        // 發布促銷資訊更新事件
        collectEvent(PromotionInfoUpdatedEvent.create(
                this.id, oldName, newName, oldDescription, newDescription));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Promotion promotion = (Promotion) obj;
        return id.equals(promotion.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    // 新增的規則獲取方法
    /** 獲取加價購規則 */
    public Optional<AddOnPurchaseRule> getAddOnPurchaseRule() {
        if (type == PromotionType.ADD_ON_PURCHASE && rule instanceof AddOnPurchaseRule) {
            return Optional.of((AddOnPurchaseRule) rule);
        }
        return Optional.empty();
    }

    /** 獲取閃購規則 */
    public Optional<FlashSaleRule> getFlashSaleRule() {
        if (type == PromotionType.FLASH_SALE && rule instanceof FlashSaleRule) {
            return Optional.of((FlashSaleRule) rule);
        }
        return Optional.empty();
    }

    /** 獲取限量規則 */
    public Optional<LimitedQuantityRule> getLimitedQuantityRule() {
        if (type == PromotionType.LIMITED_QUANTITY && rule instanceof LimitedQuantityRule) {
            return Optional.of((LimitedQuantityRule) rule);
        }
        return Optional.empty();
    }

    /** 獲取滿額贈禮規則 */
    public Optional<GiftWithPurchaseRule> getGiftWithPurchaseRule() {
        if (type == PromotionType.GIFT_WITH_PURCHASE && rule instanceof GiftWithPurchaseRule) {
            return Optional.of((GiftWithPurchaseRule) rule);
        }
        return Optional.empty();
    }

    /** 獲取超商優惠券規則 */
    public Optional<ConvenienceStoreVoucherRule> getConvenienceStoreVoucherRule() {
        // 簡化實現：假設所有促銷都可能有優惠券規則
        if (rule instanceof ConvenienceStoreVoucherRule) {
            return Optional.of((ConvenienceStoreVoucherRule) rule);
        }
        // 返回預設的優惠券規則
        return Optional.of(
                new ConvenienceStoreVoucherRule(
                        "預設優惠券",
                        Money.twd(100),
                        Money.twd(150),
                        java.time.Period.ofDays(30),
                        "全台門市",
                        "預設優惠券內容",
                        10));
    }
}
