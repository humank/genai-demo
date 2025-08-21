# MoneyMatchers 使用範例

修正後的 `MoneyMatchers` 現在可以正確處理貨幣比較：

## 修正前的問題

```java
// ❌ 錯誤：比較 Currency 對象和 String
return "TWD".equals(money.getCurrency());
```

## 修正後的解決方案

```java
// ✅ 正確：比較貨幣代碼
return "TWD".equals(money.getCurrency().getCurrencyCode());
```

## 使用範例

```java
import static solid.humank.genaidemo.testutils.matchers.MoneyMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@Test
void testMoneyMatchers() {
    Money twdMoney = Money.of(100, "TWD");
    Money usdMoney = Money.of(50, "USD");
    
    // 測試台幣
    assertThat(twdMoney, isTwd());
    
    // 測試美元
    assertThat(usdMoney, isUsd());
    
    // 測試任意貨幣
    assertThat(twdMoney, hasCurrency("TWD"));
    assertThat(usdMoney, hasCurrency("USD"));
    
    // 組合使用
    assertThat(twdMoney, hasAmount(100));
    assertThat(twdMoney, isTwd());
    assertThat(twdMoney, isPositive());
}
```

## 新增的匹配器

- `hasCurrency(String)` - 匹配指定貨幣代碼
- `isTwd()` - 匹配台幣
- `isUsd()` - 匹配美元  
- `isEur()` - 匹配歐元
