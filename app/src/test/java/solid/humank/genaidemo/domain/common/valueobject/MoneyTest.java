package solid.humank.genaidemo.domain.common.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void testMoneyCreation() {
        Money money = Money.twd(100);
        assertEquals(BigDecimal.valueOf(100), money.amount());
        assertEquals(Currency.getInstance("TWD"), money.currency());
    }

    @Test
    void testMoneyAddition() {
        Money money1 = Money.twd(100);
        Money money2 = Money.twd(50);
        Money result = money1.add(money2);
        assertEquals(BigDecimal.valueOf(150), result.amount());
    }

    @Test
    void testMoneyEquality() {
        Money money1 = Money.twd(100);
        Money money2 = Money.twd(100);
        assertEquals(money1, money2);
    }

    @Test
    void testBackwardCompatibility() {
        Money money = Money.twd(100);
        // 測試舊的 getter 方法
        assertEquals(BigDecimal.valueOf(100), money.getAmount());
        assertEquals(Currency.getInstance("TWD"), money.getCurrency());
    }
}