package solid.humank.genaidemo.examples.order;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import solid.humank.genaidemo.ddd.events.DomainEventPublisherService;
import solid.humank.genaidemo.examples.order.model.aggregate.Order;
import solid.humank.genaidemo.examples.order.model.events.OrderCreatedEvent;
import solid.humank.genaidemo.examples.order.model.events.OrderItemAddedEvent;
import solid.humank.genaidemo.examples.order.model.valueobject.Money;
import solid.humank.genaidemo.examples.order.model.valueobject.OrderItem;
import solid.humank.genaidemo.examples.order.model.valueobject.OrderStatus;

/**
 * Order 聚合根的單元測試
 * 測試所有從 OrderController 識別出的用例
 */
class OrderTest {

    @Mock
    private DomainEventPublisherService publisherService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Nested
    @DisplayName("建立訂單測試")
    class OrderCreationTests {
        @Test
        @DisplayName("應該成功建立訂單並設置正確的初始狀態")
        void testOrderCreation() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                String customerId = "customer-123";
                String shippingAddress = "台北市信義區";
                
                // 執行
                Order order = new Order(customerId, shippingAddress);
                
                // 驗證
                assertEquals(customerId, order.getCustomerId());
                assertEquals(shippingAddress, order.getShippingAddress());
                assertEquals(OrderStatus.CREATED, order.getStatus());
                assertTrue(order.getItems().isEmpty());
                
