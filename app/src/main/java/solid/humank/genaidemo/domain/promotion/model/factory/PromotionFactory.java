package solid.humank.genaidemo.domain.promotion.model.factory;

import java.time.LocalDateTime;
import solid.humank.genaidemo.domain.common.annotations.Factory;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.aggregate.Promotion;
import solid.humank.genaidemo.domain.promotion.model.valueobject.DateRange;
import solid.humank.genaidemo.domain.promotion.model.valueobject.FlashSaleRule;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionId;
import solid.humank.genaidemo.domain.promotion.model.valueobject.PromotionType;

/** 促銷工廠 用於創建各種類型的促銷 */
@Factory(name = "PromotionFactory", description = "促銷工廠，用於創建各種類型的促銷聚合根")
public class PromotionFactory {

    /** 創建閃購促銷 */
    public static Promotion createFlashSalePromotion(
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String productId,
            double specialPrice,
            int quantityLimit) {

        PromotionId promotionId = PromotionId.generate();
        DateRange validPeriod = new DateRange(startDate, endDate);

        FlashSaleRule rule =
                new FlashSaleRule(
                        new ProductId(productId),
                        Money.twd(specialPrice),
                        quantityLimit,
                        validPeriod);

        return new Promotion(
                promotionId, name, description, PromotionType.FLASH_SALE, rule, validPeriod);
    }

    /** 創建買一送一促銷 */
    public static Promotion createBuyOneGetOnePromotion(
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String productId) {

        PromotionId promotionId = PromotionId.generate();
        DateRange validPeriod = new DateRange(startDate, endDate);

        // 創建一個簡單的買一送一規則（使用FlashSaleRule作為基礎）
        FlashSaleRule rule =
                new FlashSaleRule(
                        new ProductId(productId),
                        Money.twd(0), // 第二件免費
                        1, // 每次限制一件
                        validPeriod);

        return new Promotion(
                promotionId, name, description, PromotionType.BUY_ONE_GET_ONE, rule, validPeriod);
    }

    /** 創建限量促銷 */
    public static Promotion createLimitedQuantityPromotion(
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String productId,
            double specialPrice,
            int quantityLimit) {

        PromotionId promotionId = PromotionId.generate();
        DateRange validPeriod = new DateRange(startDate, endDate);

        FlashSaleRule rule =
                new FlashSaleRule(
                        new ProductId(productId),
                        Money.twd(specialPrice),
                        quantityLimit,
                        validPeriod);

        return new Promotion(
                promotionId, name, description, PromotionType.LIMITED_QUANTITY, rule, validPeriod);
    }

    /** 創建滿額贈禮促銷 */
    public static Promotion createGiftWithPurchasePromotion(
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String giftProductId,
            double minimumAmount) {

        PromotionId promotionId = PromotionId.generate();
        DateRange validPeriod = new DateRange(startDate, endDate);

        // 使用FlashSaleRule作為基礎實現
        FlashSaleRule rule =
                new FlashSaleRule(
                        new ProductId(giftProductId),
                        Money.twd(0), // 贈品免費
                        1, // 每次限制一件贈品
                        validPeriod);

        return new Promotion(
                promotionId,
                name,
                description,
                PromotionType.GIFT_WITH_PURCHASE,
                rule,
                validPeriod);
    }

    /** 創建加價購促銷 */
    public static Promotion createAddOnPurchasePromotion(
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String addOnProductId,
            double specialPrice) {

        PromotionId promotionId = PromotionId.generate();
        DateRange validPeriod = new DateRange(startDate, endDate);

        FlashSaleRule rule =
                new FlashSaleRule(
                        new ProductId(addOnProductId),
                        Money.twd(specialPrice),
                        1, // 每次限制一件
                        validPeriod);

        return new Promotion(
                promotionId, name, description, PromotionType.ADD_ON_PURCHASE, rule, validPeriod);
    }
}
