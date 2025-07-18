package solid.humank.genaidemo.testutils.matchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import solid.humank.genaidemo.domain.common.valueobject.Money;

import java.math.BigDecimal;

/**
 * 金額自定義匹配器
 * 提供更具表達性的金額斷言
 */
public class MoneyMatchers {
    
    /**
     * 匹配金額值
     */
    public static Matcher<Money> hasAmount(BigDecimal expectedAmount) {
        return new TypeSafeMatcher<Money>() {
            @Override
            protected boolean matchesSafely(Money money) {
                return expectedAmount.compareTo(money.getAmount()) == 0;
            }
            
            @Override
            public void describeTo(Description description) {
                description.appendText("money with amount ").appendValue(expectedAmount);
            }
            
            @Override
            protected void describeMismatchSafely(Money money, Description mismatchDescription) {
                mismatchDescription.appendText("money had amount ").appendValue(money.getAmount());
            }
        };
    }
    
    /**
     * 匹配金額值（整數）
     */
    public static Matcher<Money> hasAmount(int expectedAmount) {
        return hasAmount(new BigDecimal(expectedAmount));
    }
    
    /**
     * 匹配零金額
     */
    public static Matcher<Money> isZero() {
        return hasAmount(BigDecimal.ZERO);
    }
    
    /**
     * 匹配正金額
     */
    public static Matcher<Money> isPositive() {
        return new TypeSafeMatcher<Money>() {
            @Override
            protected boolean matchesSafely(Money money) {
                return money.getAmount().compareTo(BigDecimal.ZERO) > 0;
            }
            
            @Override
            public void describeTo(Description description) {
                description.appendText("positive money amount");
            }
            
            @Override
            protected void describeMismatchSafely(Money money, Description mismatchDescription) {
                mismatchDescription.appendText("money amount was ").appendValue(money.getAmount());
            }
        };
    }
    
    /**
     * 匹配負金額
     */
    public static Matcher<Money> isNegative() {
        return new TypeSafeMatcher<Money>() {
            @Override
            protected boolean matchesSafely(Money money) {
                return money.getAmount().compareTo(BigDecimal.ZERO) < 0;
            }
            
            @Override
            public void describeTo(Description description) {
                description.appendText("negative money amount");
            }
            
            @Override
            protected void describeMismatchSafely(Money money, Description mismatchDescription) {
                mismatchDescription.appendText("money amount was ").appendValue(money.getAmount());
            }
        };
    }
    
    /**
     * 匹配大於指定金額
     */
    public static Matcher<Money> isGreaterThan(BigDecimal threshold) {
        return new TypeSafeMatcher<Money>() {
            @Override
            protected boolean matchesSafely(Money money) {
                return money.getAmount().compareTo(threshold) > 0;
            }
            
            @Override
            public void describeTo(Description description) {
                description.appendText("money amount greater than ").appendValue(threshold);
            }
            
            @Override
            protected void describeMismatchSafely(Money money, Description mismatchDescription) {
                mismatchDescription.appendText("money amount was ").appendValue(money.getAmount());
            }
        };
    }
    
    /**
     * 匹配小於指定金額
     */
    public static Matcher<Money> isLessThan(BigDecimal threshold) {
        return new TypeSafeMatcher<Money>() {
            @Override
            protected boolean matchesSafely(Money money) {
                return money.getAmount().compareTo(threshold) < 0;
            }
            
            @Override
            public void describeTo(Description description) {
                description.appendText("money amount less than ").appendValue(threshold);
            }
            
            @Override
            protected void describeMismatchSafely(Money money, Description mismatchDescription) {
                mismatchDescription.appendText("money amount was ").appendValue(money.getAmount());
            }
        };
    }
    
    /**
     * 匹配金額範圍
     */
    public static Matcher<Money> isBetween(BigDecimal min, BigDecimal max) {
        return new TypeSafeMatcher<Money>() {
            @Override
            protected boolean matchesSafely(Money money) {
                BigDecimal amount = money.getAmount();
                return amount.compareTo(min) >= 0 && amount.compareTo(max) <= 0;
            }
            
            @Override
            public void describeTo(Description description) {
                description.appendText("money amount between ").appendValue(min)
                    .appendText(" and ").appendValue(max);
            }
            
            @Override
            protected void describeMismatchSafely(Money money, Description mismatchDescription) {
                mismatchDescription.appendText("money amount was ").appendValue(money.getAmount());
            }
        };
    }
    
    /**
     * 匹配台幣
     */
    public static Matcher<Money> isTwd() {
        return new TypeSafeMatcher<Money>() {
            @Override
            protected boolean matchesSafely(Money money) {
                return "TWD".equals(money.getCurrency());
            }
            
            @Override
            public void describeTo(Description description) {
                description.appendText("TWD currency");
            }
            
            @Override
            protected void describeMismatchSafely(Money money, Description mismatchDescription) {
                mismatchDescription.appendText("currency was ").appendValue(money.getCurrency());
            }
        };
    }
}