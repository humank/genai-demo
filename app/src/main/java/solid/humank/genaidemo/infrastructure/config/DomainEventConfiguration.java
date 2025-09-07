package solid.humank.genaidemo.infrastructure.config;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import solid.humank.genaidemo.domain.common.event.DomainEventPublisher;
import solid.humank.genaidemo.infrastructure.event.publisher.DomainEventPublisherAdapter;
import solid.humank.genaidemo.infrastructure.event.publisher.InMemoryDomainEventPublisher;
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
     * 開發環境專用的記憶體事件發布器（開發環境主要使用）
     * 提供事件追蹤和除錯功能
     */
    @Bean("domainEventPublisher")
    @Primary
    @Profile("dev")
    public DomainEventPublisher inMemoryDomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        return new InMemoryDomainEventPublisher(eventPublisher);
    }

    /**
     * 事務感知的領域事件發布器（生產環境主要使用）
     * 確保事件在事務提交後才被處理，事務回滾時清理事件
     */
    @Bean("domainEventPublisher")
    @Primary
    @Profile({"production", "test"})
    public DomainEventPublisher transactionalDomainEventPublisher(ApplicationEventPublisher eventPublisher) {
        return new TransactionalDomainEventPublisher(eventPublisher);
    }

    /**
     * 事務感知的領域事件發布器（備用 Bean）
     * 確保事件在事務提交後才被處理，事務回滾時清理事件
     */
    @Bean("transactionalDomainEventPublisher")
    public DomainEventPublisher transactionalDomainEventPublisherBean(ApplicationEventPublisher eventPublisher) {
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