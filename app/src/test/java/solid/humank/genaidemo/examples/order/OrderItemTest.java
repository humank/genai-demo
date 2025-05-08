package solid.humank.genaidemo.examples.order;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * OrderItem 值對象的單元測試
 */
class OrderItemTest {

    @Nested
    @DisplayName("建立訂單項測試")
    class OrderItemCreationTests {
        @Test
        @DisplayName("應該成功建立訂單項")
        void testOrderItemCreation() {
            // 準備
            String productId = "product-123";
            String productName = "iPhone 15";
            int quantity = 2;
            Money price = Money.twd(30000);
            
            // 執行
            OrderItem orderItem = new OrderItem(productId, productName, quantity, price);
            
            // 驗證
            assertEquals(productId, orderItem.getProductId());
            assertEquals(productName, orderItem.getProductName());
            assertEquals(quantity, orderItem.getQuantity());
            assertEquals(price, orderItem.getPrice());
        }
        
        @Test
        @DisplayName("數量必須為正數")
        void testQuantityMustBePositive() {
            // 準備
            String productId = "product-123";
            String productName = "iPhone 15";
            int quantity = 0;
            Money price = Money.twd(30000);
            
            // 執行與驗證
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                new OrderItem(productId, productName, quantity, price);
            });
            
            assertEquals("Quantity must be positive", exception.getMessage());
        }
        
        @Test
        @DisplayName("產品ID不能為null")
        void testProductIdCannotBeNull() {
            // 準備
            String productId = null;
            String productName = "iPhone 15";
            int quantity = 1;
            Money price = Money.twd(30000);
            
            // 執行與驗證
            NullPointerException exception = assertThrows(NullPointerException.class, () -> {
                new OrderItem(productId, productName, quantity, price);
            });
            
            assertEquals("Product ID cannot be null", exception.getMessage());
        }
        
        @Test
        @DisplayName("產品名稱不能為null")
        void testProductNameCannotBeNull() {
            // 準備
            String productId = "product-123";
            String productName = null;
            int quantity = 1;
            Money price = Money.twd(30000);
            
            // 執行與驗證
            NullPointerException exception = assertThrows(NullPointerException.class, () -> {
                new OrderItem(productId, productName, quantity, price);
            });
            
            assertEquals("Product name cannot be null", exception.getMessage());
        }
        
        @Test
        @DisplayName("價格不能為null")
        void testPriceCannotBeNull() {
            // 準備
            String productId = "product-123";
            String productName = "iPhone 15";
            int quantity = 1;
            Money price = null;
            
            // 執行與驗證
            NullPointerException exception = assertThrows(NullPointerException.class, () -> {
                new OrderItem(productId, productName, quantity, price);
            });
            
            assertEquals("Price cannot be null", exception.getMessage());
        }
    }
    
    @Nested
    @DisplayName("訂單項金額計算測試")
    class OrderItemAmountCalculationTests {
        @Test
        @DisplayName("應該正確計算訂單項小計金額")
        void testGetSubtotal() {
            // 準備
            String productId = "product-123";
            String productName = "iPhone 15";
            int quantity = 2;
            Money price = Money.twd(30000);
            OrderItem orderItem = new OrderItem(productId, productName, quantity, price);
            
            // 執行
            Money subtotal = orderItem.getSubtotal();
            
            // 驗證
            Money expectedSubtotal = Money.twd(60000); // 30000 * 2
            assertEquals(expectedSubtotal, subtotal);
        }
        
        @Test
        @DisplayName("單一數量的訂單項小計應等於價格")
        void testGetSubtotalWithSingleQuantity() {
            // 準備
            String productId = "product-123";
            String productName = "iPhone 15";
            int quantity = 1;
            Money price = Money.twd(30000);
            OrderItem orderItem = new OrderItem(productId, productName, quantity, price);
            
            // 執行
            Money subtotal = orderItem.getSubtotal();
            
            // 驗證
            assertEquals(price, subtotal);
        }
    }
    
    @Nested
    @DisplayName("訂單項相等性測試")
    class OrderItemEqualityTests {
        @Test
        @DisplayName("相同屬性的訂單項應該相等")
        void testOrderItemEquality() {
            // 準備
            OrderItem item1 = new OrderItem("product-123", "iPhone 15", 2, Money.twd(30000));
            OrderItem item2 = new OrderItem("product-123", "iPhone 15", 2, Money.twd(30000));
            
            // 驗證
            assertEquals(item1, item2);
            assertEquals(item1.hashCode(), item2.hashCode());
        }
        
        @Test
        @DisplayName("不同屬性的訂單項不應該相等")
        void testOrderItemInequality() {
            // 準備
            OrderItem item1 = new OrderItem("product-123", "iPhone 15", 2, Money.twd(30000));
            OrderItem item2 = new OrderItem("product-456", "iPhone 15", 2, Money.twd(30000));
            OrderItem item3 = new OrderItem("product-123", "iPhone 14", 2, Money.twd(30000));
            OrderItem item4 = new OrderItem("product-123", "iPhone 15", 3, Money.twd(30000));
            OrderItem item5 = new OrderItem("product-123", "iPhone 15", 2, Money.twd(25000));
            
            // 驗證
            assertNotEquals(item1, item2); // 不同產品ID
            assertNotEquals(item1, item3); // 不同產品名稱
            assertNotEquals(item1, item4); // 不同數量
            assertNotEquals(item1, item5); // 不同價格
        }
        
        @Test
        @DisplayName("訂單項不應該等於null或其他類型")
        void testOrderItemNotEqualToNullOrOtherTypes() {
            // 準備
            OrderItem item = new OrderItem("product-123", "iPhone 15", 2, Money.twd(30000));
            
            // 驗證
            assertNotEquals(item, null);
            assertNotEquals(item, "Not an OrderItem");
        }
    }
    
    @Test
    @DisplayName("訂單項的toString方法應該返回有意義的字符串表示")
    void testToString() {
        // 準備
        OrderItem item = new OrderItem("product-123", "iPhone 15", 2, Money.twd(30000));
        
        // 執行
        String result = item.toString();
        
        // 驗證
        assertTrue(result.contains("product-123"));
        assertTrue(result.contains("iPhone 15"));
        assertTrue(result.contains("2"));
        assertTrue(result.contains("30000"));
    }
}