package solid.humank.genaidemo.domain.observability.model.aggregate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import solid.humank.genaidemo.domain.common.aggregate.AggregateRootInterface;
import solid.humank.genaidemo.domain.common.annotations.AggregateRoot;
import solid.humank.genaidemo.domain.observability.valueobject.SessionId;
import solid.humank.genaidemo.domain.observability.valueobject.UserId;

/**
 * 分析會話聚合根
 * 
 * 代表用戶在系統中的一次會話，包含所有相關的分析數據和統計資訊。
 * 僅在生產環境中用於數據持久化和統計查詢。
 * 
 * 設計原則：
 * - 聚合根模式，封裝業務邏輯
 * - 不可變的核心屬性
 * - 支援統計數據的增量更新
 * - 包含數據保留政策管理
 * 
 * 需求: 2.3, 3.3
 */
@AggregateRoot(name = "AnalyticsSession", description = "分析會話聚合根", boundedContext = "Observability", version = "1.0")
public class AnalyticsSession implements AggregateRootInterface {

    private final SessionId sessionId;
    private final Optional<UserId> userId;
    private final String traceId;
    private final LocalDateTime startTime;
    private LocalDateTime lastActivityAt;
    private LocalDateTime endTime;
    private Integer pageViewsCount;
    private Integer userActionsCount;
    private Integer businessEventsCount;
    private Integer performanceMetricsCount;
    private Map<String, Object> sessionMetadata;
    private LocalDateTime retentionDate;

    /**
     * 建構子
     * 
     * @param sessionId       會話 ID
     * @param userId          用戶 ID（可選）
     * @param traceId         追蹤 ID
     * @param startTime       開始時間
     * @param sessionMetadata 會話元數據
     */
    public AnalyticsSession(SessionId sessionId, Optional<UserId> userId, String traceId,
            LocalDateTime startTime, Map<String, Object> sessionMetadata) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.traceId = traceId;
        this.startTime = startTime;
        this.lastActivityAt = startTime;
        this.pageViewsCount = 0;
        this.userActionsCount = 0;
        this.businessEventsCount = 0;
        this.performanceMetricsCount = 0;
        this.sessionMetadata = sessionMetadata != null ? new HashMap<>(sessionMetadata) : new HashMap<>();
        // 預設保留 90 天
        this.retentionDate = startTime.plusDays(90);
    }

    /**
     * 工廠方法 - 創建新的分析會話
     */
    public static AnalyticsSession create(SessionId sessionId, Optional<UserId> userId, String traceId) {
        return new AnalyticsSession(sessionId, userId, traceId, LocalDateTime.now(), new HashMap<>());
    }

    /**
     * 工廠方法 - 創建帶元數據的分析會話
     */
    public static AnalyticsSession createWithMetadata(SessionId sessionId, Optional<UserId> userId,
            String traceId, Map<String, Object> metadata) {
        return new AnalyticsSession(sessionId, userId, traceId, LocalDateTime.now(), metadata);
    }

    // 業務方法

    /**
     * 記錄頁面瀏覽
     */
    public void recordPageView() {
        this.pageViewsCount++;
        updateLastActivity();
    }

    /**
     * 記錄用戶操作
     */
    public void recordUserAction() {
        this.userActionsCount++;
        updateLastActivity();
    }

    /**
     * 記錄業務事件
     */
    public void recordBusinessEvent() {
        this.businessEventsCount++;
        updateLastActivity();
    }

    /**
     * 記錄效能指標
     */
    public void recordPerformanceMetric() {
        this.performanceMetricsCount++;
        updateLastActivity();
    }

    /**
     * 更新最後活動時間
     */
    public void updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    /**
     * 結束會話
     */
    public void endSession() {
        if (this.endTime == null) {
            this.endTime = LocalDateTime.now();
        }
    }

    /**
     * 添加會話元數據
     */
    public void addMetadata(String key, Object value) {
        this.sessionMetadata.put(key, value);
    }

    /**
     * 移除會話元數據
     */
    public void removeMetadata(String key) {
        this.sessionMetadata.remove(key);
    }

    /**
     * 檢查會話是否活躍
     */
    public boolean isActive() {
        return endTime == null;
    }

    /**
     * 檢查會話是否過期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(retentionDate);
    }

    /**
     * 檢查是否為匿名用戶
     */
    public boolean isAnonymous() {
        return userId.isEmpty();
    }

    /**
     * 獲取會話持續時間（秒）
     */
    public Optional<Long> getDurationSeconds() {
        if (endTime != null) {
            return Optional.of(java.time.Duration.between(startTime, endTime).getSeconds());
        }
        return Optional.empty();
    }

    /**
     * 獲取總事件數量
     */
    public int getTotalEventsCount() {
        return pageViewsCount + userActionsCount + businessEventsCount + performanceMetricsCount;
    }

    /**
     * 獲取會話活躍度分數
     */
    public double getActivityScore() {
        int totalEvents = getTotalEventsCount();
        long durationMinutes = java.time.Duration.between(startTime, lastActivityAt).toMinutes();

        if (durationMinutes == 0) {
            return totalEvents;
        }

        return (double) totalEvents / durationMinutes;
    }

    // Getters

    public SessionId getSessionId() {
        return sessionId;
    }

    public Optional<UserId> getUserId() {
        return userId;
    }

    public String getTraceId() {
        return traceId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public Optional<LocalDateTime> getEndTime() {
        return Optional.ofNullable(endTime);
    }

    public Integer getPageViewsCount() {
        return pageViewsCount;
    }

    public Integer getUserActionsCount() {
        return userActionsCount;
    }

    public Integer getBusinessEventsCount() {
        return businessEventsCount;
    }

    public Integer getPerformanceMetricsCount() {
        return performanceMetricsCount;
    }

    public Map<String, Object> getSessionMetadata() {
        return new HashMap<>(sessionMetadata);
    }

    public LocalDateTime getRetentionDate() {
        return retentionDate;
    }

    // Setters (for persistence layer)

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setPageViewsCount(Integer pageViewsCount) {
        this.pageViewsCount = pageViewsCount;
    }

    public void setUserActionsCount(Integer userActionsCount) {
        this.userActionsCount = userActionsCount;
    }

    public void setBusinessEventsCount(Integer businessEventsCount) {
        this.businessEventsCount = businessEventsCount;
    }

    public void setPerformanceMetricsCount(Integer performanceMetricsCount) {
        this.performanceMetricsCount = performanceMetricsCount;
    }

    public void setSessionMetadata(Map<String, Object> sessionMetadata) {
        this.sessionMetadata = sessionMetadata != null ? new HashMap<>(sessionMetadata) : new HashMap<>();
    }

    public void setRetentionDate(LocalDateTime retentionDate) {
        this.retentionDate = retentionDate;
    }

    public String getId() {
        return sessionId.value();
    }

    @Override
    public String toString() {
        return String.format("AnalyticsSession{sessionId=%s, userId=%s, startTime=%s, totalEvents=%d, isActive=%s}",
                sessionId.value(),
                userId.map(UserId::value).orElse("anonymous"),
                startTime,
                getTotalEventsCount(),
                isActive());
    }
}