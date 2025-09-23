package solid.humank.genaidemo.infrastructure.observability.persistence;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分析會話儲存庫介面
 */
public interface AnalyticsSessionRepository {
    
    List<AnalyticsSessionEntity> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
}