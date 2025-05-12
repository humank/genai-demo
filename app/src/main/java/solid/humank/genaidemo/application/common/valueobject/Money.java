package solid.humank.genaidemo.application.common.valueobject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 金錢值對象 (應用層)
 */
public class Money {
    private final BigDecimal amount;
    private final String currency;
    
    public static final Money ZERO = new Money(BigDecimal.ZERO, "TWD");
    
    public Money(BigDecimal amount, String currency) {
        this.amount = amount;
        this.currency = currency;
    }
    
    /**
     * 創建金錢值對象
     * 
     * @param amount 金額
     * @param currency 貨幣
     * @return 金錢值對象
     */
    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }
    
    /**
     * 創建金錢值對象
     * 
     * @param amount 金額
     * @return 金錢值對象，使用默認貨幣 TWD
     */
    public static Money of(BigDecimal amount) {
        return new Money(amount, "TWD");
    }
    
    /**
     * 創建金錢值對象
     * 
     * @param amount 金額
     * @return 金錢值對象，使用默認貨幣 TWD
     */
    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount), "TWD");
    }
    
    /**
     * 創建金錢值對象
     * 
     * @param amount 金額
     * @param currency 貨幣
     * @return 金錢值對象
     */
    public static Money of(double amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
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
     * 獲取貨幣
     * 
     * @return 貨幣
     */
    public String getCurrency() {
        return currency;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return Objects.equals(amount, money.amount) && Objects.equals(currency, money.currency);
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