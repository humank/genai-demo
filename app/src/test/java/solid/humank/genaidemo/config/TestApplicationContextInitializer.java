package solid.humank.genaidemo.config;

import java.util.List;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.lang.NonNull;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventBus;
import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;
import solid.humank.genaidemo.domain.common.lifecycle.AggregateLifecycle;
import solid.humank.genaidemo.domain.common.lifecycle.AggregateLifecycleAware;

/**
 * 測試應用程式上下文初始化器
 * 在測試開始前設置必要的靜態依賴
 */
public class TestApplicationContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
        // 創建測試用的領域事件發布器
        DomainEventPublisher testEventPublisher = new DomainEventPublisher() {
            @Override
            public void publish(DomainEvent event) {
                // 測試環境下不實際發布事件
                System.out.println("Test: Publishing event " + event.getClass().getSimpleName());
            }

            @Override
            public void publishAll(List<DomainEvent> events) {
                events.forEach(this::publish);
            }
        };

        // 創建測試用的領域事件總線
        DomainEventBus testEventBus = new DomainEventBus(testEventPublisher);

        // 創建並設置聚合生命週期管理器
        AggregateLifecycle lifecycle = new AggregateLifecycle(testEventBus);
        AggregateLifecycleAware.setLifecycleInstance(lifecycle);
    }
}