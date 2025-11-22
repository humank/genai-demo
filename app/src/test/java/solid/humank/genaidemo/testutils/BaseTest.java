package solid.humank.genaidemo.testutils;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.context.annotation.Import;

import solid.humank.genaidemo.config.TestApplicationConfiguration;
import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventBus;
import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;
import solid.humank.genaidemo.domain.common.lifecycle.AggregateLifecycle;
import solid.humank.genaidemo.domain.common.lifecycle.AggregateLifecycleAware;

/**
 * 基礎測試類
 * 提供測試所需的基本設置
 */
@Import(TestApplicationConfiguration.class)
public abstract class BaseTest {

    private static boolean initialized = false;

    @BeforeAll
    static void setupAggregateLifecycle() {
        if (!initialized) {
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

            initialized = true;
            System.out.println("AggregateLifecycle initialized for tests");
        }
    }
}