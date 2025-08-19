package solid.humank.genaidemo.infrastructure.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventHandler;

/**
 * 領域事件處理器註冊表
 * 管理所有領域事件處理器的註冊和查找
 * 
 * 需求 6.2: 建立事件處理器接收和處理事件的機制
 */
@Component
public class DomainEventHandlerRegistry {

    private final Map<Class<? extends DomainEvent>, DomainEventHandler<? extends DomainEvent>> handlers = new HashMap<>();

    @Autowired
    private List<DomainEventHandler<? extends DomainEvent>> eventHandlers;

    @PostConstruct
    public void registerHandlers() {
        for (DomainEventHandler<? extends DomainEvent> handler : eventHandlers) {
            Class<? extends DomainEvent> eventType = handler.getSupportedEventType();
            handlers.put(eventType, handler);

            System.out.println(String.format(
                    "Registered domain event handler: %s for event type: %s",
                    handler.getHandlerName(),
                    eventType.getSimpleName()));
        }

        System.out.println(String.format(
                "Domain event handler registry initialized with %d handlers",
                handlers.size()));
    }

    /**
     * 獲取指定事件類型的處理器
     */
    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> DomainEventHandler<T> getHandler(Class<T> eventType) {
        return (DomainEventHandler<T>) handlers.get(eventType);
    }

    /**
     * 檢查是否有處理器可以處理指定事件類型
     */
    public boolean hasHandler(Class<? extends DomainEvent> eventType) {
        return handlers.containsKey(eventType);
    }

    /**
     * 獲取所有已註冊的事件類型
     */
    public java.util.Set<Class<? extends DomainEvent>> getRegisteredEventTypes() {
        return handlers.keySet();
    }

    /**
     * 獲取已註冊處理器的數量
     */
    public int getHandlerCount() {
        return handlers.size();
    }
}