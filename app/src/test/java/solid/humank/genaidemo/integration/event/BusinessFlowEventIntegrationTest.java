package solid.humank.genaidemo.integration.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import solid.humank.genaidemo.application.order.dto.AddOrderItemCommand;
import solid.humank.genaidemo.application.order.dto.CreateOrderCommand;
import solid.humank.genaidemo.application.order.dto.response.OrderResponse;
import solid.humank.genaidemo.application.order.service.OrderApplicationService;
import solid.humank.genaidemo.domain.order.model.events.OrderCreatedEvent;
import solid.humank.genaidemo.domain.order.model.events.OrderItemAddedEvent;
import solid.humank.genaidemo.domain.order.model.events.OrderSubmittedEvent;
import solid.humank.genaidemo.domain.order.service.OrderEventHandler;
import solid.humank.genaidemo.testutils.annotations.IntegrationTest;
import solid.humank.genaidemo.testutils.builders.OrderTestDataBuilder;
import solid.humank.genaidemo.testutils.fixtures.TestConstants;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * 業務流程事件整合測試
 * 重構後遵循3A原則，每個測試方法只測試一個特定場景
 */
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@IntegrationTest
public class BusinessFlowEventIntegrationTest {

    @Autowired
    private OrderApplicationService orderApplicationService;
    
    @Mock
    private OrderEventHandler orderEventHandler;
    
    @Test
    @DisplayName("應該在訂單創建時發布OrderCreatedEvent")
    public void shouldPublishOrderCreatedEventWhenOrderIsCreated() {
        // Arrange
        CreateOrderCommand createCommand = OrderTestDataBuilder.anOrder()
            .withCustomerId("customer-flow-test")
            .withShippingAddress(TestConstants.Order.DEFAULT_SHIPPING_ADDRESS)
            .buildCreateCommand();
        
        // Act
        OrderResponse orderResponse = orderApplicationService.createOrder(createCommand);
        
        // Assert
        assertNotNull(orderResponse.getId(), "Order ID should not be null");
        verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
    }
    
    @Test
    @DisplayName("應該在添加訂單項目時發布OrderItemAddedEvent")
    public void shouldPublishOrderItemAddedEventWhenItemIsAdded() {
        // Arrange
        String orderId = createTestOrderAndGetId();
        AddOrderItemCommand addItemCommand = OrderTestDataBuilder.anOrder()
            .withItem("product-123", "iPhone 15", 1, TestConstants.Product.DEFAULT_PRICE)
            .buildAddItemCommand(orderId);
        
        // Act
        orderApplicationService.addOrderItem(addItemCommand);
        
        // Assert
        verify(orderEventHandler, timeout(1000)).handleOrderItemAdded(any(OrderItemAddedEvent.class));
    }
    
    @Test
    @DisplayName("應該在提交訂單時發布OrderSubmittedEvent")
    public void shouldPublishOrderSubmittedEventWhenOrderIsSubmitted() {
        // Arrange
        String orderId = createTestOrderWithItemAndGetId();
        
        // Act
        orderApplicationService.submitOrder(orderId);
        
        // Assert
        verify(orderEventHandler, timeout(1000)).handleOrderSubmitted(any(OrderSubmittedEvent.class));
    }
    
    @Test
    @DisplayName("應該在完整訂單流程中正確處理所有事件並更新訂單狀態")
    public void shouldHandleAllEventsAndUpdateOrderStateInCompleteFlow() {
        // Arrange
        CreateOrderCommand createCommand = OrderTestDataBuilder.anOrder()
            .withCustomerId("customer-state-test")
            .withShippingAddress(TestConstants.Order.ALTERNATIVE_ADDRESS)
            .buildCreateCommand();
        
        // Act - 創建訂單
        OrderResponse orderResponse = orderApplicationService.createOrder(createCommand);
        String orderId = orderResponse.getId();
        
        // Act - 添加產品
        AddOrderItemCommand addItemCommand = AddOrderItemCommand.of(
            orderId, "product-456", "MacBook Pro", 1, new BigDecimal("58000")
        );
        orderApplicationService.addOrderItem(addItemCommand);
        
        // Act - 提交訂單
        orderApplicationService.submitOrder(orderId);
        
        // Assert - 驗證所有事件都被處理
        verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
        verify(orderEventHandler, timeout(1000)).handleOrderItemAdded(any(OrderItemAddedEvent.class));
        verify(orderEventHandler, timeout(1000)).handleOrderSubmitted(any(OrderSubmittedEvent.class));
        
        // Assert - 驗證最終訂單狀態
        OrderResponse finalOrder = orderApplicationService.getOrder(orderId);
        assertNotNull(finalOrder, "Final order should not be null");
        assertEquals("PENDING", finalOrder.getStatus());
        assertEquals(0, BigDecimal.valueOf(58000).compareTo(finalOrder.getTotalAmount()));
        assertEquals(1, finalOrder.getItems().size());
    }
    
    @Test
    @DisplayName("應該在異常情況下正確處理事件並保持訂單狀態")
    public void shouldHandleEventsCorrectlyWhenExceptionOccurs() {
        // Arrange
        CreateOrderCommand createCommand = OrderTestDataBuilder.anOrder()
            .withCustomerId("customer-exception-test")
            .withShippingAddress(TestConstants.Order.THIRD_ADDRESS)
            .buildCreateCommand();
        
        // Act - 創建訂單
        OrderResponse orderResponse = orderApplicationService.createOrder(createCommand);
        String orderId = orderResponse.getId();
        
        // Assert - 驗證訂單創建事件被處理
        verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
        
        // Act & Assert - 嘗試提交沒有項目的訂單應該拋出異常
        Exception exception = assertThrows(Exception.class, () -> {
            orderApplicationService.submitOrder(orderId);
        }, "Should throw exception when submitting order with no items");
        
        assertTrue(exception.getMessage().contains(TestConstants.ErrorMessages.ORDER_NO_ITEMS),
            "Exception message should contain expected error message");
        
        // Assert - 驗證訂單狀態沒有變化
        OrderResponse finalOrder = orderApplicationService.getOrder(orderId);
        assertNotNull(finalOrder, "Order should still exist after exception");
        assertEquals("CREATED", finalOrder.getStatus(), "Order status should remain CREATED");
    }
    
    // 輔助方法
    
    private String createTestOrderAndGetId() {
        CreateOrderCommand createCommand = OrderTestDataBuilder.anOrder()
            .withCustomerId(TestConstants.Customer.DEFAULT_ID)
            .withShippingAddress(TestConstants.Order.DEFAULT_SHIPPING_ADDRESS)
            .buildCreateCommand();
        
        OrderResponse orderResponse = orderApplicationService.createOrder(createCommand);
        return orderResponse.getId();
    }
    
    private String createTestOrderWithItemAndGetId() {
        String orderId = createTestOrderAndGetId();
        
        AddOrderItemCommand addItemCommand = OrderTestDataBuilder.anOrder()
            .withDefaultItem()
            .buildAddItemCommand(orderId);
        
        orderApplicationService.addOrderItem(addItemCommand);
        return orderId;
    }
}