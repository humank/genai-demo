package solid.humank.genaidemo.infrastructure.event;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;

/** 事務性事件管理器 確保事件在事務提交後才發布，事務回滾時清理事件 */
@Component
public class TransactionalEventManager {

    private final ThreadLocal<List<DomainEvent>> pendingEvents = new ThreadLocal<>();

    /**
     * 添加待發布的事件
     *
     * @param event 領域事件
     */
    public void addPendingEvent(DomainEvent event) {
        if (event == null) {
            return;
        }

        List<DomainEvent> events = pendingEvents.get();
        if (events == null) {
            events = new ArrayList<>();
            pendingEvents.set(events);
        }
        events.add(event);
    }

    /**
     * 添加多個待發布的事件
     *
     * @param events 領域事件列表
     */
    public void addPendingEvents(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        for (DomainEvent event : events) {
            addPendingEvent(event);
        }
    }

    /**
     * 獲取當前線程的待發布事件
     *
     * @return 待發布事件列表
     */
    public List<DomainEvent> getPendingEvents() {
        List<DomainEvent> events = pendingEvents.get();
        return events != null ? new ArrayList<>(events) : new ArrayList<>();
    }

    /** 清理當前線程的待發布事件 */
    public void clearPendingEvents() {
        pendingEvents.remove();
    }

    /** 事務提交後處理事件 Spring會在事務提交後調用此方法 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAfterCommit(DomainEventPublisherAdapter.DomainEventWrapper eventWrapper) {
        // 這個方法會在事務提交後被Spring調用
        // 實際的事件發布已經在DomainEventPublisherAdapter中處理
        // 這裡主要是為了確保事務邊界的正確性
    }

    /** 事務回滾後清理事件 Spring會在事務回滾後調用此方法 */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleAfterRollback(DomainEventPublisherAdapter.DomainEventWrapper eventWrapper) {
        // 事務回滾時清理待發布的事件
        clearPendingEvents();
    }
}
