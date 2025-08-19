package solid.humank.genaidemo.infrastructure.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;
import solid.humank.genaidemo.infrastructure.event.publisher.TransactionalDomainEventPublisher;

/**
 * 領域事件配置
 * 配置事件發布器和相關基礎設施
 * 
 * 需求 6.1: 實現聚合根領域事件的正確捕獲和發布
 * 需求 6.3: 確保事件在事務提交後才被處理
 * 需求 6.4: 實現事務回滾時事件清理機制
 */
@Configuration
public class DomainEventConfiguration {

    /**
     * 事務感知的領域事件發布器（主要使用）
     * 確保事件在事務提交後才被處理，事務回滾時清理事件
     */
    @Bean("transactionalDomainEventPublisher")
    @Primary
    public DomainEventPublisher transactionalDomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        return new TransactionalDomainEventPublisher(eventPublisher);
    }

    /**
     * 傳統的領域事件發布器適配器（備用）
     * 用於需要立即發布事件的場景
     */
    @Bean("domainEventPublisherAdapter")
    public DomainEventPublisher domainEventPublisherAdapter(ApplicationEventPublisher eventPublisher) {
        return new DomainEventPublisherAdapter(eventPublisher);
    }
}