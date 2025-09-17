package solid.humank.genaidemo.infrastructure.observability.persistence;

import java.time.LocalDateTime;

/**
 * 分析事件實體介面
 * 
 * 定義分析事件的基本屬性，支援不同的實現方式。
 */
public interface AnalyticsEventEntity {
    
    String getId();
    String getUserId();
    String getSessionId();
    String getFrontendEventType();
    String getMetricType();
    String getMetricValue();
    LocalDateTime getTimestamp();
    String getPagePath();
    String getActionType();
}