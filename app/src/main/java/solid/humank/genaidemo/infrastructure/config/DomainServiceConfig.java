package solid.humank.genaidemo.infrastructure.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import solid.humank.genaidemo.domain.common.event.DomainEventBus;
import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;
import solid.humank.genaidemo.domain.common.lifecycle.AggregateLifecycle;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;

/**
 * 領域服務配置
 * 負責將純領域服務註冊到Spring容器中
 * 
 * 需求 3.4: 移除Domain層的基礎設施框架依賴
 */
@Configuration
public class DomainServiceConfig {

    /**
     * 註冊領域事件發布器適配器 (備用實現)
     * 注意：主要的 domainEventPublisher bean 由 DomainEventConfiguration 提供
     */
    @Bean("domainServiceEventPublisher")
    public DomainEventPublisher domainServiceEventPublisher(ApplicationEventPublisher eventPublisher) {
        return new DomainEventPublisherAdapter(eventPublisher);
    }

    /**
     * 註冊領域事件總線
     * 使用主要的 domainEventPublisher bean
     */
    @Bean
    public DomainEventBus domainEventBus(DomainEventPublisher domainEventPublisher) {
        return new DomainEventBus(domainEventPublisher);
    }

    /**
     * 註冊聚合生命週期管理器
     */
    @Bean
    public AggregateLifecycle aggregateLifecycle(DomainEventBus eventBus) {
        AggregateLifecycle lifecycle = new AggregateLifecycle(eventBus);
        // 設置靜態實例以供領域實體使用
        solid.humank.genaidemo.domain.common.lifecycle.AggregateLifecycleAware.setLifecycleInstance(lifecycle);
        return lifecycle;
    }
}