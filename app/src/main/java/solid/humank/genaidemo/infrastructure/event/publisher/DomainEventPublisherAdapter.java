package solid.humank.genaidemo.infrastructure.event.publisher;

import org.springframework.context.ApplicationEventPublisher;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;

/** 領域事件發布適配器 將領域事件轉換為Spring應用事件 */
public class DomainEventPublisherAdapter implements DomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public DomainEventPublisherAdapter(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 發布領域事件 將領域事件包裝為Spring應用事件 如果在事務中，事件會在事務提交後才真正處理
     *
     * @param event 領域事件
     */
    @Override
    public void publish(DomainEvent event) {
        if (event != null) {
            DomainEventWrapper wrapper = new DomainEventWrapper(event);

            // 檢查是否在事務中
            if (org.springframework.transaction.support.TransactionSynchronizationManager
                    .isActualTransactionActive()) {
                // 在事務中，使用Spring的事務事件機制
                // @TransactionalEventListener會確保事件在事務提交後處理
                eventPublisher.publishEvent(wrapper);
            } else {
                // 不在事務中，立即發布
                eventPublisher.publishEvent(wrapper);
            }
        }
    }

    /**
     * 批量發布領域事件
     *
     * @param events 領域事件列表
     */
    @Override
    public void publishAll(java.util.List<DomainEvent> events) {
        if (events != null && !events.isEmpty()) {
            for (DomainEvent event : events) {
                publish(event);
            }
        }
    }

    /**
     * 同步發布領域事件（立即發布，不等待事務提交）
     *
     * @param event 領域事件
     */
    public void publishSync(DomainEvent event) {
        if (event != null) {
            // 立即發布，不管是否在事務中
            eventPublisher.publishEvent(new DomainEventWrapper(event));
        }
    }

    /** 領域事件包裝器 將領域事件包裝為Spring應用事件 */
    public static class DomainEventWrapper extends org.springframework.context.ApplicationEvent {
        private static final long serialVersionUID = 1L;

        public DomainEventWrapper(DomainEvent event) {
            super(event);
        }

        @Override
        public DomainEvent getSource() {
            return (DomainEvent) super.getSource();
        }
    }
}
