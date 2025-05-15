package solid.humank.genaidemo.domain.common.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * 金錢值對象
 */
@ValueObject
public class Money {
    private final BigDecimal amount;
    private final Currency currency;
    
    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.getInstance("TWD"));
    
    public Money(BigDecimal amount, Currency currency) {
        this.amount = amount;
        this.currency = currency;
    }
    
    /**
     * 創建金錢值對象
     * 
     * @param amount 金額
     * @param currencyCode 貨幣代碼
     * @return 金錢值對象
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }
    
    /**
     * 創建金錢值對象
     * 
     * @param amount 金額
     * @param currency 貨幣
     * @return 金錢值對象
     */
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }
    
    /**
     * 創建金錢值對象
     * 
     * @param amount 金額
     * @return 金錢值對象，使用默認貨幣 TWD
     */
    public static Money of(BigDecimal amount) {
        return new Money(amount, Currency.getInstance("TWD"));
    }
    
    /**
     * 創建金錢值對象
     * 
     * @param amount 金額
     * @return 金錢值對象，使用默認貨幣 TWD
     */
    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("TWD"));
    }
    
    /**
     * 創建金錢值對象
     * 
     * @param amount 金額
     * @param currencyCode 貨幣代碼
     * @return 金錢值對象
     */
    public static Money of(double amount, String currencyCode) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance(currencyCode));
    }
    
    /**
     * 創建台幣金錢值對象
     * 
     * @param amount 金額
     * @return 金錢值對象，使用 TWD 貨幣
     */
    public static Money twd(double amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("TWD"));
    }
    
    /**
     * 創建台幣金錢值對象
     * 
     * @param amount 金額
     * @return 金錢值對象，使用 TWD 貨幣
     */
    public static Money twd(int amount) {
        return new Money(BigDecimal.valueOf(amount), Currency.getInstance("TWD"));
    }
    
    /**
     * 創建零金額的金錢值對象
     * 
     * @return 零金額的金錢值對象
     */
    public static Money zero() {
        return ZERO;
    }
    
    /**
     * 加法
     * 
     * @param money 金錢
     * @return 新的金錢值對象
     */
    public Money add(Money money) {
        if (!this.currency.equals(money.currency)) {
            throw new IllegalArgumentException("Cannot add money with different currencies");
        }
        return new Money(this.amount.add(money.amount), this.currency);
    }
    
    /**
     * 加法 - 使用 Java 21 的 StringTemplate
     * 
     * @param money 金錢
     * @return 新的金錢值對象
     */
    public Money plus(Money money) {
        if (!this.currency.equals(money.currency)) {
            var errorMsg = STR."Cannot add money with different currencies: \{this.currency} vs \{money.currency}";
            throw new IllegalArgumentException(errorMsg);
        }
        return new Money(this.amount.add(money.amount), this.currency);
    }
    
    /**
     * 減法
     * 
     * @param money 金錢
     * @return 新的金錢值對象
     */
    public Money subtract(Money money) {
        if (!this.currency.equals(money.currency)) {
            throw new IllegalArgumentException("Cannot subtract money with different currencies");
        }
        return new Money(this.amount.subtract(money.amount), this.currency);
    }
    
    /**
     * 乘法
     * 
     * @param multiplier 乘數
     * @return 新的金錢值對象
     */
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }
    
    /**
     * 乘法
     * 
     * @param multiplier 乘數
     * @return 新的金錢值對象
     */
    public Money multiply(double multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }
    
    /**
     * 除法
     * 
     * @param divisor 除數
     * @return 新的金錢值對象
     */
    public Money divide(int divisor) {
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor)), this.currency);
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
     * 獲取金額（用於兼容舊代碼）
     * 
     * @return 金額
     */
    public BigDecimal amount() {
        return amount;
    }
    
    /**
     * 獲取貨幣
     * 
     * @return 貨幣
     */
    public Currency getCurrency() {
        return currency;
    }
    
    /**
     * 獲取貨幣（用於兼容舊代碼）
     * 
     * @return 貨幣
     */
    public Currency currency() {
        return currency;
    }
    
    /**
     * 比較金額是否大於另一個金額
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Money money) {
            return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
    
    @Override
    public String toString() {
        return amount + " " + currency.getCurrencyCode();
    }
}