package solid.humank.genaidemo.domain.common.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/**
 * 共享金額值對象
 * 不可變的金額表示，包含金額和幣別
 */
@ValueObject
public final class Money {
    private final BigDecimal amount;
    private final Currency currency;

    /**
     * 建立金額
     * 
     * @param amount 金額
     */
    public Money(BigDecimal amount) {
        this(amount, Currency.TWD);
    }

    /**
     * 建立金額
     * 
     * @param amount 金額
     * @param currency 幣別
     */
    public Money(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }

    /**
     * 建立金額
     * 
     * @param amount 金額
     */
    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }
    
    /**
     * 建立金額
     * 
     * @param amount 金額
     */
    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    /**
     * 建立台幣金額
     * 
     * @param amount 金額
     */
    public static Money twd(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.TWD);
    }

    /**
     * 建立台幣金額
     * 
     * @param amount 金額
     */
    public static Money twd(int amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.TWD);
    }

    /**
     * 建立美金金額
     * 
     * @param amount 金額
     */
    public static Money usd(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.USD);
    }

    /**
     * 建立零金額
     */
    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    /**
     * 加法
     * 
     * @param other 另一個金額
     * @return 相加後的金額
     */
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add money with different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * 減法
     * 
     * @param other 另一個金額
     * @return 相減後的金額
     */
    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract money with different currencies");
        }
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * 乘法
     * 
     * @param multiplier 乘數
     * @return 相乘後的金額
     */
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    /**
     * 乘法
     * 
     * @param multiplier 乘數
     * @return 相乘後的金額
     */
    public Money multiply(double multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }

    /**
     * 是否為零
     * 
     * @return 是否為零
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 是否大於另一個金額
     * 
     * @param other 另一個金額
     * @return 是否大於
     */
    public boolean isGreaterThan(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare money with different currencies");
        }
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * 是否小於另一個金額
     * 
     * @param other 另一個金額
     * @return 是否小於
     */
    public boolean isLessThan(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare money with different currencies");
        }
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * 獲取金額
     * 
     * @return 金額
     */
    public BigDecimal amount() {
        return amount;
    }

    /**
     * 獲取幣別
     * 
     * @return 幣別
     */
    public Currency currency() {
        return currency;
    }

    /**
     * 獲取金額
     * 
     * @return 金額
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * 獲取幣別
     * 
     * @return 幣別
     */
    public Currency getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && currency == money.currency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return amount.toString();
    }
    
    /**
     * 貨幣枚舉
     */
    public enum Currency {
        TWD("新台幣"),
        USD("美元"),
        EUR("歐元"),
        JPY("日元"),
        CNY("人民幣");
        
        private final String displayName;
        
        Currency(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
}