package solid.humank.genaidemo.testutils.matchers;

import java.math.BigDecimal;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import solid.humank.genaidemo.domain.common.valueobject.OrderStatus;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;

/** 訂單自定義匹配器 提供更具表達性的訂單斷言 */
public class OrderMatchers {

    /** 匹配訂單狀態 */
    public static Matcher<Order> hasStatus(OrderStatus expectedStatus) {
        return new TypeSafeMatcher<Order>() {
            @Override
            protected boolean matchesSafely(Order order) {
                return expectedStatus.equals(order.getStatus());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("order with status ").appendValue(expectedStatus);
            }

            @Override
            protected void describeMismatchSafely(Order order, Description mismatchDescription) {
                mismatchDescription.appendText("order had status ").appendValue(order.getStatus());
            }
        };
    }

    /** 匹配訂單總金額 */
    public static Matcher<Order> hasTotalAmount(BigDecimal expectedAmount) {
        return new TypeSafeMatcher<Order>() {
            @Override
            protected boolean matchesSafely(Order order) {
                return expectedAmount.compareTo(order.getTotalAmount().getAmount()) == 0;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("order with total amount ").appendValue(expectedAmount);
            }

            @Override
            protected void describeMismatchSafely(Order order, Description mismatchDescription) {
                mismatchDescription
                        .appendText("order had total amount ")
                        .appendValue(order.getTotalAmount().getAmount());
            }
        };
    }

    /** 匹配訂單有效金額 */
    public static Matcher<Order> hasEffectiveAmount(BigDecimal expectedAmount) {
        return new TypeSafeMatcher<Order>() {
            @Override
            protected boolean matchesSafely(Order order) {
                return expectedAmount.compareTo(order.getEffectiveAmount().getAmount()) == 0;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("order with effective amount ").appendValue(expectedAmount);
            }

            @Override
            protected void describeMismatchSafely(Order order, Description mismatchDescription) {
                mismatchDescription
                        .appendText("order had effective amount ")
                        .appendValue(order.getEffectiveAmount().getAmount());
            }
        };
    }

    /** 匹配訂單項目數量 */
    public static Matcher<Order> hasItemCount(int expectedCount) {
        return new TypeSafeMatcher<Order>() {
            @Override
            protected boolean matchesSafely(Order order) {
                return expectedCount == order.getItems().size();
            }

            @Override
            public void describeTo(Description description) {
                description
                        .appendText("order with ")
                        .appendValue(expectedCount)
                        .appendText(" items");
            }

            @Override
            protected void describeMismatchSafely(Order order, Description mismatchDescription) {
                mismatchDescription
                        .appendText("order had ")
                        .appendValue(order.getItems().size())
                        .appendText(" items");
            }
        };
    }

    /** 匹配訂單客戶ID */
    public static Matcher<Order> hasCustomerId(String expectedCustomerId) {
        return new TypeSafeMatcher<Order>() {
            @Override
            protected boolean matchesSafely(Order order) {
                return expectedCustomerId.equals(order.getCustomerIdAsString());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("order with customer ID ").appendValue(expectedCustomerId);
            }

            @Override
            protected void describeMismatchSafely(Order order, Description mismatchDescription) {
                mismatchDescription
                        .appendText("order had customer ID ")
                        .appendValue(order.getCustomerIdAsString());
            }
        };
    }

    /** 匹配訂單配送地址 */
    public static Matcher<Order> hasShippingAddress(String expectedAddress) {
        return new TypeSafeMatcher<Order>() {
            @Override
            protected boolean matchesSafely(Order order) {
                return expectedAddress.equals(order.getShippingAddress());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("order with shipping address ").appendValue(expectedAddress);
            }

            @Override
            protected void describeMismatchSafely(Order order, Description mismatchDescription) {
                mismatchDescription
                        .appendText("order had shipping address ")
                        .appendValue(order.getShippingAddress());
            }
        };
    }

    /** 匹配訂單折扣金額 */
    public static Matcher<Order> hasDiscountAmount(BigDecimal expectedDiscount) {
        return new TypeSafeMatcher<Order>() {
            @Override
            protected boolean matchesSafely(Order order) {
                BigDecimal actualDiscount =
                        order.getTotalAmount()
                                .getAmount()
                                .subtract(order.getEffectiveAmount().getAmount());
                return expectedDiscount.compareTo(actualDiscount) == 0;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("order with discount amount ").appendValue(expectedDiscount);
            }

            @Override
            protected void describeMismatchSafely(Order order, Description mismatchDescription) {
                BigDecimal actualDiscount =
                        order.getTotalAmount()
                                .getAmount()
                                .subtract(order.getEffectiveAmount().getAmount());
                mismatchDescription
                        .appendText("order had discount amount ")
                        .appendValue(actualDiscount);
            }
        };
    }

    /** 匹配空訂單（沒有項目） */
    public static Matcher<Order> isEmpty() {
        return hasItemCount(0);
    }

    /** 匹配非空訂單（有項目） */
    public static Matcher<Order> isNotEmpty() {
        return new TypeSafeMatcher<Order>() {
            @Override
            protected boolean matchesSafely(Order order) {
                return order.getItems().size() > 0;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("order with items");
            }

            @Override
            protected void describeMismatchSafely(Order order, Description mismatchDescription) {
                mismatchDescription.appendText("order was empty");
            }
        };
    }
}
