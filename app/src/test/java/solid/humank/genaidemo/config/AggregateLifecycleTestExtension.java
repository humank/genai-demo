package solid.humank.genaidemo.config;

import java.util.List;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventBus;
import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;
import solid.humank.genaidemo.domain.common.lifecycle.AggregateLifecycle;
import solid.humank.genaidemo.domain.common.lifecycle.AggregateLifecycleAware;

/**
 * JUnit 5 擴展，用於在測試開始前初始化 AggregateLifecycle
 */
public class AggregateLifecycleTestExtension implements BeforeAllCallback {

    private static boolean initialized = false;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (!initialized) {
            initializeAggregateLifecycle();
            initialized = true;
        }
    }

    private void initializeAggregateLifecycle() {
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

        System.out.println("AggregateLifecycle initialized for tests");
    }
}