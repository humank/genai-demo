package solid.humank.genaidemo.examples.order;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * OrderId 值對象的單元測試
 */
class OrderIdTest {

    @Nested
    @DisplayName("建立訂單ID測試")
    class OrderIdCreationTests {
        @Test
        @DisplayName("應該成功使用UUID建立訂單ID")
        void testOrderIdCreationWithUUID() {
            // 準備
            UUID uuid = UUID.randomUUID();
            
            // 執行
            OrderId orderId = new OrderId(uuid);
            
            // 驗證
            assertEquals(uuid, orderId.getValue());
        }
        
        @Test
        @DisplayName("應該成功使用字符串建立訂單ID")
        void testOrderIdCreationWithString() {
            // 準備
            String uuidString = "550e8400-e29b-41d4-a716-446655440000";
            
            // 執行
            OrderId orderId = new OrderId(uuidString);
            
            // 驗證
            assertEquals(UUID.fromString(uuidString), orderId.getValue());
        }
        
        @Test
        @DisplayName("UUID不能為null")
        void testUUIDCannotBeNull() {
            // 執行與驗證
            NullPointerException exception = assertThrows(NullPointerException.class, () -> {
                new OrderId((UUID) null);
            });
            
            assertEquals("Order ID cannot be null", exception.getMessage());
        }
        
        @Test
        @DisplayName("無效的UUID字符串應拋出異常")
        void testInvalidUUIDString() {
            // 準備
            String invalidUuid = "not-a-uuid";
            
            // 執行與驗證
            assertThrows(IllegalArgumentException.class, () -> {
                new OrderId(invalidUuid);
            });
        }
        
        @Test
        @DisplayName("應該成功生成新的訂單ID")
        void testGenerateOrderId() {
            // 執行
            OrderId orderId = OrderId.generate();
            
            // 驗證
            assertNotNull(orderId);
            assertNotNull(orderId.getValue());
        }
        
        @Test
        @DisplayName("應該成功使用of方法建立訂單ID")
        void testOrderIdOf() {
            // 準備
            String uuidString = "550e8400-e29b-41d4-a716-446655440000";
            
            // 執行
            OrderId orderId = OrderId.of(uuidString);
            
            // 驗證
            assertEquals(UUID.fromString(uuidString), orderId.getValue());
        }
    }
    
    @Nested
    @DisplayName("訂單ID相等性測試")
    class OrderIdEqualityTests {
        @Test
        @DisplayName("相同UUID的訂單ID應該相等")
        void testOrderIdEquality() {
            // 準備
            String uuidString = "550e8400-e29b-41d4-a716-446655440000";
            OrderId orderId1 = new OrderId(uuidString);
            OrderId orderId2 = new OrderId(uuidString);
            
            // 驗證
            assertEquals(orderId1, orderId2);
            assertEquals(orderId1.hashCode(), orderId2.hashCode());
        }
        
        @Test
        @DisplayName("不同UUID的訂單ID不應該相等")
        void testOrderIdInequality() {
            // 準備
            OrderId orderId1 = new OrderId("550e8400-e29b-41d4-a716-446655440000");
            OrderId orderId2 = new OrderId("650e8400-e29b-41d4-a716-446655440000");
            
            // 驗證
            assertNotEquals(orderId1, orderId2);
        }
        
        @Test
        @DisplayName("訂單ID不應該等於null或其他類型")
        void testOrderIdNotEqualToNullOrOtherTypes() {
            // 準備
            OrderId orderId = new OrderId("550e8400-e29b-41d4-a716-446655440000");
            
            // 驗證
            assertNotEquals(orderId, null);
            assertNotEquals(orderId, "Not an OrderId");
        }
    }
    
    @Test
    @DisplayName("訂單ID的toString方法應該返回UUID字符串")
    void testToString() {
        // 準備
        String uuidString = "550e8400-e29b-41d4-a716-446655440000";
        OrderId orderId = new OrderId(uuidString);
        
        // 執行
        String result = orderId.toString();
        
        // 驗證
        assertEquals(uuidString, result);
    }
}