package solid.humank.genaidemo.ddd.lifecycle;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import solid.humank.genaidemo.ddd.events.DomainEvent;
import solid.humank.genaidemo.utils.SpringContextHolder;

class AggregateLifecycleAwareTest {

    @Mock
    private AggregateLifecycle aggregateLifecycle;
    
    @Mock
    private DomainEvent mockEvent;
    
    @Mock
    private ApplicationContext applicationContext;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 模擬 SpringContextHolder
        ReflectionTestUtilsWrapper.setField(SpringContextHolder.class, "applicationContext", applicationContext);
        ReflectionTestUtilsWrapper.setField(SpringContextHolder.class, "initialized", true);
        
        // 設置 applicationContext 的行為
        when(applicationContext.getBean(AggregateLifecycle.class)).thenReturn(aggregateLifecycle);
    }
    
    @Test
    void testApply() {
        // 執行
        AggregateLifecycleAware.apply(mockEvent);
        
        // 驗證
        verify(aggregateLifecycle).apply(mockEvent);
    }
    
    @Test
    void testCommit() {
        // 執行
        AggregateLifecycleAware.commit();
        
        // 驗證
        verify(aggregateLifecycle).commit();
    }
    
    @Test
    void testRollback() {
        // 執行
        AggregateLifecycleAware.rollback();
        
        // 驗證
        verify(aggregateLifecycle).rollback();
    }
    
    @Test
    void testClear() {
        // 執行
        AggregateLifecycleAware.clear();
        
        // 驗證
        verify(aggregateLifecycle).clear();
    }
    
    @Test
    void testGetPendingEvents() {
        // 執行
        AggregateLifecycleAware.getPendingEvents();
        
        // 驗證 - 這個方法直接調用 AggregateLifecycle 的靜態方法，所以不需要驗證 mock
    }
    
    /**
     * 用於在測試中訪問私有靜態字段的工具類
     */
    static class ReflectionTestUtilsWrapper {
        @SuppressWarnings("unchecked")
        public static void setField(Class<?> targetClass, String fieldName, Object value) {
            try {
                java.lang.reflect.Field field = targetClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                
                // 根據字段類型設置值
                if (field.getType() == AtomicReference.class && value != null) {
                    ((AtomicReference<Object>) field.get(null)).set(value);
                } else if (field.getType() == AtomicBoolean.class && value instanceof Boolean) {
                    ((AtomicBoolean) field.get(null)).set((Boolean) value);
                } else {
                    field.set(null, value);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to set field: " + fieldName, e);
            }
        }
    }
}