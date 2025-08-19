package solid.humank.genaidemo.domain.product.model.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;

/** 捆綁銷售折扣 */
@ValueObject
public class BundleDiscount {
    private Money regularPrice;
    private Money discountedPrice;
    private int discountPercentage;
    private boolean isFixedPrice;

    // 固定價格折扣建構函數
    public BundleDiscount(Money regularPrice, Money discountedPrice) {
        this.regularPrice = regularPrice;
        this.discountedPrice = discountedPrice;
        this.isFixedPrice = true;

        // 計算折扣百分比（僅供參考）
        if (regularPrice.getAmount().doubleValue() > 0) {
            double discount =
                    1
                            - (discountedPrice.getAmount().doubleValue()
                                    / regularPrice.getAmount().doubleValue());
            this.discountPercentage = (int) Math.round(discount * 100);
        }
    }

    // 百分比折扣建構函數
    public BundleDiscount(int discountPercentage) {
        this.discountPercentage = discountPercentage;
        this.isFixedPrice = false;
    }

    public Money getRegularPrice() {
        return regularPrice;
    }

    public Money getDiscountedPrice() {
        return discountedPrice;
    }

    public int getDiscountPercentage() {
        return discountPercentage;
    }

    public boolean isFixedPrice() {
        return isFixedPrice;
    }

    // 計算折扣金額
    public Money calculateDiscount(Money originalPrice) {
        if (isFixedPrice) {
            return regularPrice.subtract(discountedPrice);
        } else {
            return originalPrice.multiply(discountPercentage / 100.0);
        }
    }

    // 應用折扣
    public Money applyDiscount(Money originalPrice) {
        if (isFixedPrice) {
            return discountedPrice;
        } else {
            return originalPrice.subtract(calculateDiscount(originalPrice));
        }
    }
}
