package solid.humank.genaidemo.ddd.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import solid.humank.genaidemo.ddd.events.DomainEvent;
import solid.humank.genaidemo.ddd.events.DomainEventBus;

class AggregateLifecycleTest {

    private AggregateLifecycle aggregateLifecycle;
    
    @Mock
    private DomainEventBus eventBus;
    
    @Mock
    private DomainEvent mockEvent;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aggregateLifecycle = new AggregateLifecycle();
        ReflectionTestUtils.setField(aggregateLifecycle, "eventBus", eventBus);
        
        // 清理 ThreadLocal 資源
        AggregateLifecycle.clearCurrentThreadEvents();
    }
    
    @Test
    void testApply() {
        // 執行
        aggregateLifecycle.apply(mockEvent);
        
        // 驗證
        List<DomainEvent> pendingEvents = aggregateLifecycle.getPendingEvents();
        assertEquals(1, pendingEvents.size());
        assertSame(mockEvent, pendingEvents.get(0));
    }
    
    @Test
    void testCommit() {
        // 準備
        aggregateLifecycle.apply(mockEvent);
        
        // 執行
        aggregateLifecycle.commit();
        
        // 驗證
        verify(eventBus).publish(mockEvent);
        assertTrue(aggregateLifecycle.getPendingEvents().isEmpty());
    }
    
    @Test
    void testRollback() {
        // 準備
        aggregateLifecycle.apply(mockEvent);
        
        // 執行
        aggregateLifecycle.rollback();
        
        // 驗證
        assertTrue(aggregateLifecycle.getPendingEvents().isEmpty());
        verify(eventBus, never()).publish(any());
    }
    
    @Test
    void testClear() {
        // 準備
        aggregateLifecycle.apply(mockEvent);
        
        // 執行
        aggregateLifecycle.clear();
        
        // 驗證
        assertTrue(AggregateLifecycle.getCurrentThreadPendingEvents().isEmpty());
    }
    
    @Test
    void testGetCurrentThreadPendingEvents() {
        // 準備
        aggregateLifecycle.apply(mockEvent);
        
        // 執行
        List<DomainEvent> events = AggregateLifecycle.getCurrentThreadPendingEvents();
        
        // 驗證
        assertEquals(1, events.size());
        assertSame(mockEvent, events.get(0));
    }
}