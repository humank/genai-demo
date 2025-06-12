package solid.humank.genaidemo.integration.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import solid.humank.genaidemo.application.order.dto.AddOrderItemCommand;
import solid.humank.genaidemo.application.order.dto.CreateOrderCommand;
import solid.humank.genaidemo.application.order.dto.response.OrderResponse;
import solid.humank.genaidemo.application.order.service.OrderApplicationService;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.order.model.events.OrderCreatedEvent;
import solid.humank.genaidemo.domain.order.model.events.OrderItemAddedEvent;
import solid.humank.genaidemo.domain.order.model.events.OrderSubmittedEvent;
import solid.humank.genaidemo.domain.order.service.OrderEventHandler;
import solid.humank.genaidemo.infrastructure.event.DomainEventPublisherAdapter;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * 業務流程事件整合測試
 * 測試業務操作觸發的事件流
 */
@SpringBootTest
public class BusinessFlowEventIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private OrderApplicationService orderApplicationService;
    
    @SpyBean
    private OrderEventHandler orderEventHandler;
    
    /**
     * 測試訂單創建流程中的事件發布和處理
     */
    @Test
    public void testOrderCreationEventFlow() {
        // 執行業務操作：創建訂單
        CreateOrderCommand createCommand = new CreateOrderCommand("customer-flow-test", "台北市信義區");
        OrderResponse orderResponse = orderApplicationService.createOrder(createCommand);
        String orderId = orderResponse.getId();
        
        // 驗證訂單創建事件被處理
        verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
        
        // 執行業務操作：添加訂單項
        AddOrderItemCommand addItemCommand = AddOrderItemCommand.of(
            orderId, "product-123", "iPhone 15", 1, new BigDecimal("35000")
        );
        orderApplicationService.addOrderItem(addItemCommand);
        
        // 驗證訂單項添加事件被處理
        verify(orderEventHandler, timeout(1000)).handleOrderItemAdded(any(OrderItemAddedEvent.class));
        
        // 執行業務操作：提交訂單
        orderApplicationService.submitOrder(orderId);
        
        // 驗證訂單提交事件被處理
        verify(orderEventHandler, timeout(1000)).handleOrderSubmitted(any(OrderSubmittedEvent.class));
    }
    
    /**
     * 測試訂單狀態變更是否正確反映事件處理結果
     */
    @Test
    public void testOrderStateChangeAfterEventHandling() {
        // 執行業務操作：創建訂單
        CreateOrderCommand createCommand = new CreateOrderCommand("customer-state-test", "台北市大安區");
        OrderResponse orderResponse = orderApplicationService.createOrder(createCommand);
        String orderId = orderResponse.getId();
        
        // 添加產品
        AddOrderItemCommand addItemCommand = AddOrderItemCommand.of(
            orderId, "product-456", "MacBook Pro", 1, new BigDecimal("58000")
        );
        orderApplicationService.addOrderItem(addItemCommand);
        
        // 提交訂單
        OrderResponse submittedOrder = orderApplicationService.submitOrder(orderId);
        
        // 驗證所有事件都被處理
        verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
        verify(orderEventHandler, timeout(1000)).handleOrderItemAdded(any(OrderItemAddedEvent.class));
        verify(orderEventHandler, timeout(1000)).handleOrderSubmitted(any(OrderSubmittedEvent.class));
        
        // 獲取訂單並驗證狀態
        OrderResponse order = orderApplicationService.getOrder(orderId);
        assertNotNull(order);
        assertEquals("PENDING", order.getStatus());
        assertEquals(0, BigDecimal.valueOf(58000).compareTo(order.getTotalAmount()));
        assertEquals(1, order.getItems().size());
    }
    
    /**
     * 測試異常情況下的事件處理
     */
    @Test
    public void testEventHandlingWithExceptions() {
        // 創建訂單
        CreateOrderCommand createCommand = new CreateOrderCommand("customer-exception-test", "台北市中正區");
        OrderResponse orderResponse = orderApplicationService.createOrder(createCommand);
        String orderId = orderResponse.getId();
        
        // 驗證訂單創建事件被處理
        verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
        
        try {
            // 嘗試提交沒有訂單項的訂單，應該拋出異常
            orderApplicationService.submitOrder(orderId);
            fail("應該拋出異常，因為訂單沒有項目");
        } catch (Exception e) {
            // 預期會拋出異常
            assertTrue(e.getMessage().contains("Cannot submit an order with no items"));
        }
        
        // 獲取訂單並驗證狀態沒有變化
        OrderResponse order = orderApplicationService.getOrder(orderId);
        assertNotNull(order);
        assertEquals("CREATED", order.getStatus());
    }
}