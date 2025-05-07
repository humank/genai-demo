package solid.humank.genaidemo.ddd.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

/**
 * 領域事件匯流排
 * 用於處理領域事件的發布和訂閱
 * 使用 Spring 的依賴注入代替單例模式
 */
@Component
public class DomainEventBus {
    private final Map<Class<? extends DomainEvent>, List<Consumer<DomainEvent>>> subscribers;

    /**
     * 構造函數
     */
    public DomainEventBus() {
        this.subscribers = new HashMap<>();
    }

    /**
     * 訂閱特定類型的領域事件
     * 
     * @param <T> 事件類型
     * @param eventType 事件類
     * @param handler 事件處理器
     */
    public <T extends DomainEvent> void subscribe(
        Class<T> eventType, 
        Consumer<DomainEvent> handler
    ) {
        if (eventType == null) {
            throw new IllegalArgumentException("Event type cannot be null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Event handler cannot be null");
        }
        
        subscribers
            .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
            .add(handler);
    }

    /**
     * 發布領域事件
     * 
     * @param event 要發布的事件
     * @throws IllegalArgumentException 如果事件為 null
     * @throws DomainEventHandlingException 如果事件處理過程中發生錯誤
     */
    public void publish(DomainEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        List<Exception> exceptions = new ArrayList<>();

        // 通知所有訂閱者
        subscribers.getOrDefault(event.getClass(), List.of())
            .forEach(handler -> {
                try {
                    handler.accept(event);
                } catch (Exception e) {
                    exceptions.add(e);
                }
            });

        // 如果有任何處理器出錯，拋出組合異常
        if (!exceptions.isEmpty()) {
            throw new DomainEventHandlingException(
                "Error handling event: " + event.getClass().getSimpleName(),
                exceptions
            );
        }
    }

    /**
     * 取消訂閱特定類型的領域事件
     * 
     * @param <T> 事件類型
     * @param eventType 事件類
     * @param handler 事件處理器
     */
    public <T extends DomainEvent> void unsubscribe(
        Class<T> eventType,
        Consumer<DomainEvent> handler
    ) {
        if (eventType == null || handler == null) {
            return;
        }
        
        subscribers.computeIfPresent(eventType, (key, handlers) -> {
            handlers.remove(handler);
            return handlers.isEmpty() ? null : handlers;
        });
    }

    /**
     * 清除所有訂閱
     */
    public void clear() {
        subscribers.clear();
    }
}

/**
 * 領域事件處理異常
 */
class DomainEventHandlingException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    
    private final List<Exception> exceptions;

    /**
     * 構造函數
     * 
     * @param message 錯誤訊息
     * @param exceptions 異常列表
     */
    public DomainEventHandlingException(String message, List<Exception> exceptions) {
        super(message + ": " + exceptions.size() + " handlers failed");
        this.exceptions = new ArrayList<>(exceptions);
    }

    /**
     * 獲取異常列表
     * 
     * @return 異常列表的副本
     */
    public List<Exception> getExceptions() {
        return new ArrayList<>(exceptions);
    }
}
