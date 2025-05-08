package solid.humank.genaidemo.examples.order;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * OrderStatus 枚舉的單元測試
 */
class OrderStatusTest {

    @Test
    @DisplayName("訂單狀態應該有正確的描述")
    void testOrderStatusDescription() {
        assertEquals("已建立", OrderStatus.CREATED.getDescription());
        assertEquals("處理中", OrderStatus.PENDING.getDescription());
        assertEquals("已確認", OrderStatus.CONFIRMED.getDescription());
        assertEquals("已付款", OrderStatus.PAID.getDescription());
        assertEquals("配送中", OrderStatus.SHIPPING.getDescription());
        assertEquals("已送達", OrderStatus.DELIVERED.getDescription());
        assertEquals("已取消", OrderStatus.CANCELLED.getDescription());
    }
    
    @ParameterizedTest
    @DisplayName("CREATED狀態只能轉換到PENDING或CANCELLED")
    @CsvSource({
        "PENDING, true",
        "CANCELLED, true",
        "CONFIRMED, false",
        "PAID, false",
        "SHIPPING, false",
        "DELIVERED, false"
    })
    void testCreatedStateTransitions(OrderStatus nextStatus, boolean canTransition) {
        assertEquals(canTransition, OrderStatus.CREATED.canTransitionTo(nextStatus));
    }
    
    @ParameterizedTest
    @DisplayName("PENDING狀態只能轉換到CONFIRMED或CANCELLED")
    @CsvSource({
        "CONFIRMED, true",
        "CANCELLED, true",
        "CREATED, false",
        "PAID, false",
        "SHIPPING, false",
        "DELIVERED, false"
    })
    void testPendingStateTransitions(OrderStatus nextStatus, boolean canTransition) {
        assertEquals(canTransition, OrderStatus.PENDING.canTransitionTo(nextStatus));
    }
    
    @ParameterizedTest
    @DisplayName("CONFIRMED狀態只能轉換到PAID或CANCELLED")
    @CsvSource({
        "PAID, true",
        "CANCELLED, true",
        "CREATED, false",
        "PENDING, false",
        "SHIPPING, false",
        "DELIVERED, false"
    })
    void testConfirmedStateTransitions(OrderStatus nextStatus, boolean canTransition) {
        assertEquals(canTransition, OrderStatus.CONFIRMED.canTransitionTo(nextStatus));
    }
    
    @ParameterizedTest
    @DisplayName("PAID狀態只能轉換到SHIPPING或CANCELLED")
    @CsvSource({
        "SHIPPING, true",
        "CANCELLED, true",
        "CREATED, false",
        "PENDING, false",
        "CONFIRMED, false",
        "DELIVERED, false"
    })
    void testPaidStateTransitions(OrderStatus nextStatus, boolean canTransition) {
        assertEquals(canTransition, OrderStatus.PAID.canTransitionTo(nextStatus));
    }
    
    @ParameterizedTest
    @DisplayName("SHIPPING狀態只能轉換到DELIVERED或CANCELLED")
    @CsvSource({
        "DELIVERED, true",
        "CANCELLED, true",
        "CREATED, false",
        "PENDING, false",
        "CONFIRMED, false",
        "PAID, false"
    })
    void testShippingStateTransitions(OrderStatus nextStatus, boolean canTransition) {
        assertEquals(canTransition, OrderStatus.SHIPPING.canTransitionTo(nextStatus));
    }
    
    @ParameterizedTest
    @DisplayName("DELIVERED狀態不能轉換到任何狀態")
    @CsvSource({
        "CREATED, false",
        "PENDING, false",
        "CONFIRMED, false",
        "PAID, false",
        "SHIPPING, false",
        "CANCELLED, false",
        "DELIVERED, false"
    })
    void testDeliveredStateTransitions(OrderStatus nextStatus, boolean canTransition) {
        assertEquals(canTransition, OrderStatus.DELIVERED.canTransitionTo(nextStatus));
    }
    
    @ParameterizedTest
    @DisplayName("CANCELLED狀態不能轉換到任何狀態")
    @CsvSource({
        "CREATED, false",
        "PENDING, false",
        "CONFIRMED, false",
        "PAID, false",
        "SHIPPING, false",
        "DELIVERED, false",
        "CANCELLED, false"
    })
    void testCancelledStateTransitions(OrderStatus nextStatus, boolean canTransition) {
        assertEquals(canTransition, OrderStatus.CANCELLED.canTransitionTo(nextStatus));
    }
}