package solid.humank.genaidemo.domain.common.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

/**
 * 領域事件匯流排
 * 用於處理領域事件的發布和訂閱
 * 使用 Spring 的依賴注入代替單例模式
 */
@Component
public class DomainEventBus {
    private static final Logger LOGGER = Logger.getLogger(DomainEventBus.class.getName());
    
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
     * @throws IllegalArgumentException 如果事件類型或處理器為 null
     */
    public <T extends DomainEvent> void subscribe(
        Class<T> eventType, 
        Consumer<DomainEvent> handler
    ) {
        Objects.requireNonNull(eventType, "Event type cannot be null");
        Objects.requireNonNull(handler, "Event handler cannot be null");
        
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("Subscribing to event type: %s", eventType.getSimpleName()));
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
        Objects.requireNonNull(event, "Event cannot be null");

        String eventName = event.getClass().getSimpleName();
        
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("Publishing event: %s", eventName));
        }

        List<Exception> exceptions = new ArrayList<>();

        // 通知所有訂閱者
        List<Consumer<DomainEvent>> handlers = subscribers.getOrDefault(event.getClass(), List.of());
        
        if (handlers.isEmpty() && LOGGER.isLoggable(Level.WARNING)) {
            LOGGER.warning(String.format("No handlers found for event: %s", eventName));
        }
        
        handlers.forEach(handler -> {
            try {
                handler.accept(event);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, String.format("Error handling event %s", eventName), e);
                exceptions.add(e);
            }
        });

        // 如果有任何處理器出錯，拋出組合異常
        if (!exceptions.isEmpty()) {
            throw new DomainEventHandlingException(
                String.format("Error handling event: %s", eventName),
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
        
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(String.format("Unsubscribing from event type: %s", eventType.getSimpleName()));
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
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Clearing all event subscriptions");
        }
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
        super(String.format("%s: %d handlers failed", message, exceptions.size()));
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
