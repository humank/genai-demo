package solid.humank.genaidemo.infrastructure.event;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.EventSubscriber;

/** 領域事件訂閱管理器 負責管理基於註解的事件訂閱 */
@Component
public class DomainEventSubscriptionManager
        implements BeanPostProcessor, ApplicationListener<ApplicationEvent> {
    private static final Logger LOGGER = Logger.getLogger(DomainEventSubscriptionManager.class.getName());

    // 存儲事件類型到訂閱者的映射
    private final Map<Class<?>, Map<Object, Method>> subscriptions = new ConcurrentHashMap<>();

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 掃描bean中的所有方法，查找帶有@EventSubscriber註解的方法
        for (Method method : bean.getClass().getDeclaredMethods()) {
            EventSubscriber annotation = AnnotationUtils.findAnnotation(method, EventSubscriber.class);
            if (annotation != null) {
                registerSubscriber(bean, method, annotation);
            }
        }
        return bean;
    }

    /** 註冊事件訂閱者 */
    private void registerSubscriber(Object bean, Method method, EventSubscriber annotation) {
        Class<? extends DomainEvent> eventType = annotation.value();

        // 檢查方法參數是否與事件類型匹配
        if (method.getParameterCount() != 1
                || !eventType.isAssignableFrom(method.getParameterTypes()[0])) {
            LOGGER.warning(
                    () -> String.format(
                            "Method %s in %s has @EventSubscriber annotation but parameter"
                                    + " types don't match event type %s",
                            method.getName(),
                            bean.getClass().getName(),
                            eventType.getName()));
            return;
        }

        // 確保方法可訪問
        if (!method.canAccess(bean)) {
            method.setAccessible(true);
        }

        // 註冊訂閱
        subscriptions.computeIfAbsent(eventType, k -> new HashMap<>()).put(bean, method);

        LOGGER.info(
                () -> String.format(
                        "Registered event subscriber %s.%s for event %s",
                        bean.getClass().getSimpleName(),
                        method.getName(),
                        eventType.getSimpleName()));
    }

    /** 處理應用事件 */
    @Override
    public void onApplicationEvent(ApplicationEvent springEvent) {
        Object event = springEvent.getSource();
        // 只處理DomainEvent類型的事件
        if (!(event instanceof DomainEvent domainEvent)) {
            return;
        }

        // 獲取事件的實際類型
        Class<?> eventClass = domainEvent.getClass();

        // 查找並調用所有匹配的訂閱者
        subscriptions.forEach(
                (eventType, subscribers) -> {
                    if (eventType.isAssignableFrom(eventClass)) {
                        notifySubscribers(subscribers, domainEvent);
                    }
                });
    }

    /** 通知所有訂閱者 */
    private void notifySubscribers(Map<Object, Method> subscribers, DomainEvent event) {
        subscribers.forEach(
                (bean, method) -> {
                    try {
                        LOGGER.fine(
                                () -> String.format(
                                        "Invoking subscriber %s.%s for event %s",
                                        bean.getClass().getSimpleName(),
                                        method.getName(),
                                        event.getClass().getSimpleName()));

                        method.invoke(bean, event);
                    } catch (Exception e) {
                        LOGGER.log(
                                Level.SEVERE,
                                String.format(
                                        "Error invoking subscriber %s.%s for event %s",
                                        bean.getClass().getSimpleName(),
                                        method.getName(),
                                        event.getClass().getSimpleName()),
                                e);
                    }
                });
    }
}