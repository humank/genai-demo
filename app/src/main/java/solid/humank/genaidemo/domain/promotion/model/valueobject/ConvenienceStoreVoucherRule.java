package solid.humank.genaidemo.domain.promotion.model.valueobject;

import java.time.Period;
import solid.humank.genaidemo.domain.common.annotations.ValueObject;
import solid.humank.genaidemo.domain.common.valueobject.Money;

/** 超商優惠券規則 */
@ValueObject
public class ConvenienceStoreVoucherRule {
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
}
