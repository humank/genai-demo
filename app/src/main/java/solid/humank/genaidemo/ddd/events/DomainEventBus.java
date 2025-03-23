package solid.humank.genaidemo.ddd.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 領域事件匯流排
 * 用於處理領域事件的發布和訂閱
 */
public class DomainEventBus {
    private static final DomainEventBus INSTANCE = new DomainEventBus();
    private final Map<Class<? extends DomainEvent>, List<Consumer<DomainEvent>>> subscribers;

    private DomainEventBus() {
        this.subscribers = new HashMap<>();
    }

    public static DomainEventBus getInstance() {
        return INSTANCE;
    }

    /**
     * 訂閱特定類型的領域事件
     */
    public <T extends DomainEvent> void subscribe(
        Class<T> eventType, 
        Consumer<DomainEvent> handler
    ) {
        subscribers
            .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
            .add(handler);
    }

    /**
     * 發布領域事件
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
     */
    public <T extends DomainEvent> void unsubscribe(
        Class<T> eventType,
        Consumer<DomainEvent> handler
    ) {
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
    private final List<Exception> exceptions;

    public DomainEventHandlingException(String message, List<Exception> exceptions) {
        super(message + ": " + exceptions.size() + " handlers failed");
        this.exceptions = exceptions;
    }

    public List<Exception> getExceptions() {
        return new ArrayList<>(exceptions);
    }
}