                // 驗證事件發布
                mockedStatic.verify(() -> 
                    DomainEventPublisherService.publishEvent(any(OrderCreatedEvent.class))
                );
            }
        }
        
        @Test
        @DisplayName("使用訂單ID建立訂單")
        void testOrderCreationWithOrderId() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                String orderId = "550e8400-e29b-41d4-a716-446655440000";
                
                // 執行
                Order order = new Order(orderId);
                
                // 驗證
                assertEquals(orderId, order.getId().toString());
                assertEquals(OrderStatus.CREATED, order.getStatus());
                assertTrue(order.getItems().isEmpty());
                
                // 驗證事件發布
                mockedStatic.verify(() -> 
                    DomainEventPublisherService.publishEvent(any(OrderCreatedEvent.class))
                );
            }
        }
    }
    
    @Nested
    @DisplayName("添加訂單項測試")
    class AddOrderItemTests {
        @Test
        @DisplayName("應該成功添加訂單項")
        void testAddItem() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                String productId = "product-456";
                String productName = "iPhone 15";
                int quantity = 2;
                Money price = Money.twd(30000);
                
                // 執行
                order.addItem(productId, productName, quantity, price);
                
                // 驗證
                assertEquals(1, order.getItems().size());
                OrderItem item = order.getItems().get(0);
                assertEquals(productId, item.getProductId());
                assertEquals(productName, item.getProductName());
                assertEquals(quantity, item.getQuantity());
                assertEquals(price, item.getPrice());
                
                // 驗證事件發布
                mockedStatic.verify(() -> 
                    DomainEventPublisherService.publishEvent(any(OrderItemAddedEvent.class))
                );
            }
        }
        
        @Test
        @DisplayName("添加多個訂單項應正確計算總金額")
        void testAddMultipleItems() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                
                // 執行
                order.addItem("product-1", "iPhone 15", 1, Money.twd(30000));
                order.addItem("product-2", "保護殼", 2, Money.twd(1000));
                order.addItem("product-3", "充電器", 1, Money.twd(1500));
                
                // 驗證
                assertEquals(3, order.getItems().size());
                Money expectedTotal = Money.twd(33500); // 30000 + (1000 * 2) + 1500
                assertEquals(expectedTotal, order.getTotalAmount());
            }
        }
        
        @Test
        @DisplayName("非CREATED狀態的訂單不能添加項目")
        void testCannotAddItemToNonCreatedOrder() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                order.addItem("product-1", "iPhone 15", 1, Money.twd(30000));
                order.submit(); // 狀態變為PENDING
                
                // 執行與驗證
                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
                    order.addItem("product-2", "保護殼", 1, Money.twd(1000));
                });
                
                assertEquals("Cannot add items to an order that is not in CREATED state", exception.getMessage());
            }
        }
    }
    
    @Nested
    @DisplayName("訂單狀態轉換測試")
    class OrderStateTransitionTests {
        @Test
        @DisplayName("訂單應該按照正確的生命週期狀態轉換")
        void testOrderLifecycle() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                order.addItem("product-456", "iPhone 15", 2, Money.twd(30000));
                
                // 執行和驗證
                order.submit();
                assertEquals(OrderStatus.PENDING, order.getStatus());
                
                order.confirm();
                assertEquals(OrderStatus.CONFIRMED, order.getStatus());
                
                order.markAsPaid();
                assertEquals(OrderStatus.PAID, order.getStatus());
                
                order.ship();
                assertEquals(OrderStatus.SHIPPING, order.getStatus());
                
                order.deliver();
                assertEquals(OrderStatus.DELIVERED, order.getStatus());
            }
        }
        
        @Test
        @DisplayName("空訂單不能提交")
        void testCannotSubmitEmptyOrder() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                
                // 執行與驗證
                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
                    order.submit();
                });
                
                assertEquals("Cannot submit an order with no items", exception.getMessage());
            }
        }
        
        @Test
        @DisplayName("已送達的訂單不能取消")
        void testCannotCancelDeliveredOrder() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                order.addItem("product-456", "iPhone 15", 2, Money.twd(30000));
                order.submit();
                order.confirm();
                order.markAsPaid();
                order.ship();
                order.deliver();
                
                // 執行與驗證
                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
                    order.cancel();
                });
                
                assertEquals("Cannot cancel an order that is already delivered or cancelled", exception.getMessage());
            }
        }
    }
    
    @Nested
    @DisplayName("取消訂單測試")
    class CancelOrderTests {
        @Test
        @DisplayName("應該成功取消處於PENDING狀態的訂單")
        void testCancelOrder() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                order.addItem("product-456", "iPhone 15", 2, Money.twd(30000));
                order.submit();
                
                // 執行
                order.cancel();
                
                // 驗證
                assertEquals(OrderStatus.CANCELLED, order.getStatus());
            }
        }
        
        @Test
        @DisplayName("應該成功取消處於CONFIRMED狀態的訂單")
        void testCancelConfirmedOrder() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                order.addItem("product-456", "iPhone 15", 2, Money.twd(30000));
                order.submit();
                order.confirm();
                
                // 執行
                order.cancel();
                
                // 驗證
                assertEquals(OrderStatus.CANCELLED, order.getStatus());
            }
        }
        
        @Test
        @DisplayName("已取消的訂單不能再次取消")
        void testCannotCancelCancelledOrder() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                order.addItem("product-456", "iPhone 15", 2, Money.twd(30000));
                order.submit();
                order.cancel();
                
                // 執行與驗證
                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
                    order.cancel();
                });
                
                assertEquals("Cannot cancel an order that is already delivered or cancelled", exception.getMessage());
            }
        }
    }
    
    @Nested
    @DisplayName("訂單金額計算測試")
    class OrderAmountCalculationTests {
        @Test
        @DisplayName("應該正確計算訂單總金額")
        void testGetTotalAmount() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                Money price1 = Money.twd(30000);
                Money price2 = Money.twd(2000);
                
                // 執行
                order.addItem("product-1", "iPhone 15", 1, price1);
                order.addItem("product-2", "保護殼", 1, price2);
                
                // 驗證
                Money expectedTotal = Money.twd(32000);
                assertEquals(expectedTotal, order.getTotalAmount());
            }
        }
        
        @Test
        @DisplayName("應該正確計算多數量訂單項的總金額")
        void testGetTotalAmountWithMultipleQuantities() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                
                // 執行
                order.addItem("product-1", "iPhone 15", 2, Money.twd(30000)); // 60000
                order.addItem("product-2", "保護殼", 3, Money.twd(1000));     // 3000
                
                // 驗證
                Money expectedTotal = Money.twd(63000);
                assertEquals(expectedTotal, order.getTotalAmount());
            }
        }
    }
    
    @Nested
    @DisplayName("訂單處理測試")
    class OrderProcessingTests {
        @Test
        @DisplayName("處理訂單應將狀態從CREATED變為PENDING")
        void testProcessOrder() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                order.addItem("product-456", "iPhone 15", 2, Money.twd(30000));
                
                // 執行
                order.process();
                
                // 驗證
                assertEquals(OrderStatus.PENDING, order.getStatus());
            }
        }
        
        @Test
        @DisplayName("非CREATED狀態的訂單不能處理")
        void testCannotProcessNonCreatedOrder() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                order.addItem("product-456", "iPhone 15", 2, Money.twd(30000));
                order.submit(); // 狀態變為PENDING
                
                // 執行與驗證
                IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
                    order.process();
                });
                
                assertEquals("只有處於建立狀態的訂單可以處理", exception.getMessage());
            }
        }
    }
    
    @Nested
    @DisplayName("折扣應用測試")
    class DiscountApplicationTests {
        @Test
        @DisplayName("應該正確應用折扣到訂單")
        void testApplyDiscount() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                order.addItem("product-1", "iPhone 15", 1, Money.twd(30000));
                Money discountAmount = Money.twd(3000);
                
                // 執行
                order.applyDiscount(discountAmount);
                
                // 驗證
                Money expectedEffectiveAmount = Money.twd(27000); // 30000 - 3000
                assertEquals(expectedEffectiveAmount, order.getEffectiveAmount());
                assertEquals(Money.twd(30000), order.getTotalAmount()); // 原始總金額不變
            }
        }
        
        @Test
        @DisplayName("折扣金額不能大於訂單總金額")
        void testCannotApplyDiscountGreaterThanTotal() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                order.addItem("product-1", "iPhone 15", 1, Money.twd(30000));
                Money discountAmount = Money.twd(35000); // 大於總金額
                
                // 執行與驗證
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                    order.applyDiscount(discountAmount);
                });
                
                assertEquals("折扣金額不能大於訂單總金額", exception.getMessage());
            }
        }
        
        @Test
        @DisplayName("未應用折扣時有效金額等於總金額")
        void testEffectiveAmountEqualsToTotalWhenNoDiscount() {
            // 使用 MockedStatic 模擬靜態方法
            try (MockedStatic<DomainEventPublisherService> mockedStatic = mockStatic(DomainEventPublisherService.class)) {
                // 準備
                Order order = new Order("customer-123", "台北市信義區");
                order.addItem("product-1", "iPhone 15", 1, Money.twd(30000));
                
                // 驗證
                assertEquals(order.getTotalAmount(), order.getEffectiveAmount());
            }
        }
    }
}