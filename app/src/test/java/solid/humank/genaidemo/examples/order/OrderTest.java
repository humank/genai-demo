package solid.humank.genaidemo.examples.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import solid.humank.genaidemo.ddd.events.DomainEventPublisherService;
import solid.humank.genaidemo.examples.order.events.OrderCreatedEvent;
import solid.humank.genaidemo.examples.order.events.OrderItemAddedEvent;

class OrderTest {

    @Mock
    private DomainEventPublisherService publisherService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
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
}