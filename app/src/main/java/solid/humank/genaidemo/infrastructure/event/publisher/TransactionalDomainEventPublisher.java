package solid.humank.genaidemo.infrastructure.event.publisher;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;

/**
 * 事務感知的領域事件發布器
 * 確保事件在事務提交後才被處理，事務回滾時清理事件
 * 
 * 需求 6.3: 確保事件在事務提交後才被處理
 * 需求 6.4: 實現事務回滾時事件清理機制
 */
public class TransactionalDomainEventPublisher implements DomainEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(TransactionalDomainEventPublisher.class);

    private final ApplicationEventPublisher eventPublisher;

    public TransactionalDomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void publish(DomainEvent event) {
        if (event == null) {
            return;
        }

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // 在事務中，延遲到事務提交後發布
            publishAfterCommit(event);
        } else {
            // 不在事務中，立即發布
            publishImmediately(event);
        }
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        for (DomainEvent event : events) {
            publish(event);
        }
    }

    /**
     * 立即發布事件（不等待事務提交）
     */
    public void publishImmediately(DomainEvent event) {
        if (event != null) {
            eventPublisher.publishEvent(new DomainEventPublisherAdapter.DomainEventWrapper(event));
        }
    }

    /**
     * 在事務提交後發布事件
     */
    private void publishAfterCommit(DomainEvent event) {
        // 獲取或創建當前事務的事件收集器
        DomainEventTransactionSynchronization synchronization = getCurrentTransactionSynchronization();

        // 將事件添加到待發布列表
        synchronization.addEvent(event);
    }

    /**
     * 獲取當前事務的同步器，如果不存在則創建
     */
    private DomainEventTransactionSynchronization getCurrentTransactionSynchronization() {
        // 檢查是否已經註冊了同步器
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();

        for (TransactionSynchronization sync : synchronizations) {
            if (sync instanceof DomainEventTransactionSynchronization) {
                return (DomainEventTransactionSynchronization) sync;
            }
        }

        // 沒有找到，創建新的同步器
        DomainEventTransactionSynchronization synchronization = new DomainEventTransactionSynchronization(
                eventPublisher);
        TransactionSynchronizationManager.registerSynchronization(synchronization);

        return synchronization;
    }

    /**
     * 事務同步器，負責在事務提交後發布事件，回滾時清理事件
     */
    private static class DomainEventTransactionSynchronization implements TransactionSynchronization {

        private final ApplicationEventPublisher eventPublisher;
        private final List<DomainEvent> pendingEvents = new ArrayList<>();

        public DomainEventTransactionSynchronization(ApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
        }

        public void addEvent(DomainEvent event) {
            pendingEvents.add(event);
        }

        @Override
        public void afterCommit() {
            // 事務提交後發布所有待發布的事件
            for (DomainEvent event : pendingEvents) {
                try {
                    eventPublisher.publishEvent(new DomainEventPublisherAdapter.DomainEventWrapper(event));
                } catch (Exception e) {
                    // 記錄錯誤但不影響其他事件的發布
                    logger.error("Failed to publish domain event: {}, error: {}",
                            event.getClass().getSimpleName(), e.getMessage(), e);
                }
            }
        }

        @Override
        public void afterCompletion(int status) {
            // 需求 6.4: 實現事務回滾時事件清理機制
            int eventCount = pendingEvents.size();

            if (status == STATUS_ROLLED_BACK) {
                // 事務回滾，記錄日誌並清理事件
                logger.info("Transaction rolled back, cleared {} pending domain events", eventCount);
            }

            // 無論事務成功還是失敗，都清理待發布事件列表
            pendingEvents.clear();
        }

        @Override
        public int getOrder() {
            return LOWEST_PRECEDENCE; // 最後執行，確保其他事務操作都完成
        }
    }
}