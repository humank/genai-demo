package solid.humank.genaidemo.application.common.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import solid.humank.genaidemo.domain.common.aggregate.AggregateRootInterface;
import solid.humank.genaidemo.domain.common.event.DomainEvent;
import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;

/**
 * 領域事件應用服務
 * 負責從聚合根收集事件並發布
 * 在 Application Layer 協調 Domain Layer
 * 
 * 需求 6.1: 實現聚合根領域事件的正確捕獲和發布
 * 需求 6.3: 確保事件在事務提交後才被處理
 */
@Service
public class DomainEventApplicationService {

    private final DomainEventPublisher domainEventPublisher;

    public DomainEventApplicationService(
            @Qualifier("transactionalDomainEventPublisher") DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
    }

    /**
     * 從聚合根收集並發布所有未提交的事件 在事務邊界內確保事件發布的一致性
     *
     * @param aggregateRoot 聚合根實例
     */
    @Transactional
    public void publishEventsFromAggregate(AggregateRootInterface aggregateRoot) {
        if (aggregateRoot == null || !aggregateRoot.hasUncommittedEvents()) {
            return;
        }

        List<DomainEvent> events = aggregateRoot.getUncommittedEvents();

        // 在事務邊界內發布所有事件
        // Spring的@TransactionalEventListener會確保事件在事務提交後才真正處理
        domainEventPublisher.publishAll(events);

        // 標記事件為已提交
        aggregateRoot.markEventsAsCommitted();
    }

    /**
     * 從多個聚合根收集並發布所有未提交的事件 在事務邊界內確保事件發布的一致性
     *
     * @param aggregateRoots 聚合根實例列表
     */
    @Transactional
    public void publishEventsFromAggregates(List<AggregateRootInterface> aggregateRoots) {
        if (aggregateRoots == null || aggregateRoots.isEmpty()) {
            return;
        }

        for (AggregateRootInterface aggregateRoot : aggregateRoots) {
            publishEventsFromAggregate(aggregateRoot);
        }
    }

    /**
     * 同步發布事件（立即發布，不等待事務提交） 用於需要立即處理的事件
     *
     * @param aggregateRoot 聚合根實例
     */
    public void publishEventsFromAggregateSync(AggregateRootInterface aggregateRoot) {
        if (aggregateRoot == null || !aggregateRoot.hasUncommittedEvents()) {
            return;
        }

        List<DomainEvent> events = aggregateRoot.getUncommittedEvents();

        // 立即發布所有事件
        domainEventPublisher.publishAll(events);

        // 標記事件為已提交
        aggregateRoot.markEventsAsCommitted();
    }
}
