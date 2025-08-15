package solid.humank.genaidemo.domain.promotion.model.valueobject;

import java.time.Period;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.shoppingcart.model.aggregate.ShoppingCart;

/** 超商優惠券規則 */
@ValueObject
public final class ConvenienceStoreVoucherRule implements PromotionRule {
    private final String voucherName;
    private final Money price;
    private final Money regularPrice;
    private final Period validPeriod;
    private final String redemptionLocation;
    private final String contents;
    private final int quantity;

    public ConvenienceStoreVoucherRule(
            String voucherName,
            Money price,
            Money regularPrice,
            Period validPeriod,
            String redemptionLocation,
            String contents,
            int quantity) {
        this.voucherName = voucherName;
        this.price = price;
        this.regularPrice = regularPrice;
        this.validPeriod = validPeriod;
        this.redemptionLocation = redemptionLocation;
        this.contents = contents;
        this.quantity = quantity;
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
    public boolean matches(ShoppingCart cart) {
        // 超商優惠券通常沒有特殊條件，任何購物車都可以購買
        return true;
    }

    @Override
    public Money calculateDiscount(ShoppingCart cart) {
        // 超商優惠券的折扣是購買價格與原價的差額
        return regularPrice.subtract(price);
    }

    @Override
    public String getDescription() {
        return String.format("超商優惠券：%s，特價 %s（原價 %s）", voucherName, price, regularPrice);
    }
}
