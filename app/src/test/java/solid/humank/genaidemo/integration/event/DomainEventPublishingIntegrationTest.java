package solid.humank.genaidemo.integration.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.events.OrderCreatedEvent;
import solid.humank.genaidemo.domain.order.model.events.OrderItemAddedEvent;
import solid.humank.genaidemo.domain.order.model.events.OrderSubmittedEvent;
import solid.humank.genaidemo.domain.order.service.OrderEventHandler;
import solid.humank.genaidemo.infrastructure.event.DomainEventPublisherAdapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 領域事件發布整合測試
 * 測試領域事件從發布到處理的完整流程
 */
@SpringBootTest
public class DomainEventPublishingIntegrationTest {

    @Autowired
    private DomainEventPublisherAdapter eventPublisherAdapter;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @SpyBean
    private OrderEventHandler orderEventHandler;
    
    // 事件捕獲器
    private List<DomainEvent> capturedEvents;
    
    @BeforeEach
    public void setup() {
        capturedEvents = new ArrayList<>();
        
        // 使用spy替換原始的eventPublisherAdapter方法，以捕獲發布的事件
        doAnswer(invocation -> {
            DomainEvent event = invocation.getArgument(0);
            capturedEvents.add(event);
            // 調用原始方法
            return invocation.callRealMethod();
        }).when(eventPublisherAdapter).publish(any(DomainEvent.class));
    }
    
    /**
     * 測試直接發布事件
     * 驗證事件是否被正確發布和處理
     */
    @Test
    public void testDirectEventPublishing() {
        // 創建測試事件
        OrderCreatedEvent event = new OrderCreatedEvent(
            OrderId.generate(),
            "customer-123",
            Money.of(BigDecimal.valueOf(1000)),
            Collections.emptyList()
        );
        
        // 發布事件
        eventPublisherAdapter.publish(event);
        
        // 驗證事件被捕獲
        assertEquals(1, capturedEvents.size());
        assertTrue(capturedEvents.get(0) instanceof OrderCreatedEvent);
        
        // 驗證事件處理器被調用
        verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
    }
    
    /**
     * 測試多個事件的發布和處理
     */
    @Test
    public void testMultipleEventsPublishing() {
        OrderId orderId = OrderId.generate();
        
        // 創建並發布訂單創建事件
        OrderCreatedEvent createdEvent = new OrderCreatedEvent(
            orderId,
            "customer-456",
            Money.zero(),
            Collections.emptyList()
        );
        eventPublisherAdapter.publish(createdEvent);
        
        // 創建並發布訂單項添加事件
        OrderItemAddedEvent itemAddedEvent = new OrderItemAddedEvent(
            orderId,
            "product-123",
            2,
            Money.of(BigDecimal.valueOf(500))
        );
        eventPublisherAdapter.publish(itemAddedEvent);
        
        // 驗證事件被捕獲
        assertEquals(2, capturedEvents.size());
        
        // 驗證事件處理器被調用
        verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
        verify(orderEventHandler, timeout(1000)).handleOrderItemAdded(any(OrderItemAddedEvent.class));
    }
    
    /**
     * 測試事件處理的順序性
     */
    @Test
    public void testEventProcessingOrder() {
        OrderId orderId = OrderId.generate();
        
        // 創建事件
        OrderCreatedEvent createdEvent = new OrderCreatedEvent(
            orderId,
            "customer-789",
            Money.zero(),
            Collections.emptyList()
        );
        
        OrderItemAddedEvent itemAddedEvent = new OrderItemAddedEvent(
            orderId,
            "product-456",
            1,
            Money.of(BigDecimal.valueOf(300))
        );
        
        OrderSubmittedEvent submittedEvent = new OrderSubmittedEvent(
            orderId,
            "customer-789",
            Money.of(BigDecimal.valueOf(300)),
            1
        );
        
        // 按順序發布事件
        eventPublisherAdapter.publish(createdEvent);
        eventPublisherAdapter.publish(itemAddedEvent);
        eventPublisherAdapter.publish(submittedEvent);
        
        // 驗證事件處理器被按順序調用
        verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
        verify(orderEventHandler, timeout(1000)).handleOrderItemAdded(any(OrderItemAddedEvent.class));
        verify(orderEventHandler, timeout(1000)).handleOrderSubmitted(any(OrderSubmittedEvent.class));
        
        // 驗證捕獲的事件順序
        assertEquals(3, capturedEvents.size());
        assertTrue(capturedEvents.get(0) instanceof OrderCreatedEvent);
        assertTrue(capturedEvents.get(1) instanceof OrderItemAddedEvent);
        assertTrue(capturedEvents.get(2) instanceof OrderSubmittedEvent);
    }
}