package solid.humank.genaidemo.ddd.events;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import solid.humank.genaidemo.utils.SpringContextHolder;

class DomainEventPublisherServiceTest {

    private DomainEventPublisherService publisherService;
    
    @Mock
    private DomainEventBus eventBus;
    
    @Mock
    private DomainEvent mockEvent;
    
    @Mock
    private ApplicationContext applicationContext;
    
    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        publisherService = new DomainEventPublisherService(eventBus);
        
        // 模擬 SpringContextHolder
        ReflectionTestUtilsWrapper.setField(SpringContextHolder.class, "applicationContext", applicationContext);
        ReflectionTestUtilsWrapper.setField(SpringContextHolder.class, "initialized", true);
        
        // 設置 applicationContext 的行為
        when(applicationContext.getBean(DomainEventPublisherService.class)).thenReturn(publisherService);
    }
    
    @Test
    void testPublish() {
        // 執行
        publisherService.publish(mockEvent);
        
        // 驗證
        verify(eventBus).publish(mockEvent);
    }
    
    @Test
    void testPublishWithNullEvent() {
        // 驗證拋出異常
        assertThrows(IllegalArgumentException.class, () -> {
            publisherService.publish(null);
        });
    }
    
    @Test
    void testPublishEventStatic() {
        // 執行
        DomainEventPublisherService.publishEvent(mockEvent);
        
        // 驗證
        verify(eventBus).publish(mockEvent);
    }
    
    /**
     * 用於在測試中訪問私有靜態字段的工具類
     */
    static class ReflectionTestUtilsWrapper {
        public static void setField(Class<?> targetClass, String fieldName, Object value) {
            try {
                java.lang.reflect.Field field = targetClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(null, value);
            } catch (Exception e) {
                throw new RuntimeException("Failed to set field: " + fieldName, e);
            }
        }
    }
}