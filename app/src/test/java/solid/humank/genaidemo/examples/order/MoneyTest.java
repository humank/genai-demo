package solid.humank.genaidemo.examples.order;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Money 值對象的單元測試
 */
class MoneyTest {

    @Nested
    @DisplayName("建立金額測試")
    class MoneyCreationTests {
        @Test
        @DisplayName("應該成功建立金額")
        void testMoneyCreation() {
            // 準備
            BigDecimal amount = new BigDecimal("100.50");
            
            // 執行
            Money money = new Money(amount);
            
            // 驗證
            assertEquals(amount.setScale(2), money.getAmount());
            assertEquals(Currency.TWD, money.getCurrency());
        }
        
        @Test
        @DisplayName("應該成功建立指定貨幣的金額")
        void testMoneyCreationWithCurrency() {
            // 準備
            BigDecimal amount = new BigDecimal("100.50");
            Currency currency = Currency.USD;
            
            // 執行
            Money money = new Money(amount, currency);
            
            // 驗證
            assertEquals(amount.setScale(2), money.getAmount());
            assertEquals(currency, money.getCurrency());
        }
        
        @Test
        @DisplayName("金額不能為null")
        void testAmountCannotBeNull() {
            // 執行與驗證
            NullPointerException exception = assertThrows(NullPointerException.class, () -> {
                new Money(null);
            });
            
            assertEquals("Amount cannot be null", exception.getMessage());
        }
        
        @Test
        @DisplayName("貨幣不能為null")
        void testCurrencyCannotBeNull() {
            // 準備
            BigDecimal amount = new BigDecimal("100.50");
            
            // 執行與驗證
            NullPointerException exception = assertThrows(NullPointerException.class, () -> {
                new Money(amount, null);
            });
            
            assertEquals("Currency cannot be null", exception.getMessage());
        }
        
        @Test
        @DisplayName("應該成功使用of方法建立金額")
        void testMoneyOf() {
            // 執行
            Money money = Money.of(100.50);
            
            // 驗證
            assertEquals(new BigDecimal("100.50").setScale(2), money.getAmount());
            assertEquals(Currency.TWD, money.getCurrency());
        }
        
        @Test
        @DisplayName("應該成功使用twd方法建立台幣金額")
        void testMoneyTwd() {
            // 執行
            Money money1 = Money.twd(100);
            Money money2 = Money.twd(100.50);
            
            // 驗證
            assertEquals(new BigDecimal("100.00"), money1.getAmount());
            assertEquals(new BigDecimal("100.50"), money2.getAmount());
            assertEquals(Currency.TWD, money1.getCurrency());
            assertEquals(Currency.TWD, money2.getCurrency());
        }
        
        @Test
        @DisplayName("應該成功建立零金額")
        void testMoneyZero() {
            // 執行
            Money money = Money.zero();
            
            // 驗證
            assertEquals(BigDecimal.ZERO.setScale(2), money.getAmount());
            assertEquals(Currency.TWD, money.getCurrency());
            assertTrue(money.isZero());
        }
    }
    
    @Nested
    @DisplayName("金額運算測試")
    class MoneyOperationTests {
        @Test
        @DisplayName("應該正確執行金額加法")
        void testAdd() {
            // 準備
            Money money1 = Money.of(100.50);
            Money money2 = Money.of(50.25);
            
            // 執行
            Money result = money1.add(money2);
            
            // 驗證
            assertEquals(new BigDecimal("150.75"), result.getAmount());
        }
        
        @Test
        @DisplayName("應該正確執行金額減法")
        void testSubtract() {
            // 準備
            Money money1 = Money.of(100.50);
            Money money2 = Money.of(50.25);
            
            // 執行
            Money result = money1.subtract(money2);
            
            // 驗證
            assertEquals(new BigDecimal("50.25"), result.getAmount());
        }
        
        @Test
        @DisplayName("應該正確執行金額乘法(整數)")
        void testMultiplyWithInt() {
            // 準備
            Money money = Money.of(100.50);
            int multiplier = 3;
            
            // 執行
            Money result = money.multiply(multiplier);
            
            // 驗證
            assertEquals(new BigDecimal("301.50"), result.getAmount());
        }
        
        @Test
        @DisplayName("應該正確執行金額乘法(小數)")
        void testMultiplyWithDouble() {
            // 準備
            Money money = Money.of(100.00);
            double multiplier = 0.75;
            
            // 執行
            Money result = money.multiply(multiplier);
            
            // 驗證
            assertEquals(new BigDecimal("75.00"), result.getAmount());
        }
    }
    
    @Nested
    @DisplayName("金額比較測試")
    class MoneyComparisonTests {
        @Test
        @DisplayName("應該正確比較金額大小")
        void testComparison() {
            // 準備
            Money smaller = Money.of(50.00);
            Money larger = Money.of(100.00);
            
            // 驗證
            assertTrue(larger.isGreaterThan(smaller));
            assertTrue(smaller.isLessThan(larger));
            assertFalse(smaller.isGreaterThan(larger));
            assertFalse(larger.isLessThan(smaller));
        }
        
        @Test
        @DisplayName("應該正確判斷金額是否為零")
        void testIsZero() {
            // 準備
            Money zero = Money.zero();
            Money nonZero = Money.of(0.01);
            
            // 驗證
            assertTrue(zero.isZero());
            assertFalse(nonZero.isZero());
        }
    }
    
    @Nested
    @DisplayName("金額相等性測試")
    class MoneyEqualityTests {
        @Test
        @DisplayName("相同金額應該相等")
        void testMoneyEquality() {
            // 準備
            Money money1 = Money.of(100.50);
            Money money2 = Money.of(100.50);
            
            // 驗證
            assertEquals(money1, money2);
            assertEquals(money1.hashCode(), money2.hashCode());
        }
        
        @Test
        @DisplayName("不同金額不應該相等")
        void testMoneyInequality() {
            // 準備
            Money money1 = Money.of(100.50);
            Money money2 = Money.of(100.51);
            
            // 驗證
            assertNotEquals(money1, money2);
        }
        
        @Test
        @DisplayName("金額不應該等於null或其他類型")
        void testMoneyNotEqualToNullOrOtherTypes() {
            // 準備
            Money money = Money.of(100.50);
            
            // 驗證
            assertNotEquals(money, null);
            assertNotEquals(money, "Not a Money");
        }
    }
    
    @Test
    @DisplayName("金額的toString方法應該返回金額字符串")
    void testToString() {
        // 準備
        Money money = Money.of(100.50);
        
        // 執行
        String result = money.toString();
        
        // 驗證
        assertEquals("100.50", result);
    }
}