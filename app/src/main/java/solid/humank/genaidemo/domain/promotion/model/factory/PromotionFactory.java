package solid.humank.genaidemo.domain.promotion.model.factory;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import solid.humank.genaidemo.domain.common.annotations.Factory;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;
import solid.humank.genaidemo.domain.promotion.model.aggregate.Promotion;
import solid.humank.genaidemo.domain.promotion.model.specification.*;
import solid.humank.genaidemo.domain.promotion.model.valueobject.*;

/** 促銷工廠 用於創建各種類型的促銷 */
@Factory(name = "PromotionFactory", description = "促銷工廠，用於創建各種類型的促銷聚合根")
public class PromotionFactory {

    /**
     * 創建加價購促銷
     *
     * @param name 促銷名稱
     * @param description 促銷描述
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @param mainProductId 主要商品ID
     * @param addOnProductId 加價購商品ID
     * @param specialPrice 特價
     * @param regularPrice 原價
     * @return 加價購促銷
     */
    public static Promotion createAddOnPurchasePromotion(
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String mainProductId,
            String addOnProductId,
            double specialPrice,
            double regularPrice) {

        PromotionId promotionId = new PromotionId();

        AddOnPurchaseRule rule =
                new AddOnPurchaseRule(
                        new ProductId(mainProductId),
                        new ProductId(addOnProductId),
                        Money.of(specialPrice),
                        Money.of(regularPrice));

        Promotion promotion =
                new Promotion(promotionId, name, description, startDate, endDate, rule);

        // 設置規格
        promotion.setSpecification(new AddOnPurchaseSpecification(rule));

        return promotion;
    }

    /**
     * 創建限時特價促銷
     *
     * @param name 促銷名稱
     * @param description 促銷描述
     * @param startDate 促銷開始日期
     * @param endDate 促銷結束日期
     * @param productId 商品ID
     * @param specialPrice 特價
     * @param regularPrice 原價
     * @param flashSaleStartTime 限時特價開始時間
     * @param flashSaleEndTime 限時特價結束時間
     * @param timeZone 時區
     * @return 限時特價促銷
     */
    public static Promotion createFlashSalePromotion(
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String productId,
            double specialPrice,
            double regularPrice,
            LocalDateTime flashSaleStartTime,
            LocalDateTime flashSaleEndTime,
            ZoneId timeZone) {

        PromotionId promotionId = new PromotionId();

        FlashSaleRule rule =
                new FlashSaleRule(
                        new ProductId(productId),
                        Money.of(specialPrice),
                        Money.of(regularPrice),
                        flashSaleStartTime,
                        flashSaleEndTime,
                        timeZone);

        Promotion promotion =
                new Promotion(promotionId, name, description, startDate, endDate, rule);

        // 設置規格
        promotion.setSpecification(new FlashSaleSpecification(rule));

        return promotion;
    }

    /**
     * 創建限量特價促銷
     *
     * @param name 促銷名稱
     * @param description 促銷描述
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @param productId 商品ID
     * @param specialPrice 特價
     * @param regularPrice 原價
     * @param quantity 限量數量
     * @return 限量特價促銷
     */
    public static Promotion createLimitedQuantityPromotion(
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String productId,
            double specialPrice,
            double regularPrice,
            int quantity) {

        PromotionId promotionId = new PromotionId();

        LimitedQuantityRule rule =
                new LimitedQuantityRule(
                        new ProductId(productId),
                        Money.of(specialPrice),
                        Money.of(regularPrice),
                        quantity,
                        promotionId.getId());

        Promotion promotion =
                new Promotion(promotionId, name, description, startDate, endDate, rule);

        // 設置規格
        promotion.setSpecification(new LimitedQuantitySpecification(rule));

        return promotion;
    }

    /**
     * 創建滿額贈禮促銷
     *
     * @param name 促銷名稱
     * @param description 促銷描述
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @param minimumPurchaseAmount 最低消費金額
     * @param giftProductId 贈品ID
     * @param giftValue 贈品價值
     * @param maxGiftsPerOrder 每筆訂單最大贈品數量
     * @param isMultipleGiftsAllowed 是否允許多件贈品
     * @return 滿額贈禮促銷
     */
    public static Promotion createGiftWithPurchasePromotion(
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            double minimumPurchaseAmount,
            String giftProductId,
            double giftValue,
            int maxGiftsPerOrder,
            boolean isMultipleGiftsAllowed) {

        PromotionId promotionId = new PromotionId();

        GiftWithPurchaseRule rule =
                new GiftWithPurchaseRule(
                        Money.of(minimumPurchaseAmount),
                        new ProductId(giftProductId),
                        Money.of(giftValue),
                        maxGiftsPerOrder,
                        isMultipleGiftsAllowed);

        Promotion promotion =
                new Promotion(promotionId, name, description, startDate, endDate, rule);

        // 設置規格
        promotion.setSpecification(new GiftWithPurchaseSpecification(rule));

        return promotion;
    }

    /**
     * 創建超商優惠券促銷
     *
     * @param name 促銷名稱
     * @param description 促銷描述
     * @param startDate 開始日期
     * @param endDate 結束日期
     * @param voucherName 優惠券名稱
     * @param price 價格
     * @param regularPrice 原價
     * @param validDays 有效天數
     * @param redemptionLocation 兌換地點
     * @param contents 內容
     * @param quantity 數量
     * @return 超商優惠券促銷
     */
    public static Promotion createConvenienceStoreVoucherPromotion(
            String name,
            String description,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String voucherName,
            double price,
            double regularPrice,
            int validDays,
            String redemptionLocation,
            String contents,
            int quantity) {

        PromotionId promotionId = new PromotionId();

        ConvenienceStoreVoucherRule rule =
                new ConvenienceStoreVoucherRule(
                        voucherName,
                        Money.of(price),
                        Money.of(regularPrice),
                        Period.ofDays(validDays),
                        redemptionLocation,
                        contents,
                        quantity);

        Promotion promotion =
                new Promotion(promotionId, name, description, startDate, endDate, rule);

        return promotion;
    }
}
