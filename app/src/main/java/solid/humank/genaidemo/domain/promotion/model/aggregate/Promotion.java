package solid.humank.genaidemo.domain.promotion.model.aggregate;

import java.time.LocalDateTime;
import java.util.Optional;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.promotion.model.specification.PromotionSpecification;
import solid.humank.genaidemo.domain.promotion.model.valueobject.*;

/** 促銷聚合根 */
@AggregateRoot
public class Promotion {
    private final PromotionId promotionId;
    private final String name;
    private final String description;
    private final PromotionType type;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private boolean isActive;

    // 不同類型促銷的規則
    private AddOnPurchaseRule addOnPurchaseRule;
    private FlashSaleRule flashSaleRule;
    private LimitedQuantityRule limitedQuantityRule;
    private GiftWithPurchaseRule giftWithPurchaseRule;
    private ConvenienceStoreVoucherRule convenienceStoreVoucherRule;

    // 促銷規格
    private PromotionSpecification specification;

    // 建構函數 - 加價購
    public Promotion(
            PromotionId promotionId,
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            AddOnPurchaseRule addOnPurchaseRule) {
        this.promotionId = promotionId;
        this.name = name;
        this.description = description;
        this.type = PromotionType.ADD_ON_PURCHASE;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = true;
        this.addOnPurchaseRule = addOnPurchaseRule;
    }

    // 建構函數 - 限時特價
    public Promotion(
            PromotionId promotionId,
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            FlashSaleRule flashSaleRule) {
        this.promotionId = promotionId;
        this.name = name;
        this.description = description;
        this.type = PromotionType.FLASH_SALE;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = true;
        this.flashSaleRule = flashSaleRule;
    }

    // 建構函數 - 限量特價
    public Promotion(
            PromotionId promotionId,
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            LimitedQuantityRule limitedQuantityRule) {
        this.promotionId = promotionId;
        this.name = name;
        this.description = description;
        this.type = PromotionType.LIMITED_QUANTITY_DEAL;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = true;
        this.limitedQuantityRule = limitedQuantityRule;
    }

    // 建構函數 - 滿額贈禮
    public Promotion(
            PromotionId promotionId,
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            GiftWithPurchaseRule giftWithPurchaseRule) {
        this.promotionId = promotionId;
        this.name = name;
        this.description = description;
        this.type = PromotionType.GIFT_WITH_PURCHASE;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = true;
        this.giftWithPurchaseRule = giftWithPurchaseRule;
    }

    // 建構函數 - 超商優惠券
    public Promotion(
            PromotionId promotionId,
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            ConvenienceStoreVoucherRule convenienceStoreVoucherRule) {
        this.promotionId = promotionId;
        this.name = name;
        this.description = description;
        this.type = PromotionType.CONVENIENCE_STORE_VOUCHER;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = true;
        this.convenienceStoreVoucherRule = convenienceStoreVoucherRule;
    }

    public PromotionId getPromotionId() {
        return promotionId;
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

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public Optional<AddOnPurchaseRule> getAddOnPurchaseRule() {
        return Optional.ofNullable(addOnPurchaseRule);
    }

    public Optional<FlashSaleRule> getFlashSaleRule() {
        return Optional.ofNullable(flashSaleRule);
    }

    public Optional<LimitedQuantityRule> getLimitedQuantityRule() {
        return Optional.ofNullable(limitedQuantityRule);
    }

    public Optional<GiftWithPurchaseRule> getGiftWithPurchaseRule() {
        return Optional.ofNullable(giftWithPurchaseRule);
    }

    public Optional<ConvenienceStoreVoucherRule> getConvenienceStoreVoucherRule() {
        return Optional.ofNullable(convenienceStoreVoucherRule);
    }

    public void setSpecification(PromotionSpecification specification) {
        this.specification = specification;
    }

    public Optional<PromotionSpecification> getSpecification() {
        return Optional.ofNullable(specification);
    }
}
