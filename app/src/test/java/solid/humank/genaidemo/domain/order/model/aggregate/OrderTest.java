package solid.humank.genaidemo.domain.order.model.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 訂單聚合根單元測試
 * 使用 JUnit 5 測試框架
 */
class OrderTest {

    /**
     * 測試建立訂單時應設置正確的初始狀態
     */
    @Test
    @DisplayName("建立訂單時若客戶ID為空應拋出異常")
    void shouldThrowExceptionWhenCustomerIdIsEmpty() {
        // Arrange
        String customerId = "";
        String shippingAddress = "台北市信義區";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Order(customerId, shippingAddress)
        );
        assertTrue(exception.getMessage().contains("客戶ID不能為空"));
    }

    /**
     * 測試建立訂單時若配送地址為空應拋出異常
     */
    @Test
    @DisplayName("建立訂單時若配送地址為空應拋出異常")
    void shouldThrowExceptionWhenShippingAddressIsEmpty() {
        // Arrange
        String customerId = "customer-123";
        String shippingAddress = "";

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new Order(customerId, shippingAddress)
        );
        assertTrue(exception.getMessage().contains("配送地址不能為空"));
    }
}