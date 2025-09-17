package solid.humank.genaidemo.infrastructure.observability.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka Topic 配置
 * 
 * 提供可觀測性相關的 Kafka topic 名稱配置，
 * 支援環境差異化的 topic 命名規範。
 * 
 * Topic 命名規範：
 * - 格式: genai-demo.{environment}.observability.{domain}
 * - 環境: dev, test, production
 * - 領域: user.behavior, performance.metrics, business.analytics
 * 
 * 需求: 3.1, 3.2
 */
@Configuration
@ConditionalOnProperty(name = "genai-demo.events.publisher", havingValue = "kafka")
public class KafkaTopicConfig {

    @Value("${genai-demo.domain-events.topic.prefix:genai-demo}")
    private String topicPrefix;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 獲取用戶行為分析事件的 topic 名稱
     * 
     * @return topic 名稱
     */
    public String getUserBehaviorTopicName() {
        return String.format("%s.%s.observability.user.behavior", topicPrefix, activeProfile);
    }

    /**
     * 獲取效能指標事件的 topic 名稱
     * 
     * @return topic 名稱
     */
    public String getPerformanceMetricTopicName() {
        return String.format("%s.%s.observability.performance.metrics", topicPrefix, activeProfile);
    }

    /**
     * 獲取業務分析事件的 topic 名稱
     * 
     * @return topic 名稱
     */
    public String getBusinessAnalyticsTopicName() {
        return String.format("%s.%s.observability.business.analytics", topicPrefix, activeProfile);
    }

    /**
     * 獲取用戶行為分析事件的 DLQ topic 名稱
     * 
     * @return DLQ topic 名稱
     */
    public String getUserBehaviorDlqTopicName() {
        return getUserBehaviorTopicName() + ".dlq";
    }

    /**
     * 獲取效能指標事件的 DLQ topic 名稱
     * 
     * @return DLQ topic 名稱
     */
    public String getPerformanceMetricDlqTopicName() {
        return getPerformanceMetricTopicName() + ".dlq";
    }

    /**
     * 獲取業務分析事件的 DLQ topic 名稱
     * 
     * @return DLQ topic 名稱
     */
    public String getBusinessAnalyticsDlqTopicName() {
        return getBusinessAnalyticsTopicName() + ".dlq";
    }

    // === Getter 方法 ===

    public String getTopicPrefix() {
        return topicPrefix;
    }

    public String getActiveProfile() {
        return activeProfile;
    }
}