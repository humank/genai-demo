package solid.humank.genaidemo.infrastructure.common.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import solid.humank.genaidemo.domain.common.event.EventBus;
import solid.humank.genaidemo.domain.common.event.EventHandler;

/** 簡單事件總線實現 使用內存存儲事件處理器 */
@Component
public class SimpleEventBus implements EventBus {

    private final Map<Class<?>, List<EventHandler<?>>> handlers = new ConcurrentHashMap<>();

    @Override
    public void publish(Object event) {
        if (event == null) {
            return;
        }

        Class<?> eventType = event.getClass();
        List<EventHandler<?>> eventHandlers = handlers.get(eventType);

        if (eventHandlers != null) {
            for (EventHandler<?> handler : eventHandlers) {
                try {
                    invokeHandler(handler, event);
                } catch (Exception e) {
                    // 記錄錯誤但不中斷處理
                    System.err.println("Error handling event: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void invokeHandler(EventHandler<?> handler, Object event) {
        ((EventHandler<T>) handler).handle((T) event);
    }

    @Override
    public <T> void subscribe(Class<T> eventType, EventHandler<T> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    @Override
    public <T> void unsubscribe(Class<T> eventType, EventHandler<T> handler) {
        List<EventHandler<?>> eventHandlers = handlers.get(eventType);
        if (eventHandlers != null) {
            eventHandlers.remove(handler);
            if (eventHandlers.isEmpty()) {
                handlers.remove(eventType);
            }
        }
    }
}
