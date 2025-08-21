package solid.humank.genaidemo.domain.promotion.model.valueobject;

import java.time.LocalDateTime;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.product.model.valueobject.ProductId;

/** 閃購特價規則 */
@ValueObject(name = "FlashSaleRule", description = "閃購特價規則")
public record FlashSaleRule(
        ProductId targetProductId, Money specialPrice, int quantityLimit, DateRange flashSalePeriod)
        implements PromotionRule {

    public FlashSaleRule {
        if (targetProductId == null) {
            throw new IllegalArgumentException("目標商品ID不能為空");
        }
        if (specialPrice == null
                || specialPrice.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("特價必須大於 0");
        }
        if (quantityLimit <= 0) {
            throw new IllegalArgumentException("數量限制必須大於 0");
        }
        if (flashSalePeriod == null) {
            throw new IllegalArgumentException("閃購期間不能為空");
        }
    }

    @Override
    public boolean matches(CartSummary cartSummary) {
        // 檢查是否在閃購期間
        if (!flashSalePeriod.contains(LocalDateTime.now())) {
            return false;
        }

        // 檢查購物車是否包含目標商品
        return cartSummary.items().stream()
                .anyMatch(item -> item.productId().equals(targetProductId.getId()));
    }

    @Override
    public Money calculateDiscount(CartSummary cartSummary) {
        if (!matches(cartSummary)) {
            return Money.twd(0);
        }

        // 找到目標商品項目
        return cartSummary.items().stream()
                .filter(item -> item.productId().equals(targetProductId.getId()))
                .findFirst()
                .map(
                        item -> {
                            // 計算原價與特價的差額
                            Money originalPrice = item.unitPrice();
                            Money discount = originalPrice.subtract(specialPrice);

                            // 應用數量限制
                            int applicableQuantity = Math.min(item.quantity(), quantityLimit);
                            return discount.multiply(applicableQuantity);
                        })
                .orElse(Money.twd(0));
    }

    @Override
    public String getDescription() {
        return String.format(
                "閃購特價：%s 特價 %s，限量 %d 件", targetProductId.getId(), specialPrice, quantityLimit);
    }

    public boolean isActive() {
        return flashSalePeriod.isActive();
    }

    public boolean isExpired() {
        return flashSalePeriod.isExpired();
    }
}
