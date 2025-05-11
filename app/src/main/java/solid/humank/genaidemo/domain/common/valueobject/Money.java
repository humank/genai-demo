package solid.humank.genaidemo.domain.common.valueobject;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 金額值對象
 * 
 * 表示貨幣金額，包含數值和貨幣單位。
 * 作為值對象，它是不可變的，所有屬性在創建後不能被修改。
 */
@ValueObject
public class Money {
    private final BigDecimal amount;
    private final String currency;
    
    private static final String DEFAULT_CURRENCY = "TWD";
    
    /**
     * 建立金額
     * 
     * @param amount 金額數值
     * @param currency 貨幣單位
     */
    private Money(BigDecimal amount, String currency) {
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.currency = Objects.requireNonNull(currency, "Currency cannot be null");
    }
    
    /**
     * 建立金額，使用默認貨幣單位
     * 
     * @param amount 金額數值
     * @return 金額值對象
     */
    public static Money of(BigDecimal amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }
    
    /**
     * 建立金額，使用指定貨幣單位
     * 
     * @param amount 金額數值
     * @param currency 貨幣單位
     * @return 金額值對象
     */
    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }
    
    /**
     * 建立金額，使用整數金額和默認貨幣單位
     * 
     * @param amount 整數金額
     * @return 金額值對象
     */
    public static Money of(int amount) {
        return new Money(BigDecimal.valueOf(amount), DEFAULT_CURRENCY);
    }
    
    /**
     * 建立金額，使用整數金額和指定貨幣單位
     * 
     * @param amount 整數金額
     * @param currency 貨幣單位
     * @return 金額值對象
     */
    public static Money of(int amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }
    
    /**
     * 建立台幣金額
     * 
     * @param amount 金額數值
     * @return 金額值對象
     */
    public static Money twd(int amount) {
        return of(amount, "TWD");
    }
    
    /**
     * 建立零金額
     * 
     * @return 金額值對象
     */
    public static Money zero() {
        return of(BigDecimal.ZERO);
    }
    
    /**
     * 獲取金額數值
     * 
     * @return 金額數值
     */
    public BigDecimal getAmount() {
        return amount;
    }
    
    /**
     * 獲取金額數值（用於兼容舊代碼）
     * 
     * @return 金額數值
     */
    public BigDecimal amount() {
        return amount;
    }
    
    /**
     * 獲取貨幣單位
     * 
     * @return 貨幣單位
     */
    public String getCurrency() {
        return currency;
    }
    
    /**
     * 獲取貨幣單位（用於兼容舊代碼）
     * 
     * @return 貨幣單位
     */
    public String currency() {
        return currency;
    }
    
    /**
     * 加法運算
     * 
     * @param other 另一個金額
     * @return 相加後的新金額
     */
    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add money with different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    /**
     * 減法運算
     * 
     * @param other 另一個金額
     * @return 相減後的新金額
     */
    public Money subtract(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract money with different currencies");
        }
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    
    /**
     * 乘法運算
     * 
     * @param multiplier 乘數
     * @return 相乘後的新金額
     */
    public Money multiply(int multiplier) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(multiplier)), this.currency);
    }
    
    /**
     * 檢查金額是否為負數或零
     * 
     * @return 是否為負數或零
     */
    public boolean isNegativeOrZero() {
        return amount.compareTo(BigDecimal.ZERO) <= 0;
    }
    
    /**
     * 檢查金額是否大於另一個金額
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
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
    
    @Override
    public String toString() {
        return amount + " " + currency;
    }
}