package solid.humank.genaidemo.domain.promotion.model.valueobject;

import java.time.Period;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;

/** 超商優惠券規則 */
@ValueObject
public record ConvenienceStoreVoucherRule(
        String voucherName,
        Money price,
        Money regularPrice,
        Period validPeriod,
        String redemptionLocation,
        String contents,
        int quantity) implements PromotionRule {

    public ConvenienceStoreVoucherRule {
        Objects.requireNonNull(voucherName, "Voucher name cannot be null");
        Objects.requireNonNull(price, "Price cannot be null");
        Objects.requireNonNull(regularPrice, "Regular price cannot be null");
        Objects.requireNonNull(validPeriod, "Valid period cannot be null");
        Objects.requireNonNull(redemptionLocation, "Redemption location cannot be null");
        Objects.requireNonNull(contents, "Contents cannot be null");

        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (price.amount().compareTo(regularPrice.amount()) > 0) {
            throw new IllegalArgumentException("Price cannot be higher than regular price");
        }
    }

    public static ConvenienceStoreVoucherRule create(
            String voucherName,
            Money price,
            Money regularPrice,
            Period validPeriod,
            String redemptionLocation,
            String contents,
            int quantity) {
        return new ConvenienceStoreVoucherRule(
                voucherName, price, regularPrice, validPeriod, redemptionLocation, contents, quantity);
    }

    public String getVoucherName() {
        return voucherName;
    }

    public Money getPrice() {
        return price;
    }

    public Money getRegularPrice() {
        return regularPrice;
    }

    public Period getValidPeriod() {
        return validPeriod;
    }

    public String getRedemptionLocation() {
        return redemptionLocation;
    }

    public String getContents() {
        return contents;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public boolean matches(CartSummary cartSummary) {
        // 超商優惠券通常沒有特殊條件，任何購物車都可以購買
        return true;
    }

    @Override
    public Money calculateDiscount(CartSummary cartSummary) {
        // 超商優惠券的折扣是購買價格與原價的差額
        return regularPrice.subtract(price);
    }

    @Override
    public String getDescription() {
        return String.format("超商優惠券：%s，特價 %s（原價 %s）", voucherName, price, regularPrice);
    }
}
