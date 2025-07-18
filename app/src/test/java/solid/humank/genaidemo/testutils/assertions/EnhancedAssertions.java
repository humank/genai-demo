package solid.humank.genaidemo.testutils.assertions;

import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderStatus;
import solid.humank.genaidemo.domain.order.model.aggregate.Order;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 增強的斷言工具類別
 * 提供更清晰的錯誤訊息和更具表達性的斷言
 */
public class EnhancedAssertions {
    
    /**
     * 斷言訂單狀態
     */
    public static void assertOrderStatus(Order order, OrderStatus expectedStatus) {
        assertNotNull(order, "Order should not be null");
        assertEquals(expectedStatus, order.getStatus(),
            String.format("Order %s should have status %s but was %s", 
                order.getId(), expectedStatus, order.getStatus()));
    }
    
    /**
     * 斷言訂單總金額
     */
    public static void assertOrderTotalAmount(Order order, BigDecimal expectedAmount) {
        assertNotNull(order, "Order should not be null");
        assertEquals(0, expectedAmount.compareTo(order.getTotalAmount().getAmount()),
            String.format("Order %s should have total amount %s but was %s", 
                order.getId(), expectedAmount, order.getTotalAmount().getAmount()));
    }
    
    /**
     * 斷言訂單有效金額
     */
    public static void assertOrderEffectiveAmount(Order order, BigDecimal expectedAmount) {
        assertNotNull(order, "Order should not be null");
        assertEquals(0, expectedAmount.compareTo(order.getEffectiveAmount().getAmount()),
            String.format("Order %s should have effective amount %s but was %s", 
                order.getId(), expectedAmount, order.getEffectiveAmount().getAmount()));
    }
    
    /**
     * 斷言訂單項目數量
     */
    public static void assertOrderItemCount(Order order, int expectedCount) {
        assertNotNull(order, "Order should not be null");
        assertEquals(expectedCount, order.getItems().size(),
            String.format("Order %s should have %d items but had %d", 
                order.getId(), expectedCount, order.getItems().size()));
    }
    
    /**
     * 斷言訂單客戶ID
     */
    public static void assertOrderCustomerId(Order order, String expectedCustomerId) {
        assertNotNull(order, "Order should not be null");
        assertEquals(expectedCustomerId, order.getCustomerIdAsString(),
            String.format("Order %s should have customer ID %s but was %s", 
                order.getId(), expectedCustomerId, order.getCustomerIdAsString()));
    }
    
    /**
     * 斷言訂單折扣金額
     */
    public static void assertOrderDiscountAmount(Order order, BigDecimal expectedDiscount) {
        assertNotNull(order, "Order should not be null");
        BigDecimal actualDiscount = order.getTotalAmount().getAmount()
            .subtract(order.getEffectiveAmount().getAmount());
        assertEquals(0, expectedDiscount.compareTo(actualDiscount),
            String.format("Order %s should have discount amount %s but was %s", 
                order.getId(), expectedDiscount, actualDiscount));
    }
    
    /**
     * 斷言金額值
     */
    public static void assertMoneyAmount(Money money, BigDecimal expectedAmount) {
        assertNotNull(money, "Money should not be null");
        assertEquals(0, expectedAmount.compareTo(money.getAmount()),
            String.format("Money should have amount %s but was %s", 
                expectedAmount, money.getAmount()));
    }
    
    /**
     * 斷言金額為零
     */
    public static void assertMoneyIsZero(Money money) {
        assertMoneyAmount(money, BigDecimal.ZERO);
    }
    
    /**
     * 斷言金額為正數
     */
    public static void assertMoneyIsPositive(Money money) {
        assertNotNull(money, "Money should not be null");
        assertTrue(money.getAmount().compareTo(BigDecimal.ZERO) > 0,
            String.format("Money should be positive but was %s", money.getAmount()));
    }
    
    /**
     * 斷言金額為負數
     */
    public static void assertMoneyIsNegative(Money money) {
        assertNotNull(money, "Money should not be null");
        assertTrue(money.getAmount().compareTo(BigDecimal.ZERO) < 0,
            String.format("Money should be negative but was %s", money.getAmount()));
    }
    
    /**
     * 斷言異常包含指定訊息
     */
    public static void assertExceptionContainsMessage(Exception exception, String expectedMessage) {
        assertNotNull(exception, "Exception should not be null");
        assertNotNull(exception.getMessage(), "Exception message should not be null");
        assertTrue(exception.getMessage().contains(expectedMessage),
            String.format("Exception message should contain '%s' but was '%s'", 
                expectedMessage, exception.getMessage()));
    }
    
    /**
     * 斷言異常類型和訊息
     */
    public static void assertExceptionTypeAndMessage(Exception exception, 
                                                   Class<? extends Exception> expectedType, 
                                                   String expectedMessage) {
        assertNotNull(exception, "Exception should not be null");
        assertTrue(expectedType.isInstance(exception),
            String.format("Exception should be of type %s but was %s", 
                expectedType.getSimpleName(), exception.getClass().getSimpleName()));
        assertExceptionContainsMessage(exception, expectedMessage);
    }
    
    /**
     * 斷言集合不為空且包含指定數量的元素
     */
    public static <T> void assertCollectionSizeAndNotEmpty(java.util.Collection<T> collection, int expectedSize) {
        assertNotNull(collection, "Collection should not be null");
        assertFalse(collection.isEmpty(), "Collection should not be empty");
        assertEquals(expectedSize, collection.size(),
            String.format("Collection should have size %d but was %d", expectedSize, collection.size()));
    }
    
    /**
     * 斷言字串不為空且包含指定內容
     */
    public static void assertStringNotEmptyAndContains(String actual, String expectedContent) {
        assertNotNull(actual, "String should not be null");
        assertFalse(actual.trim().isEmpty(), "String should not be empty");
        assertTrue(actual.contains(expectedContent),
            String.format("String should contain '%s' but was '%s'", expectedContent, actual));
    }
}