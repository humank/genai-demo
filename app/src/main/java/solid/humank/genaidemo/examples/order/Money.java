package solid.humank.genaidemo.examples.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

import solid.humank.genaidemo.ddd.annotations.ValueObject;

@ValueObject
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount == null) throw new IllegalArgumentException("金額不能為空");
        if (currency == null) throw new IllegalArgumentException("幣別不能為空");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(double amount, String currencyCode) {
        return new Money(
            BigDecimal.valueOf(amount),
            Currency.getInstance(currencyCode)
        );
    }

    public static Money twd(double amount) {
        return of(amount, "TWD");
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("不能加總不同幣別的金額");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }

    @Override
    public String toString() {
        return amount.toString() + " " + currency.getCurrencyCode();
    }
}
