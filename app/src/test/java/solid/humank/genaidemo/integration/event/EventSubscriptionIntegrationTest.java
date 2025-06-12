package solid.humank.genaidemo.integration.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventSubscriptionManager;
import solid.humank.genaidemo.domain.common.event.EventSubscriber;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.notification.model.events.NotificationCreatedEvent;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationChannel;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationId;
import solid.humank.genaidemo.domain.notification.model.valueobject.NotificationType;
import solid.humank.genaidemo.domain.notification.service.NotificationEventHandler;
import solid.humank.genaidemo.domain.order.model.events.OrderCreatedEvent;
import solid.humank.genaidemo.domain.order.service.OrderEventHandler;
import solid.humank.genaidemo.infrastructure.event.DomainEventPublisherAdapter;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * 事件訂閱整合測試
 * 測試事件訂閱機制的正確性
 */
@SpringBootTest
public class EventSubscriptionIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private DomainEventPublisherAdapter eventPublisherAdapter;
    
    @Autowired
    private DomainEventSubscriptionManager subscriptionManager;
    
    @SpyBean
    private OrderEventHandler orderEventHandler;
    
    @SpyBean
    private NotificationEventHandler notificationEventHandler;
    
    /**
     * 測試事件訂閱註解是否正確註冊
     */
    @Test
    public void testEventSubscriberAnnotationRegistration() {
        // 獲取OrderEventHandler中的所有方法
        List<Method> annotatedMethods = Arrays.stream(OrderEventHandler.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(EventSubscriber.class))
                .collect(Collectors.toList());
        
        // 驗證有3個帶有@EventSubscriber註解的方法
        assertEquals(3, annotatedMethods.size());
        
        // 驗證方法名稱
        List<String> methodNames = annotatedMethods.stream()
                .map(Method::getName)
                .collect(Collectors.toList());
        
        assertTrue(methodNames.contains("handleOrderCreated"));
        assertTrue(methodNames.contains("handleOrderItemAdded"));
        assertTrue(methodNames.contains("handleOrderSubmitted"));
    }
    
    /**
     * 測試事件訂閱和處理
     */
    @Test
    public void testEventSubscriptionAndHandling() {
        // 創建訂單創建事件
        OrderCreatedEvent orderEvent = new OrderCreatedEvent(
            OrderId.generate(),
            "customer-test",
            Money.of(BigDecimal.valueOf(1500)),
            Collections.emptyList()
        );
        
        // 發布事件
        eventPublisherAdapter.publish(orderEvent);
        
        // 驗證對應的處理方法被調用
        verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
        
        // 創建通知創建事件
        NotificationCreatedEvent notificationEvent = new NotificationCreatedEvent(
            NotificationId.generate(),
            "customer-test",
            NotificationType.ORDER_CREATED,
            "訂單已確認",
            List.of(NotificationChannel.EMAIL)
        );
        
        // 發布事件
        eventPublisherAdapter.publish(notificationEvent);
        
        // 驗證對應的處理方法被調用
        verify(notificationEventHandler, timeout(1000)).handleNotificationCreated(any(NotificationCreatedEvent.class));
    }
    
    /**
     * 測試事件訂閱的特定性
     * 確保事件只被相應的處理器處理
     */
    @Test
    public void testEventSubscriptionSpecificity() {
        // 創建訂單創建事件
        OrderCreatedEvent orderEvent = new OrderCreatedEvent(
            OrderId.generate(),
            "customer-specific",
            Money.of(BigDecimal.valueOf(2000)),
            Collections.emptyList()
        );
        
        // 發布事件
        eventPublisherAdapter.publish(orderEvent);
        
        // 驗證只有訂單事件處理器被調用，而通知事件處理器沒有被調用
        verify(orderEventHandler, timeout(1000)).handleOrderCreated(any(OrderCreatedEvent.class));
        verify(notificationEventHandler, timeout(1000).times(0)).handleNotificationCreated(any());
    }
}