package solid.humank.genaidemo.infrastructure.observability.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/**
 * 記憶體分析會話儲存庫實現
 */
@Repository
@ConditionalOnProperty(name = "genai-demo.events.publisher", havingValue = "in-memory")
public class InMemoryAnalyticsSessionRepository implements AnalyticsSessionRepository {

    private final Map<String, InMemoryAnalyticsSession> sessions = new ConcurrentHashMap<>();

    @Override
    public List<AnalyticsSessionEntity> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return sessions.values().stream()
                .filter(s -> s.getCreatedAt().isAfter(startTime) && s.getCreatedAt().isBefore(endTime))
                .collect(Collectors.toList());
    }

    // 用於添加會話的方法
    public void addSession(String sessionId, String userId, LocalDateTime createdAt, LocalDateTime endedAt) {
        InMemoryAnalyticsSession session = new InMemoryAnalyticsSession(sessionId, userId, createdAt, endedAt);
        sessions.put(sessionId, session);
    }

    // 內部會話實現
    private static class InMemoryAnalyticsSession implements AnalyticsSessionEntity {
        private final String id;
        private final String userId;
        private final LocalDateTime createdAt;
        private final LocalDateTime endedAt;

        public InMemoryAnalyticsSession(String id, String userId, LocalDateTime createdAt, LocalDateTime endedAt) {
            this.id = id;
            this.userId = userId;
            this.createdAt = createdAt;
            this.endedAt = endedAt;
        }

        @Override
        public String getId() { return id; }

        @Override
        public String getUserId() { return userId; }

        @Override
        public LocalDateTime getCreatedAt() { return createdAt; }

        @Override
        public LocalDateTime getEndedAt() { return endedAt; }
    }
}