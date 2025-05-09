package solid.humank.genaidemo.domain.common.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 共享核心中的金額值物件
 * 用於在不同 Bounded Context 之間共享金額計算邏輯
 */
@ValueObject
public final class Amount {
    private final BigDecimal value;
    private final Currency currency;

    private Amount(BigDecimal value, Currency currency) {
        this.value = value.setScale(currency.getDecimalPlaces(), RoundingMode.HALF_UP);
        this.currency = currency;
    }

    public static Amount of(BigDecimal value, Currency currency) {
        Objects.requireNonNull(value, "金額不能為空");
        Objects.requireNonNull(currency, "幣別不能為空");
        return new Amount(value, currency);
    }

    public static Amount of(double value, Currency currency) {
        return of(BigDecimal.valueOf(value), currency);
    }

    public static Amount zero(Currency currency) {
        return of(BigDecimal.ZERO, currency);
    }

    public Amount add(Amount other) {
        requireSameCurrency(other);
        return new Amount(value.add(other.value), currency);
    }

    public Amount subtract(Amount other) {
        requireSameCurrency(other);
        return new Amount(value.subtract(other.value), currency);
    }

    public Amount multiply(int multiplier) {
        return new Amount(value.multiply(BigDecimal.valueOf(multiplier)), currency);
    }

    public Amount multiply(double multiplier) {
        return new Amount(value.multiply(BigDecimal.valueOf(multiplier)), currency);
    }

    private void requireSameCurrency(Amount other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                "Cannot operate on amounts with different currencies: " +
                this.currency + " vs " + other.currency
            );
        }
    }

    public boolean isGreaterThan(Amount other) {
        requireSameCurrency(other);
        return this.value.compareTo(other.value) > 0;
    }

    public boolean isLessThan(Amount other) {
        requireSameCurrency(other);
        return this.value.compareTo(other.value) < 0;
    }

    public BigDecimal getValue() {
        return value;
    }

    public Currency getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Amount amount = (Amount) o;
        return value.compareTo(amount.value) == 0 && currency == amount.currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, currency);
    }

    @Override
    public String toString() {
        return value.toString() + " " + currency.getCode();
    }
}
