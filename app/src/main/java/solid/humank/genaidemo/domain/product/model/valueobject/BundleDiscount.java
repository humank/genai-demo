package solid.humank.genaidemo.domain.product.model.valueobject;

import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;

/** 捆綁銷售折扣 */
@ValueObject
public record BundleDiscount(
        Money regularPrice,
        Money discountedPrice,
        int discountPercentage,
        boolean isFixedPrice) {

    // 固定價格折扣建構函數
    public BundleDiscount(Money regularPrice, Money discountedPrice) {
        this(
                Objects.requireNonNull(regularPrice, "Regular price cannot be null"),
                Objects.requireNonNull(discountedPrice, "Discounted price cannot be null"),
                calculateDiscountPercentage(regularPrice, discountedPrice),
                true);
    }

    // 百分比折扣建構函數
    public BundleDiscount(int discountPercentage) {
        this(null, null, validateDiscountPercentage(discountPercentage), false);
    }

    public BundleDiscount {
        if (isFixedPrice) {
            Objects.requireNonNull(regularPrice, "Regular price cannot be null for fixed price discount");
            Objects.requireNonNull(discountedPrice, "Discounted price cannot be null for fixed price discount");
            if (discountedPrice.amount().compareTo(regularPrice.amount()) > 0) {
                throw new IllegalArgumentException("Discounted price cannot be higher than regular price");
            }
        } else {
            if (discountPercentage < 0 || discountPercentage > 100) {
                throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
            }
        }
    }

    private static int calculateDiscountPercentage(Money regularPrice, Money discountedPrice) {
        if (regularPrice.amount().doubleValue() > 0) {
            double discount = 1 - (discountedPrice.amount().doubleValue() / regularPrice.amount().doubleValue());
            return (int) Math.round(discount * 100);
        }
        return 0;
    }

    private static int validateDiscountPercentage(int discountPercentage) {
        if (discountPercentage < 0 || discountPercentage > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
        return discountPercentage;
    }

    public static BundleDiscount fixedPrice(Money regularPrice, Money discountedPrice) {
        return new BundleDiscount(regularPrice, discountedPrice);
    }

    public static BundleDiscount percentage(int discountPercentage) {
        return new BundleDiscount(discountPercentage);
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
