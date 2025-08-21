package solid.humank.genaidemo.domain.common.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

import solid.humank.genaidemo.domain.common.annotations.ValueObject;

/** 金錢值對象 - 使用 Record 實作 */
@ValueObject
public record Money(BigDecimal amount, Currency currency) {

    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.getInstance("TWD"));

    /**
     * 緊湊建構子 - 驗證參數
     */
    public Money {
        Objects.requireNonNull(amount, "金額不能為空");
        Objects.requireNonNull(currency, "貨幣不能為空");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("金額不能為負數");
        }
    }

    /**
     * 創建金錢值對象
     *
     * @param amount       金額
     * @param currencyCode 貨幣代碼
     * @return 金錢值對象
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        return new Money(amount, Currency.getInstance(currencyCode));
    }

    /**
     * 創建金錢值對象
     *
     * @param amount   金額
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
     * @param amount       金額
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
     * 創建零金額的金錢值對象（指定貨幣）
     *
     * @param currency 貨幣
     * @return 零金額的金錢值對象
     */
    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /**
     * 加法
     *
     * @param money 金錢
     * @return 新的金錢值對象
     */
    public Money add(Money money) {
        requireSameCurrency(money);
        return new Money(this.amount.add(money.amount), this.currency);
    }

    /**
     * 加法 - 別名方法
     *
     * @param money 金錢
     * @return 新的金錢值對象
     */
    public Money plus(Money money) {
        return add(money);
    }

    /**
     * 減法
     *
     * @param money 金錢
     * @return 新的金錢值對象
     */
    public Money subtract(Money money) {
        requireSameCurrency(money);
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
        return new Money(this.amount.divide(BigDecimal.valueOf(divisor), RoundingMode.HALF_UP), this.currency);
    }

    /**
     * 獲取金額（向後相容方法）
     *
     * @return 金額
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * 獲取貨幣（向後相容方法）
     *
     * @return 貨幣
     */
    public Currency getCurrency() {
        return currency;
    }

    /**
     * 比較金額是否大於另一個金額
     *
     * @param other 另一個金額
     * @return 是否大於
     */
    public boolean isGreaterThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    /**
     * 比較金額是否小於另一個金額
     *
     * @param other 另一個金額
     * @return 是否小於
     */
    public boolean isLessThan(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) < 0;
    }

    /**
     * 比較金額是否等於另一個金額
     *
     * @param other 另一個金額
     * @return 是否等於
     */
    public boolean isEqualTo(Money other) {
        requireSameCurrency(other);
        return this.amount.compareTo(other.amount) == 0;
    }

    /**
     * 檢查是否為零金額
     *
     * @return 是否為零
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * 檢查是否為正數
     *
     * @return 是否為正數
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 驗證貨幣是否相同
     *
     * @param other 另一個金錢對象
     */
    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Cannot operate on money with different currencies: "
                            + this.currency.getCurrencyCode()
                            + " vs "
                            + other.currency.getCurrencyCode());
        }
    }

    @Override
    public String toString() {
        return amount + " " + currency.getCurrencyCode();
    }
}
