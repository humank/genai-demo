package solid.humank.genaidemo.examples.order.model.valueobject;

import solid.humank.genaidemo.ddd.annotations.ValueObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 金額值對象
 */
@ValueObject
public class Money {
    private final BigDecimal amount;
    private final Currency currency;
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_EVEN;
    private static final int DEFAULT_SCALE = 2;

    public Money(BigDecimal amount) {
        this(amount, Currency.TWD);
    }
    
    public Money(BigDecimal amount, Currency currency) {
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null")
                .setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
    }

    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }
    
    public static Money twd(int amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.TWD);
    }
    
    public static Money twd(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.TWD);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money subtract(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)));
    }

    public Money multiply(double multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)));
    }

    public boolean isGreaterThan(Money other) {
        return this.amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        return this.amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return BigDecimal.ZERO.compareTo(this.amount) == 0;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    
    public BigDecimal amount() {
        return getAmount();
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    public Currency currency() {
        return getCurrency();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount);
    }

    @Override
    public String toString() {
        return amount.toString();
    }
}