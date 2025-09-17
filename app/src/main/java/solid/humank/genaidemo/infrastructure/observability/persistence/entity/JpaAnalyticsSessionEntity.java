package solid.humank.genaidemo.infrastructure.observability.persistence.entity;

import java.time.LocalDateTime;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import solid.humank.genaidemo.infrastructure.observability.persistence.converter.JsonMapConverter;

/**
 * 分析會話 JPA 實體
 * 
 * 用於持久化用戶會話的分析數據，僅在生產環境中使用。
 * 支援查詢統計資料和業務指標分析。
 * 
 * 設計原則：
 * - 僅作為持久化技術實現，不包含業務邏輯
 * - 支援高效查詢的索引設計
 * - 包含數據保留政策相關欄位
 * - 支援 JSON 格式的元數據儲存
 * 
 * 需求: 2.3, 3.3
 */
@Entity
@Table(name = "analytics_sessions")
public class JpaAnalyticsSessionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "trace_id")
    private String traceId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "page_views_count", nullable = false)
    private Integer pageViewsCount = 0;

    @Column(name = "user_actions_count", nullable = false)
    private Integer userActionsCount = 0;

    @Column(name = "business_events_count", nullable = false)
    private Integer businessEventsCount = 0;

    @Column(name = "performance_metrics_count", nullable = false)
    private Integer performanceMetricsCount = 0;

    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous = true;

    @Column(name = "session_metadata", columnDefinition = "TEXT")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> sessionMetadata;

    @Column(name = "retention_date", nullable = false)
    private LocalDateTime retentionDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 默認建構子，JPA 需要
    public JpaAnalyticsSessionEntity() {
    }

    // 建構子
    public JpaAnalyticsSessionEntity(String sessionId, String userId, String traceId) {
        this.id = generateId(sessionId);
        this.sessionId = sessionId;
        this.userId = userId;
        this.traceId = traceId;
        this.startTime = LocalDateTime.now();
        this.lastActivityAt = this.startTime;
        this.isAnonymous = (userId == null || userId.trim().isEmpty());
        this.pageViewsCount = 0;
        this.userActionsCount = 0;
        this.businessEventsCount = 0;
        this.performanceMetricsCount = 0;
        // 設定數據保留期限（預設 90 天）
        this.retentionDate = this.startTime.plusDays(90);
    }

    private String generateId(String sessionId) {
        return "analytics_session_" + sessionId;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        this.isAnonymous = (userId == null || userId.trim().isEmpty());
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        if (endTime != null && startTime != null) {
            this.durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
        }
    }

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getPageViewsCount() {
        return pageViewsCount;
    }

    public void setPageViewsCount(Integer pageViewsCount) {
        this.pageViewsCount = pageViewsCount;
    }

    public Integer getUserActionsCount() {
        return userActionsCount;
    }

    public void setUserActionsCount(Integer userActionsCount) {
        this.userActionsCount = userActionsCount;
    }

    public Integer getBusinessEventsCount() {
        return businessEventsCount;
    }

    public void setBusinessEventsCount(Integer businessEventsCount) {
        this.businessEventsCount = businessEventsCount;
    }

    public Integer getPerformanceMetricsCount() {
        return performanceMetricsCount;
    }

    public void setPerformanceMetricsCount(Integer performanceMetricsCount) {
        this.performanceMetricsCount = performanceMetricsCount;
    }

    public Boolean getIsAnonymous() {
        return isAnonymous;
    }

    public void setIsAnonymous(Boolean isAnonymous) {
        this.isAnonymous = isAnonymous;
    }

    public Map<String, Object> getSessionMetadata() {
        return sessionMetadata;
    }

    public void setSessionMetadata(Map<String, Object> sessionMetadata) {
        this.sessionMetadata = sessionMetadata;
    }

    public LocalDateTime getRetentionDate() {
        return retentionDate;
    }

    public void setRetentionDate(LocalDateTime retentionDate) {
        this.retentionDate = retentionDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // 業務方法
    public void incrementPageViews() {
        this.pageViewsCount++;
        updateLastActivity();
    }

    public void incrementUserActions() {
        this.userActionsCount++;
        updateLastActivity();
    }

    public void incrementBusinessEvents() {
        this.businessEventsCount++;
        updateLastActivity();
    }

    public void incrementPerformanceMetrics() {
        this.performanceMetricsCount++;
        updateLastActivity();
    }

    public void updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(retentionDate);
    }

    public int getTotalEventsCount() {
        return pageViewsCount + userActionsCount + businessEventsCount + performanceMetricsCount;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (startTime == null) {
            startTime = now;
        }
        if (lastActivityAt == null) {
            lastActivityAt = now;
        }
        if (retentionDate == null) {
            retentionDate = now.plusDays(90);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}