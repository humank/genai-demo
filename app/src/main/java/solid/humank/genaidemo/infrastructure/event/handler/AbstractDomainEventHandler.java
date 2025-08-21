package solid.humank.genaidemo.infrastructure.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventHandler;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;

/**
 * 抽象領域事件處理器
 * 提供事務感知的事件處理基礎實現
 * 
 * 需求 6.2: 建立事件處理器接收和處理事件的機制
 * 需求 6.3: 確保事件在事務提交後才被處理
 */
public abstract class AbstractDomainEventHandler<T extends DomainEvent> implements DomainEventHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDomainEventHandler.class);

    /**
     * Spring 事件監聽器，處理包裝後的領域事件
     * 使用 @TransactionalEventListener 確保在事務提交後處理
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Order(100) // 設定處理順序
    public void onDomainEvent(DomainEventPublisherAdapter.DomainEventWrapper wrapper) {
        DomainEvent event = wrapper.getSource();

        // 檢查事件類型是否匹配
        if (getSupportedEventType().isInstance(event)) {
            @SuppressWarnings("unchecked")
            T typedEvent = (T) event;

            // 檢查是否應該處理此事件
            if (shouldHandle(typedEvent)) {
                try {
                    // 記錄處理開始
                    logEventProcessingStart(typedEvent);

                    // 處理事件
                    handle(typedEvent);

                    // 記錄處理成功
                    logEventProcessingSuccess(typedEvent);

                } catch (Exception e) {
                    // 記錄處理失敗
                    logEventProcessingError(typedEvent, e);

                    // 重新拋出異常，讓上層處理
                    throw new DomainEventProcessingException(
                            "Failed to process event: " + event.getClass().getSimpleName(), e);
                }
            }
        }
    }

    /**
     * 記錄事件處理開始
     */
    protected void logEventProcessingStart(T event) {
        String eventType = event.getClass().getSimpleName();
        String aggregateId = event.getAggregateId();
        LOGGER.info("[{}] Processing event: {} (AggregateId: {})",
                getHandlerName(), eventType, aggregateId);
    }

    /**
     * 記錄事件處理成功
     */
    protected void logEventProcessingSuccess(T event) {
        String eventType = event.getClass().getSimpleName();
        LOGGER.info("[{}] Successfully processed event: {}",
                getHandlerName(), eventType);
    }

    /**
     * 記錄事件處理錯誤
     */
    protected void logEventProcessingError(T event, Exception error) {
        String eventType = event.getClass().getSimpleName();
        LOGGER.error("[{}] Failed to process event: {}, error: {}",
                getHandlerName(), eventType, error.getMessage(), error);
    }

    /**
     * 領域事件處理異常
     */
    public static class DomainEventProcessingException extends RuntimeException {
        public DomainEventProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}