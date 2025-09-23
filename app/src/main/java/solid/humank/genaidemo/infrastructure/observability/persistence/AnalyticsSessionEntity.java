package solid.humank.genaidemo.infrastructure.observability.persistence;

import java.time.LocalDateTime;

/**
 * 分析會話實體介面
 */
public interface AnalyticsSessionEntity {
    
    String getId();
    String getUserId();
    LocalDateTime getCreatedAt();
    LocalDateTime getEndedAt();
}