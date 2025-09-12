package solid.humank.genaidemo.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import solid.humank.genaidemo.infrastructure.analytics.AnalyticsEventPublisher;
import solid.humank.genaidemo.infrastructure.analytics.NoOpAnalyticsEventPublisher;

/**
 * Configuration for Analytics components.
 * 
 * This configuration provides default implementations for analytics
 * components, particularly for development and test environments.
 */
@Configuration
public class AnalyticsConfiguration {

    /**
     * Provides a no-op analytics event publisher for development and test
     * environments.
     * This bean will be created only if no other AnalyticsEventPublisher bean is
     * available.
     */
    @Bean
    @ConditionalOnMissingBean(AnalyticsEventPublisher.class)
    @Profile({ "dev", "development", "test", "default" })
    public AnalyticsEventPublisher noOpAnalyticsEventPublisher() {
        return new NoOpAnalyticsEventPublisher();
    }
}