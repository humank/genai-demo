package solid.humank.genaidemo.integration.event;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.valueobject.Money;
import solid.humank.genaidemo.domain.common.valueobject.OrderId;
import solid.humank.genaidemo.domain.order.model.events.OrderCreatedEvent;
import solid.humank.genaidemo.domain.order.service.OrderEventHandler;
import solid.humank.genaidemo.infrastructure.event.DomainEventPublisherAdapter;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 事件處理性能測試
 * 測試事件處理機制的性能和穩定性
 */
@SpringBootTest
public class EventHandlingPerformanceTest {

    @Autowired
    private DomainEventPublisherAdapter eventPublisherAdapter;
    
    @SpyBean
    private OrderEventHandler orderEventHandler;
    
    /**
     * 測試批量事件處理性能
     */
    @Test
    public void testBulkEventHandling() throws InterruptedException {
        int eventCount = 50;
        List<DomainEvent> events = new ArrayList<>();
        
        // 創建多個事件
        for (int i = 0; i < eventCount; i++) {
            events.add(new OrderCreatedEvent(
                OrderId.generate(),
                "customer-" + i,
                Money.of(BigDecimal.valueOf(1000 + i)),
                Collections.emptyList()
            ));
        }
        
        // 記錄開始時間
        Instant start = Instant.now();
        
        // 創建計數器，用於等待所有事件處理完成
        CountDownLatch latch = new CountDownLatch(eventCount);
        
        // 設置模擬對象，每次調用時減少計數
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            latch.countDown();
            return result;
        }).when(orderEventHandler).handleOrderCreated(any(OrderCreatedEvent.class));
        
        // 發布所有事件
        for (DomainEvent event : events) {
            eventPublisherAdapter.publish(event);
        }
        
        // 等待所有事件處理完成，最多等待10秒
        boolean allProcessed = latch.await(10, TimeUnit.SECONDS);
        
        // 記錄結束時間
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        
        // 驗證所有事件都被處理
        assertTrue(allProcessed, "所有事件應該在超時前處理完成");
        
        // 驗證處理器被調用了正確的次數
        verify(orderEventHandler, times(eventCount)).handleOrderCreated(any(OrderCreatedEvent.class));
        
        // 輸出性能指標
        System.out.println("處理 " + eventCount + " 個事件耗時: " + duration.toMillis() + " 毫秒");
        System.out.println("平均每個事件處理時間: " + (duration.toMillis() / (double)eventCount) + " 毫秒");
    }
    
    /**
     * 測試事件處理的並發性
     */
    @Test
    public void testConcurrentEventHandling() throws InterruptedException {
        int threadCount = 5;
        int eventsPerThread = 10;
        int totalEvents = threadCount * eventsPerThread;
        
        // 創建計數器，用於等待所有事件處理完成
        CountDownLatch latch = new CountDownLatch(totalEvents);
        
        // 設置模擬對象，每次調用時減少計數
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            latch.countDown();
            return result;
        }).when(orderEventHandler).handleOrderCreated(any(OrderCreatedEvent.class));
        
        // 創建並啟動多個線程
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            Thread thread = new Thread(() -> {
                for (int i = 0; i < eventsPerThread; i++) {
                    OrderCreatedEvent event = new OrderCreatedEvent(
                        OrderId.generate(),
                        "customer-" + threadId + "-" + i,
                        Money.of(BigDecimal.valueOf(1000 + i)),
                        Collections.emptyList()
                    );
                    eventPublisherAdapter.publish(event);
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        // 等待所有線程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 等待所有事件處理完成，最多等待10秒
        boolean allProcessed = latch.await(10, TimeUnit.SECONDS);
        
        // 驗證所有事件都被處理
        assertTrue(allProcessed, "所有事件應該在超時前處理完成");
        
        // 驗證處理器被調用了正確的次數
        verify(orderEventHandler, times(totalEvents)).handleOrderCreated(any(OrderCreatedEvent.class));
    }
}